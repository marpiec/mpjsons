package io.mpjsons.fuzz

import java.nio.charset.StandardCharsets.UTF_8
import java.time.{Duration, Instant, LocalDate, LocalDateTime, LocalTime, ZoneId, ZonedDateTime}
import java.util.UUID
import java.util.concurrent.{Callable, CountDownLatch, Executors, TimeUnit}

import io.mpjsons.Utf8OutputSpec.{nastyChar, nastyString}
import io.mpjsons.impl.serializer.StringSerializerImpl
import io.mpjsons.impl.{SerializerFactory, StringIterator}
import io.mpjsons.{JsonTypeDeserializer, JsonTypeSerializer, MPJsons, MPJsonsConfig, NullPolicy, jsonName, typeName}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import scala.collection.immutable.BitSet
import scala.reflect.runtime.universe._
import scala.util.Random

case class ByteTexts(s: String, list: List[String], map: Map[String, String], opt: Option[String], arr: Array[String],
                     c: Char, set: Set[String], charKeys: Map[Char, String], either: Either[String, List[String]])

case class ByteTimes(d: LocalDate, t: LocalTime, dt: LocalDateTime, dur: Duration, i: Instant, u: UUID, bd: BigDecimal,
                     bi: BigInt, jbd: java.math.BigDecimal, z: ZonedDateTime)

case class ByteTuples(t1: Tuple1[String], t3: (Int, String, Long), t4: (Int, Byte, Short, String),
                      t5: (Boolean, Float, Double, String, Option[Int]), t6: (Int, Int, Int, Int, Int, List[String]),
                      bits: BitSet, keys: Map[Int, String], longKeys: Map[Long, List[Int]], boolKeys: Map[Boolean, String])

case class ByteNames(@jsonName("zażółć") a: Int, @jsonName("emoji😀") b: String, @jsonName("q\"x") c: Long)

sealed trait ByteShape

@typeName("koło") case class ByteCircle(r: Double) extends ByteShape

@typeName("q\"😀") case class ByteSquare(side: Int) extends ByteShape

case object ByteSingleton

case class ByteShapes(shapes: List[ByteShape], main: ByteShape, single: ByteSingleton.type)

/** Fields that often hold their default value, for omitDefaults. */
case class ByteDefaults(i: Int, l: Long, d: Double, f: Float, s: Short, b: Byte, z: Boolean, str: String, o: Option[Int],
                        list: List[Int], inner: Inner)

/** Written by a custom converter: bridged through a StringBuilder. */
case class ByteCustom(value: String)

/** Written by a subclass of a built-in serializer that changes the text: must be bridged too. */
case class ByteUpper(value: String)

case class ByteHasCustom(c: ByteCustom, list: List[ByteCustom], opt: Option[ByteCustom], map: Map[String, ByteCustom],
                         upper: ByteUpper, arr: Array[ByteCustom])

class ByteCustomSerializer extends JsonTypeSerializer[ByteCustom] {
  override def serialize(obj: ByteCustom, jsonBuilder: StringBuilder): Unit = {
    // reversing breaks surrogate pairs into unpaired surrogates on purpose
    jsonBuilder.append('"').append(obj.value.reverse).append("\"")
  }
}

class ByteUpperSerializer extends StringSerializerImpl(true) {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = super.serialize(obj.asInstanceOf[ByteUpper].value.toUpperCase, jsonBuilder)
}

class ByteNoDeserializer[T] extends JsonTypeDeserializer[T] {
  override def deserialize(jsonIterator: StringIterator): T = throw new UnsupportedOperationException
}

class ByteNode {
  var next: ByteNode = _
}

/**
 * serializeToBytes must give exactly serialize(x).getBytes(UTF_8) for every model and configuration, through every
 * entry point, and fail with the same exception where serialize fails.
 */
class ByteOutputDifferentialSpec extends AnyFlatSpec with Matchers {

  private val valuesPerModel = Integer.getInteger("fuzz.values", 300).intValue()
  private val seed = java.lang.Long.getLong("fuzz.seed", 20261023L).longValue()

  private def configured(config: MPJsonsConfig): MPJsons = {
    val m = new MPJsons(config)
    m.markTypedClass[Animal]()
    m.markTypedClass[ByteShape]()
    m.registerConverter[ByteCustom](_ => new ByteCustomSerializer, _ => new ByteNoDeserializer[ByteCustom])
    m.registerConverter[ByteUpper](_ => new ByteUpperSerializer().asInstanceOf[JsonTypeSerializer[ByteUpper]], _ => new ByteNoDeserializer[ByteUpper])
    m
  }

