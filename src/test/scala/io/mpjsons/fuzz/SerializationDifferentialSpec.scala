package io.mpjsons.fuzz

import java.util.concurrent.{Callable, CountDownLatch, Executors, TimeUnit}

import io.mpjsons.{MPJsons, MPJsonsConfig, NullPolicy}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import scala.reflect.runtime.universe._
import scala.util.Random

/**
 * Serialization must produce exactly the same text as 0.7.2 (copy in src/legacy) for every fuzz model and
 * configuration, and fail in the same way where 0.7.2 fails.
 */
class SerializationDifferentialSpec extends AnyFlatSpec with Matchers {

  private val valuesPerModel = Integer.getInteger("fuzz.values", 300).intValue()
  private val seed = java.lang.Long.getLong("fuzz.seed", 20261023L).longValue()

  private case class Pair(name: String, current: MPJsons, legacy: io.mpjsons.legacy.MPJsons)

  private def pair(name: String, config: MPJsonsConfig, legacyConfig: io.mpjsons.legacy.MPJsonsConfig): Pair = {
    val current = new MPJsons(config)
    val legacy = new io.mpjsons.legacy.MPJsons(legacyConfig)
    current.markTypedClass[Animal]()
    legacy.markTypedClass[Animal]()
    Pair(name, current, legacy)
  }

  private val pairs = Array(
    pair("default", MPJsonsConfig(), io.mpjsons.legacy.MPJsonsConfig()),
    pair("WriteNull+compactOptions",
      MPJsonsConfig(nullPolicy = NullPolicy.WriteNull, compactOptions = true),
      io.mpjsons.legacy.MPJsonsConfig(nullPolicy = io.mpjsons.legacy.NullPolicy.WriteNull, compactOptions = true)),
    pair("Omit+escapeSlash=false+mapsAsObjects",
      MPJsonsConfig(nullPolicy = NullPolicy.Omit, escapeSlash = false, mapsAsObjects = true),
      io.mpjsons.legacy.MPJsonsConfig(nullPolicy = io.mpjsons.legacy.NullPolicy.Omit, escapeSlash = false, mapsAsObjects = true)),
    pair("omitDefaults", MPJsonsConfig(omitDefaults = true), io.mpjsons.legacy.MPJsonsConfig(omitDefaults = true)))

  private def outcome(f: => String): String = try f catch {
    case e: Throwable => "FAILURE " + e.getClass.getName.replace("io.mpjsons.legacy.", "io.mpjsons.") + ": " + e.getMessage
  }

  private def check[T](name: String, generate: Gen => T)(implicit tag: TypeTag[T]): Unit = {
    val gen = new Gen(new Random(seed + name.hashCode))
    var checked = 0
    for (_ <- 0 until valuesPerModel; p <- pairs) {
      val value = generate(gen)
      val current = outcome(p.current.serialize[T](value))
      val legacy = outcome(p.legacy.serialize[T](value))
      if (current != legacy) {
        fail(s"""Different output for $name (configuration: ${p.name})
                |current: ${current.take(2000)}
                |legacy:  ${legacy.take(2000)}""".stripMargin)
      }
      checked += 1
    }
    info(s"$name: $checked values checked")
  }

  "Current serializer" must "write the same as 0.7.2 for primitives" in check[Prims]("Prims", _.prims())
  it must "write the same as 0.7.2 for java boxed primitives" in check[Boxed]("Boxed", _.boxed())
  it must "write the same as 0.7.2 for collections" in check[Colls]("Colls", _.colls())
  it must "write the same as 0.7.2 for maps" in check[Maps]("Maps", _.maps())
  it must "write the same as 0.7.2 for arrays" in check[Arrays]("Arrays", _.arrays())
  it must "write the same as 0.7.2 for options, eithers and tuples" in check[Opts]("Opts", _.opts())
  it must "write the same as 0.7.2 for typed classes" in check[Zoo]("Zoo", _.zoo())
  it must "write the same as 0.7.2 for recursive types" in check[io.mpjsons.fuzz.Tree]("Tree", _.tree())
  it must "write the same as 0.7.2 for beans" in check[Bean]("Bean", _.bean())
  it must "write the same as 0.7.2 for beans with final fields" in check[BeanWithFinals]("BeanWithFinals", _.beanWithFinals())
  it must "write the same as 0.7.2 for generic types" in check[UsesGeneric]("UsesGeneric", _.usesGeneric())
  it must "write the same as 0.7.2 for top level arrays" in check[Array[Double]]("TopLevelDoubles", g => Array.fill(g.size(20))(g.double()))

