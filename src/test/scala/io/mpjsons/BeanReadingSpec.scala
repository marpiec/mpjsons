package io.mpjsons

import java.util.concurrent.{Callable, CountDownLatch, Executors, TimeUnit}

import io.mpjsons.fuzz.Deep
import io.mpjsons.impl.deserializer.values.StringDeserializer
import io.mpjsons.impl.serializer.StringSerializer
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

object BeanReadingSpec {

  case class Five(a: Int, b: String, c: Option[Long], d: Boolean, e: List[Int])

  case class Renamed(@jsonName("x-first") first: Int, second: String, @skipSerialization skipped: Option[String])

  class WithDefaults {
    var o: Option[Int] = Some(7)
    var p: Option[String] = _
    var n: Int = 1
  }

  case class Embedded(values: Array[Double])

  case class WithEmbedded(before: Array[Double], embedded: Embedded, after: Array[Double], ints: Array[Int], longs: Array[Long])

  case class Arrays3(a: Array[Double], b: Array[Array[Double]], c: Array[Double], d: Array[Int], e: Array[Long])
}

/**
 * Bean reading details that the key-order prediction, Option tracking and array scratch buffers must not change.
 * Where the 0.7.2 copy (src/legacy) understands the input, results are compared with it.
 */
class BeanReadingSpec extends AnyFlatSpec with Matchers {

  import BeanReadingSpec._

  private val current = new MPJsons()
  private val legacy = new io.mpjsons.legacy.MPJsons()
  private val currentSkipping = new MPJsons(ignoreNonExistingFields = true)
  private val legacySkipping = new io.mpjsons.legacy.MPJsons(ignoreNonExistingFields = true)

  private def outcome(f: => Any): String = try Deep.repr(f) catch {
    case e: Throwable => "FAILURE " + e.getClass.getName.replace("io.mpjsons.legacy.", "io.mpjsons.")
  }

  private def sameAsLegacy(json: String): Unit = {
    withClue(json) {
      outcome(current.deserialize[Five](json)) mustBe outcome(legacy.deserialize[Five](json))
      outcome(currentSkipping.deserialize[Five](json)) mustBe outcome(legacySkipping.deserialize[Five](json))
    }
  }

  private val members = Seq(""""a":1""", """"b":"x"""", """"c":{"value":5}""", """"d":true""", """"e":[1,2]""")

  "Bean reading" must "give the same result as 0.7.2 for keys in any order" in {
    for (permutation <- members.permutations) {
      sameAsLegacy(permutation.mkString("{", ",", "}"))
      sameAsLegacy(permutation.mkString("{ ", " , ", " }"))
    }
  }

  it must "give the same result as 0.7.2 for missing, duplicated, unknown, escaped and bare keys" in {
    for (permutation <- members.permutations.take(24); drop <- members.indices) {
      sameAsLegacy(permutation.patch(drop, Nil, 1).mkString("{", ",", "}"))
      sameAsLegacy((permutation :+ permutation(drop)).mkString("{", ",", "}"))
      sameAsLegacy(permutation.patch(drop, Seq(""""zz":[1,{"a":2}]"""), 0).mkString("{", ",", "}"))
    }
    sameAsLegacy("""{"a":1,"b":"x","c":{},"d":false,"e":[]}""")
    sameAsLegacy("""{"a":1,"b":"x","c":{},"d":false,"e":[]}""")
    sameAsLegacy("""{a:1,b:"x",c:{},d:false,e:[]}""")
    sameAsLegacy("""{"a":1,b:"x","c":{},d:false,"e":[]}""")
    sameAsLegacy("""{"a":1,"bb":"x","c":{},"d":false,"e":[]}""")
    sameAsLegacy("""{"a":1,"b""")
    sameAsLegacy("""{"a":1,"b":"x","c""")
  }

  it must "read renamed fields by JSON name and by Scala name, in any order" in {
    val expected = Renamed(1, "s", None)
    current.deserialize[Renamed]("""{"x-first":1,"second":"s"}""") mustBe expected
    current.deserialize[Renamed]("""{"second":"s","x-first":1}""") mustBe expected
    current.deserialize[Renamed]("""{"first":1,"second":"s"}""") mustBe expected
    current.deserialize[Renamed]("""{"second":"s","first":1,"skipped":{"value":"ignored"}}""") mustBe expected
    current.deserialize[Renamed](current.serialize(expected)) mustBe expected
  }