  private val instances = Seq(
    "default" -> configured(MPJsonsConfig()),
    "WriteNull+compactOptions" -> configured(MPJsonsConfig(nullPolicy = NullPolicy.WriteNull, compactOptions = true)),
    "Omit+escapeSlash=false+mapsAsObjects" -> configured(MPJsonsConfig(nullPolicy = NullPolicy.Omit, escapeSlash = false, mapsAsObjects = true)),
    "omitDefaults+isoTime" -> configured(MPJsonsConfig(omitDefaults = true, isoTime = true)),
    "WriteNull+mapsAsObjects+compactOptions" -> configured(MPJsonsConfig(nullPolicy = NullPolicy.WriteNull, mapsAsObjects = true, compactOptions = true)))

  private def outcome(f: => Array[Byte]): String = try {
    val bytes = f
    bytes.map(b => f"$b%02x").mkString
  } catch {
    case e: Throwable => "FAILURE " + e.getClass.getName + ": " + e.getMessage
  }

  /**
   * Every entry point of every instance against the matching serialize(x).getBytes(UTF_8) (MPJsons and
   * StaticSerializer word the top-level null error differently, so each is compared with its own serialize).
   */
  private def compare[T](name: String, value: T)(implicit tag: TypeTag[T]): Unit = {
    for ((configName, m) <- instances) {
      val expected = outcome(m.serialize[T](value).getBytes(UTF_8))
      val static = m.buildStaticSerializer[T]
      val staticExpected = outcome(static.serialize(value).getBytes(UTF_8))
      val results = Seq(
        ("serializeToBytes[T]", outcome(m.serializeToBytes[T](value)), expected),
        ("serializeToBytes(obj, tpe)", outcome(m.serializeToBytes[T](value, typeOf[T])), expected),
        ("StaticSerializer.serializeToBytes", outcome(static.serializeToBytes(value)), staticExpected))
      for ((entry, result, wanted) <- results) {
        if (result != wanted) {
          fail(s"""Different bytes for $name via $entry (configuration: $configName)
                  |serialize: ${wanted.take(3000)}
                  |bytes:     ${result.take(3000)}""".stripMargin)
        }
      }
    }
  }

  private def check[T](name: String, generate: Gen => T)(implicit tag: TypeTag[T]): Unit = {
    val gen = new Gen(new Random(seed + name.hashCode))
    for (_ <- 0 until valuesPerModel) {
      compare(name, generate(gen))
    }
    info(s"$name: ${valuesPerModel * instances.size} values checked")
  }

  private val r = new Random(seed)

  private def nasty(): String = nastyString(r)

  private def texts(): ByteTexts = ByteTexts(nasty(), List.fill(r.nextInt(4))(nasty()), List.fill(r.nextInt(4))(nasty() -> nasty()).toMap,
    if (r.nextBoolean()) Some(nasty()) else None, Array.fill(r.nextInt(3))(nasty()), nastyChar(r),
    List.fill(r.nextInt(4))(nasty()).toSet, List.fill(r.nextInt(4))(nastyChar(r) -> nasty()).toMap,
    if (r.nextBoolean()) Left(nasty()) else Right(List.fill(r.nextInt(3))(nasty())))

  private def times(): ByteTimes = {
    val instant = Instant.ofEpochSecond(r.nextInt(Int.MaxValue).toLong - Int.MaxValue / 2, r.nextInt(1000000000))
    val dt = LocalDateTime.ofEpochSecond(r.nextInt(Int.MaxValue).toLong - Int.MaxValue / 2, r.nextInt(1000000000), java.time.ZoneOffset.UTC)
    ByteTimes(dt.toLocalDate, dt.toLocalTime, dt, Duration.ofSeconds(r.nextLong() >> 4, r.nextInt(1000000000)), instant,
      new UUID(r.nextLong(), r.nextLong()), BigDecimal(r.nextGaussian() * math.pow(10, r.nextInt(30) - 15)),
      BigInt(r.nextLong()) * BigInt(r.nextLong()), new java.math.BigDecimal(r.nextLong()).scaleByPowerOfTen(r.nextInt(40) - 20),
      ZonedDateTime.ofInstant(instant, ZoneId.of(if (r.nextBoolean()) "Europe/Warsaw" else "America/Argentina/Buenos_Aires")))
  }

  private def tuples(): ByteTuples = ByteTuples(Tuple1(nasty()), (r.nextInt(), nasty(), r.nextLong()),
    (r.nextInt(), r.nextInt(256).toByte, r.nextInt(65536).toShort, nasty()),
    (r.nextBoolean(), r.nextFloat(), r.nextGaussian(), nasty(), if (r.nextBoolean()) Some(r.nextInt()) else None),
    (r.nextInt(), r.nextInt(), r.nextInt(), r.nextInt(), r.nextInt(), List.fill(r.nextInt(3))(nasty())),
    BitSet(List.fill(r.nextInt(8))(r.nextInt(300)): _*), List.fill(r.nextInt(4))(r.nextInt() -> nasty()).toMap,
    List.fill(r.nextInt(4))(r.nextLong() -> List.fill(r.nextInt(3))(r.nextInt())).toMap,
    List.fill(r.nextInt(3))(r.nextBoolean() -> nasty()).toMap)