  it must "fail like 0.7.2 for non-finite numbers and nulls" in {
    val cases: Seq[(String, Pair => (String, String))] = Seq(
      "NaN in Array[Double]" -> (p => (outcome(p.current.serialize(Array(1.0, Double.NaN))), outcome(p.legacy.serialize(Array(1.0, Double.NaN))))),
      "Infinity in Array[Float]" -> (p => (outcome(p.current.serialize(Array(Float.PositiveInfinity))), outcome(p.legacy.serialize(Array(Float.PositiveInfinity))))),
      "NaN field" -> (p => {
        val v = Prims(1, 2, 3, 4L, 5f, Double.NaN, 'c', z = true, "s")
        (outcome(p.current.serialize(v)), outcome(p.legacy.serialize(v)))
      }),
      "null String field" -> (p => {
        val v = Prims(1, 2, 3, 4L, 5f, 6.0, 'c', z = true, null)
        (outcome(p.current.serialize(v)), outcome(p.legacy.serialize(v)))
      }),
      "null inner bean" -> (p => {
        val b = new Bean
        (outcome(p.current.serialize(b)), outcome(p.legacy.serialize(b)))
      }),
      "null array" -> (p => {
        val a = Arrays(null, Array(), Array(), Array(), Array(), Array(), Array(), Array(), Array(), Array(), Array())
        (outcome(p.current.serialize(a)), outcome(p.legacy.serialize(a)))
      }),
      "boxed Integer array with null" -> (p => {
        val a = Array[java.lang.Integer](1, null, 3)
        (outcome(p.current.serialize(a)), outcome(p.legacy.serialize(a)))
      }),
      "boxed Double array" -> (p => {
        val a = Array[java.lang.Double](1.5, -0.0, 1e300)
        (outcome(p.current.serialize(a)), outcome(p.legacy.serialize(a)))
      }),
      "empty primitive arrays" -> (p => {
        val a = Arrays(Array(), Array(), Array(), Array(), Array(), Array(), Array(), Array(), Array(), Array(), Array())
        (outcome(p.current.serialize(a)), outcome(p.legacy.serialize(a)))
      }))
    for ((name, run) <- cases; p <- pairs) {
      withClue(s"$name (${p.name})") {
        val (current, legacy) = run(p)
        current mustBe legacy
      }
    }
  }

  it must "be safe to share one instance between threads" in {
    val gen = new Gen(new Random(seed))
    val values: Vector[AnyRef] = Vector.fill(40)(gen.prims()) ++ Vector.fill(40)(gen.arrays()) ++ Vector.fill(40)(gen.zoo()) ++
      Vector.fill(40)(gen.bean()) ++ Vector.fill(40)(gen.beanWithFinals())
    for (_ <- 0 until 5) {
      val mpjsons = new MPJsons()
      mpjsons.markTypedClass[Animal]()
      val reference = new MPJsons()
      reference.markTypedClass[Animal]()
      def write(m: MPJsons, v: AnyRef): String = v match {
        case x: Prims => m.serialize(x)
        case x: Arrays => m.serialize(x)
        case x: Zoo => m.serialize(x)
        case x: Bean => m.serialize(x)
        case x: BeanWithFinals => m.serialize(x)
      }
      val expected = values.map(v => write(reference, v))
      val threads = 16
      val pool = Executors.newFixedThreadPool(threads)
      val start = new CountDownLatch(1)
      val futures = (0 until threads).map { t =>
        pool.submit(new Callable[Boolean] {
          override def call(): Boolean = {
            start.await()
            (0 until 400).forall { n =>
              val i = (t * 31 + n) % values.length
              write(mpjsons, values(i)) == expected(i)
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