  it must "initialise absent Option fields as before" in {
    current.deserialize[Five]("""{"a":1,"b":"x","d":true,"e":[]}""") mustBe Five(1, "x", None, d = true, Nil)
    current.deserialize[Five]("""{"a":1,"b":"x","c":null,"d":true,"e":[]}""") mustBe Five(1, "x", None, d = true, Nil)

    val withDefaults = current.deserialize[WithDefaults]("""{"n":2}""")
    withDefaults.o mustBe Some(7)
    withDefaults.p mustBe None
    withDefaults.n mustBe 2
    current.deserialize[WithDefaults]("""{"o":{},"p":{"value":"v"}}""").o mustBe None
  }

  it must "treat Option fields like 0.7.2 when a converter is registered for an Option type" in {
    val currentCustom = new MPJsons()
    currentCustom.registerConverter[Option[String]](_ => StringSerializer.asInstanceOf[JsonTypeSerializer[Option[String]]], _ => new JsonTypeDeserializer[Option[String]] {
      override def deserialize(jsonIterator: impl.StringIterator): Option[String] = {
        StringDeserializer.deserialize(jsonIterator)
        null
      }
    })
    val legacyCustom = new io.mpjsons.legacy.MPJsons()
    legacyCustom.registerConverter[Option[String]](_ => io.mpjsons.legacy.impl.serializer.StringSerializer.asInstanceOf[io.mpjsons.legacy.JsonTypeSerializer[Option[String]]], _ => new io.mpjsons.legacy.JsonTypeDeserializer[Option[String]] {
      override def deserialize(jsonIterator: io.mpjsons.legacy.impl.StringIterator): Option[String] = {
        io.mpjsons.legacy.impl.deserializer.values.StringDeserializer.deserialize(jsonIterator)
        null
      }
    })
    for (json <- Seq("""{"p":"anything"}""", """{"p":{"value":"v"}}""", """{"n":3}""", """{"o":"x","p":"y"}""")) {
      withClue(json) {
        outcome(currentCustom.deserialize[WithDefaults](json)) mustBe outcome(legacyCustom.deserialize[WithDefaults](json))
      }
    }
  }

  it must "initialise absent Option fields of a bean with more than 64 of them" in {
    val bean = current.deserialize[ManyOptionsBean]("""{"o3":{"value":3},"o70":{"value":70}}""")
    bean.o3 mustBe Some(3)
    bean.o70 mustBe Some(70)
    bean.o0 mustBe None
    bean.o69 mustBe None
  }

  it must "read primitive arrays of any size, several per document and nested" in {
    val big = Array.tabulate(10000)(_ * 0.5)
    val value = Arrays3(big, Array(Array(1.5), Array(), big.take(100)), Array(), Array.tabulate(5000)(identity), Array.tabulate(33)(_.toLong))
    Deep.repr(current.deserialize[Arrays3](current.serialize(value))) mustBe Deep.repr(value)
    outcome(current.deserialize[Array[Int]]("[1, null, 3]")) mustBe outcome(legacy.deserialize[Array[Int]]("[1, null, 3]"))
  }

  private def withEmbeddedConverter(mpjsons: MPJsons): MPJsons = {
    // The converter deserializes a nested document with the same instance, on the same thread, while the outer
    // document is being read.
    mpjsons.registerConverter[Embedded](
      _ => new JsonTypeSerializer[Embedded] {
        override def serialize(obj: Embedded, jsonBuilder: StringBuilder): Unit =
          StringSerializer.serialize(mpjsons.serialize(obj.values), jsonBuilder)
      },
      _ => new JsonTypeDeserializer[Embedded] {
        override def deserialize(jsonIterator: impl.StringIterator): Embedded =
          Embedded(mpjsons.deserialize[Array[Double]](StringDeserializer.deserialize(jsonIterator)))
      })
    mpjsons
  }