  private def shape(): ByteShape = if (r.nextBoolean()) ByteCircle(r.nextGaussian()) else ByteSquare(r.nextInt())

  private def orDefault[T](default: T, value: => T): T = if (r.nextBoolean()) default else value

  private def defaults(): ByteDefaults = ByteDefaults(orDefault(0, r.nextInt()), orDefault(0L, r.nextLong()),
    orDefault(0.0, r.nextGaussian()), orDefault(0.0f, r.nextFloat()), orDefault(0.toShort, r.nextInt(65536).toShort),
    orDefault(0.toByte, r.nextInt(256).toByte), orDefault(false, true), orDefault(null, nasty()),
    orDefault(None, Some(r.nextInt())), orDefault(null, List.fill(r.nextInt(3))(r.nextInt())),
    orDefault(null, Inner(orDefault(0, r.nextInt()), orDefault(null, nasty()))))

  private def custom(): ByteCustom = ByteCustom(nasty())

  private def hasCustom(): ByteHasCustom = ByteHasCustom(custom(), List.fill(r.nextInt(4))(custom()),
    if (r.nextBoolean()) Some(custom()) else None, List.fill(r.nextInt(3))(nasty() -> custom()).toMap, ByteUpper(nasty()),
    Array.fill(r.nextInt(3))(custom()))

  "serializeToBytes" must "write the same bytes as serialize for primitives" in check[Prims]("Prims", _.prims())
  it must "write the same bytes as serialize for java boxed primitives" in check[Boxed]("Boxed", _.boxed())
  it must "write the same bytes as serialize for collections" in check[Colls]("Colls", _.colls())
  it must "write the same bytes as serialize for maps" in check[Maps]("Maps", _.maps())
  it must "write the same bytes as serialize for arrays" in check[Arrays]("Arrays", _.arrays())
  it must "write the same bytes as serialize for options, eithers and tuples" in check[Opts]("Opts", _.opts())
  it must "write the same bytes as serialize for typed classes" in check[Zoo]("Zoo", _.zoo())
  it must "write the same bytes as serialize for recursive types" in check[io.mpjsons.fuzz.Tree]("Tree", _.tree())
  it must "write the same bytes as serialize for beans" in check[Bean]("Bean", _.bean())
  it must "write the same bytes as serialize for beans with final fields" in check[BeanWithFinals]("BeanWithFinals", _.beanWithFinals())
  it must "write the same bytes as serialize for generic types" in check[UsesGeneric]("UsesGeneric", _.usesGeneric())
  it must "write the same bytes as serialize for top level arrays and collections" in {
    check[Array[Double]]("TopLevelDoubles", g => Array.fill(g.size(20))(g.double()))
    check[List[String]]("TopLevelStrings", _ => List.fill(r.nextInt(5))(nasty()))
    check[String]("TopLevelString", _ => nasty())
    check[Int]("TopLevelInt", g => g.int())
  }
  it must "write the same bytes as serialize for unpaired surrogates and other unusual text" in check[ByteTexts]("ByteTexts", _ => texts())
  it must "write the same bytes as serialize for time, UUID and big number types" in check[ByteTimes]("ByteTimes", _ => times())
  it must "write the same bytes as serialize for fields holding default values" in check[ByteDefaults]("ByteDefaults", _ => defaults())
  it must "write the same bytes as serialize for tuples, bit sets and scalar map keys" in check[ByteTuples]("ByteTuples", _ => tuples())
  it must "write the same bytes as serialize for non-ASCII field names and type names" in {
    check[ByteNames]("ByteNames", _ => ByteNames(r.nextInt(), nasty(), r.nextLong()))
    check[ByteShapes]("ByteShapes", _ => ByteShapes(List.fill(r.nextInt(4))(shape()), shape(), ByteSingleton))
  }
  it must "write the same bytes as serialize for custom converters, including a subclass of a built-in serializer" in {
    check[ByteHasCustom]("ByteHasCustom", _ => hasCustom())
    compare("upper", ByteUpper("abc żółw"))
    instances.head._2.serializeToBytes(ByteUpper("abc")) mustBe "\"ABC\"".getBytes(UTF_8)
  }

  it must "fail like serialize" in {
    val prims = Prims(1, 2, 3, 4L, 5f, 6.0, 'c', z = true, "s")
    val cyclic = new ByteNode
    cyclic.next = cyclic
    val cases: Seq[(String, () => Unit)] = Seq(
      "NaN in Array[Double]" -> (() => compare("nan array", Array(1.0, Double.NaN))),
      "Infinity in Array[Float]" -> (() => compare("inf array", Array(Float.PositiveInfinity))),
      "NaN field" -> (() => compare("nan field", prims.copy(d = Double.NaN))),
      "Infinite float field" -> (() => compare("inf field", prims.copy(f = Float.NegativeInfinity))),
      "NaN in a List" -> (() => compare("nan list", List(1.0, Double.NaN))),
      "null String field" -> (() => compare("null string", prims.copy(str = null))),
      "null byte in a String" -> (() => compare("null byte", prims.copy(str = "a\u0000b"))),
      "null inner bean" -> (() => compare("null bean", new Bean)),
      "null element" -> (() => compare("null element", List[String]("a", null))),
      "null map key and value" -> (() => compare("null map", Map[String, String]((null, "x"), ("k", null)))),
      "Some(null)" -> (() => compare("some null", Option[String](null).orElse(Some(null)))),
      "null top level" -> (() => compare[Prims]("null top", null)),
      "null tuple element" -> (() => compare("null tuple", (null: String, 1))),
      "cyclic reference" -> (() => compare("cycle", cyclic)),
      "converter that throws" -> (() => compare("throws", ByteCustom(null))))
    for ((name, run) <- cases) {
      withClue(name) {
        run()
      }
    }
  }

  it must "reject a call without a type argument like serialize" in {
    val m = instances.head._2
    val nothing = typeTag[Nothing].asInstanceOf[TypeTag[Any]]
    the[IllegalArgumentException] thrownBy m.serializeToBytes[Any]("x")(nothing) must have message
      "Type for object serialization was not specified, or was Nothing. Please specify object type."
    the[IllegalArgumentException] thrownBy m.serialize[Any]("x")(nothing) must have message
      "Type for object serialization was not specified, or was Nothing. Please specify object type."
  }

  it must "work with a SimpleMapSerializer and a type name" in {
    val factory = new SerializerFactory(MPJsonsConfig(nullPolicy = NullPolicy.WriteNull))
    val static = new io.mpjsons.StaticSerializer[Map[String, Int]](new io.mpjsons.impl.serializer.SimpleMapSerializer[Int](factory,
      io.mpjsons.impl.util.Context(List.empty, Map.empty)))
    for (_ <- 0 until 200) {
      val map = List.fill(r.nextInt(5))(nasty() -> r.nextInt()).toMap
      static.serializeToBytes(map) mustBe static.serialize(map).getBytes(UTF_8)
    }
    val m = instances.head._2
    val value = Inner(5, "żółw")
    m.serializeToBytes(value, classOf[Inner].getName) mustBe m.serialize(value, classOf[Inner].getName).getBytes(UTF_8)
  }

  it must "be safe to share one instance between threads" in {
    val gen = new Gen(new Random(seed))
    val values: Vector[AnyRef] = Vector.fill(40)(gen.prims()) ++ Vector.fill(40)(gen.arrays()) ++ Vector.fill(40)(gen.zoo()) ++
      Vector.fill(40)(gen.bean()) ++ Vector.fill(40)(texts()) ++ Vector.fill(40)(hasCustom())
    for (_ <- 0 until 5) {
      val m = configured(MPJsonsConfig())
      def write(v: AnyRef): Array[Byte] = v match {
        case x: Prims => m.serializeToBytes(x)
        case x: Arrays => m.serializeToBytes(x)
        case x: Zoo => m.serializeToBytes(x)
        case x: Bean => m.serializeToBytes(x)
        case x: ByteTexts => m.serializeToBytes(x)
        case x: ByteHasCustom => m.serializeToBytes(x)
      }
      def expectedOf(v: AnyRef): Array[Byte] = (v match {
        case x: Prims => m.serialize(x)
        case x: Arrays => m.serialize(x)
        case x: Zoo => m.serialize(x)
        case x: Bean => m.serialize(x)
        case x: ByteTexts => m.serialize(x)
        case x: ByteHasCustom => m.serialize(x)
      }).getBytes(UTF_8)
      val threads = 16
      val pool = Executors.newFixedThreadPool(threads)
      val start = new CountDownLatch(1)
      val expected = values.map(expectedOf)
      val futures = (0 until threads).map { t =>
        pool.submit(new Callable[Boolean] {
          override def call(): Boolean = {
            start.await()
            (0 until 400).forall { n =>
              val i = (t * 31 + n) % values.length
              java.util.Arrays.equals(write(values(i)), expected(i))
            }
          }
        })
      }
      start.countDown()
      futures.map(_.get(60, TimeUnit.SECONDS)) mustBe Seq.fill(threads)(true)
      pool.shutdown()
    }
  }
}