  private def embeddedValue(i: Int) = WithEmbedded(Array.tabulate(20 + i)(_ + 0.25), Embedded(Array.tabulate(40 + i)(_ * 2.0)),
    Array.tabulate(30)(_ - 1.0), Array.tabulate(i)(identity), Array.tabulate(17)(_.toLong * i))

  it must "support nested deserialization from a custom converter while arrays are being read" in {
    val mpjsons = withEmbeddedConverter(new MPJsons())
    for (i <- 0 until 50) {
      val value = embeddedValue(i)
      Deep.repr(mpjsons.deserialize[WithEmbedded](mpjsons.serialize(value))) mustBe Deep.repr(value)
    }
  }

  it must "be safe to share one instance between threads, including nested deserialization" in {
    for (_ <- 0 until 10) {
      val mpjsons = withEmbeddedConverter(new MPJsons())
      val jsons = (0 until 32).map(i => i -> mpjsons.serialize(embeddedValue(i)))
      val fives = members.permutations.map(_.mkString("{", ",", "}")).toVector
      val threads = 16
      val pool = Executors.newFixedThreadPool(threads)
      val start = new CountDownLatch(1)
      val futures = (0 until threads).map { t =>
        pool.submit(new Callable[Boolean] {
          override def call(): Boolean = {
            start.await()
            (0 until 200).forall { n =>
              val (i, json) = jsons((t + n) % jsons.length)
              val five = mpjsons.deserialize[Five](fives((t * 7 + n) % fives.length))
              Deep.repr(mpjsons.deserialize[WithEmbedded](json)) == Deep.repr(embeddedValue(i)) &&
                five == Five(1, "x", Some(5L), d = true, List(1, 2))
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

class ManyOptionsBean {
  var o0: Option[Int] = _; var o1: Option[Int] = _; var o2: Option[Int] = _; var o3: Option[Int] = _; var o4: Option[Int] = _
  var o5: Option[Int] = _; var o6: Option[Int] = _; var o7: Option[Int] = _; var o8: Option[Int] = _; var o9: Option[Int] = _
  var o10: Option[Int] = _; var o11: Option[Int] = _; var o12: Option[Int] = _; var o13: Option[Int] = _; var o14: Option[Int] = _
  var o15: Option[Int] = _; var o16: Option[Int] = _; var o17: Option[Int] = _; var o18: Option[Int] = _; var o19: Option[Int] = _
  var o20: Option[Int] = _; var o21: Option[Int] = _; var o22: Option[Int] = _; var o23: Option[Int] = _; var o24: Option[Int] = _
  var o25: Option[Int] = _; var o26: Option[Int] = _; var o27: Option[Int] = _; var o28: Option[Int] = _; var o29: Option[Int] = _
  var o30: Option[Int] = _; var o31: Option[Int] = _; var o32: Option[Int] = _; var o33: Option[Int] = _; var o34: Option[Int] = _
  var o35: Option[Int] = _; var o36: Option[Int] = _; var o37: Option[Int] = _; var o38: Option[Int] = _; var o39: Option[Int] = _
  var o40: Option[Int] = _; var o41: Option[Int] = _; var o42: Option[Int] = _; var o43: Option[Int] = _; var o44: Option[Int] = _
  var o45: Option[Int] = _; var o46: Option[Int] = _; var o47: Option[Int] = _; var o48: Option[Int] = _; var o49: Option[Int] = _
  var o50: Option[Int] = _; var o51: Option[Int] = _; var o52: Option[Int] = _; var o53: Option[Int] = _; var o54: Option[Int] = _
  var o55: Option[Int] = _; var o56: Option[Int] = _; var o57: Option[Int] = _; var o58: Option[Int] = _; var o59: Option[Int] = _
  var o60: Option[Int] = _; var o61: Option[Int] = _; var o62: Option[Int] = _; var o63: Option[Int] = _; var o64: Option[Int] = _
  var o65: Option[Int] = _; var o66: Option[Int] = _; var o67: Option[Int] = _; var o68: Option[Int] = _; var o69: Option[Int] = _
  var o70: Option[Int] = _
}
