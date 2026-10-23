package io.mpjsons.fuzz

import io.mpjsons.{MPJsons, MPJsonsConfig, NullPolicy}
import io.mpjsons.impl.StringIterator
import org.scalatest.flatspec.AnyFlatSpec

import scala.reflect.runtime.universe._
import scala.util.Random

/**
 * Compares the current implementation with the reference copy of 0.7.2 (src/legacy) on canonical JSON and on many
 * lenient/corrupted variants of it, in several configurations. Results must be identical: same value
 * (including runtime classes) or failure in both, and the same final StringIterator state.
 */
class DifferentialFuzzSpec extends AnyFlatSpec {

  private val valuesPerModel = Integer.getInteger("fuzz.values", 40).intValue()
  private val variantsPerValue = Integer.getInteger("fuzz.variants", 40).intValue()
  private val seed = java.lang.Long.getLong("fuzz.seed", 20261023L).longValue()

  private case class Instances(name: String, current: MPJsons, legacy: io.mpjsons.legacy.MPJsons, strict: Boolean, skipping: Boolean)

  private def instances(name: String, ignoreNonExistingFields: Boolean = false, strict: Boolean = false, omitNulls: Boolean = false) = {
    val current = new MPJsons(MPJsonsConfig(ignoreNonExistingFields = ignoreNonExistingFields, strict = strict,
      nullPolicy = if (omitNulls) NullPolicy.Omit else NullPolicy.Fail))
    val legacy = new io.mpjsons.legacy.MPJsons(io.mpjsons.legacy.MPJsonsConfig(ignoreNonExistingFields = ignoreNonExistingFields, strict = strict,
      nullPolicy = if (omitNulls) io.mpjsons.legacy.NullPolicy.Omit else io.mpjsons.legacy.NullPolicy.Fail))
    current.markTypedClass[Animal]()
    legacy.markTypedClass[Animal]()
    Instances(name, current, legacy, strict, ignoreNonExistingFields)
  }

  private val configurations = Array(
    instances("default"),
    instances("ignoreNonExistingFields", ignoreNonExistingFields = true),
    instances("strict", strict = true),
    instances("strict+ignoreNonExistingFields+NullPolicy.Omit", ignoreNonExistingFields = true, strict = true, omitNulls = true))

  private val serializer = {
    val s = new MPJsons()
    s.markTypedClass[Animal]()
    s
  }

  private def outcome(f: => Any): String = try {
    Deep.repr(f)
  } catch {
    case e: Throwable => "FAILURE " + e.getClass.getName.replace("io.mpjsons.legacy.", "io.mpjsons.")
  }

  private def errorOf(f: => Any): String = try { f; "" } catch { case e: Throwable => String.valueOf(e.getMessage).take(1000) }

  private def check[T](name: String, generate: Gen => T)(implicit tag: TypeTag[T]): Unit = {
    val random = new Random(seed + name.hashCode)
    val gen = new Gen(random)
    val mutator = new JsonMutator(random)
    val tpe = tag.tpe
    var successes = 0
    var checked = 0
    for (_ <- 0 until valuesPerModel) {
      val json = serializer.serialize[T](generate(gen))
      for (v <- 0 until variantsPerValue) {
        val instances = configurations(v % configurations.length)
        val (variant, corrupted) = if (v < configurations.length) (json, false) else mutator.mutate(json, injectUnknownFields = instances.skipping)

        val currentOutcomes = (outcome(instances.current.deserialize[T](variant)), outcome {
          val it = new StringIterator(variant, instances.strict)
          val result = instances.current.deserialize[T](it, tpe)
          (Deep.repr(result), it.currentChar.toInt, it.nextIndex)
        })
        val legacyOutcomes = (outcome(instances.legacy.deserialize[T](variant)), outcome {
          val it = new io.mpjsons.legacy.impl.StringIterator(variant, instances.strict)
          val result = instances.legacy.deserialize[T](it, tpe)
          (Deep.repr(result), it.currentChar.toInt, it.nextIndex)
        })

        if (currentOutcomes != legacyOutcomes) {
          fail(s"""Difference for $name (configuration: ${instances.name}, corrupted: $corrupted)
                  |current error: ${errorOf(instances.current.deserialize[T](variant))}
                  |legacy error:  ${errorOf(instances.legacy.deserialize[T](variant))}
                  |variant:  ${Deep.repr(variant)}
                  |current:  ${currentOutcomes._1}
                  |legacy:   ${legacyOutcomes._1}
                  |current iterator: ${currentOutcomes._2}
                  |legacy iterator:  ${legacyOutcomes._2}""".stripMargin)
        }
        if (!currentOutcomes._1.startsWith("FAILURE")) successes += 1
        checked += 1
      }
    }
    info(s"$name: $checked variants checked, $successes deserialized successfully")
  }

  "Current deserializer" must "behave like 0.7.2 for primitives" in check[Prims]("Prims", _.prims())
  it must "behave like 0.7.2 for java boxed primitives" in check[Boxed]("Boxed", _.boxed())
  it must "behave like 0.7.2 for collections" in check[Colls]("Colls", _.colls())
  it must "behave like 0.7.2 for maps" in check[Maps]("Maps", _.maps())
  it must "behave like 0.7.2 for arrays" in check[Arrays]("Arrays", _.arrays())
  it must "behave like 0.7.2 for options, eithers and tuples" in check[Opts]("Opts", _.opts())
  it must "behave like 0.7.2 for typed classes" in check[Zoo]("Zoo", _.zoo())
  it must "behave like 0.7.2 for recursive types" in check[io.mpjsons.fuzz.Tree]("Tree", _.tree())
  it must "behave like 0.7.2 for beans" in check[Bean]("Bean", _.bean())
  it must "behave like 0.7.2 for beans with final fields" in check[BeanWithFinals]("BeanWithFinals", _.beanWithFinals())
  it must "behave like 0.7.2 for generic types" in check[UsesGeneric]("UsesGeneric", _.usesGeneric())
  it must "behave like 0.7.2 for top level collections" in check[List[Map[String, Option[Inner]]]]("TopLevel",
    g => List.fill(g.size(4))(List.fill(g.size(3))(g.word() -> (if (g.r.nextBoolean()) Some(g.inner()) else None)).toMap))
  it must "behave like 0.7.2 for top level primitives" in check[Long]("TopLevelLong", _.long())
  it must "behave like 0.7.2 for top level strings" in check[String]("TopLevelString", _.string())
  it must "behave like 0.7.2 for top level doubles" in check[Double]("TopLevelDouble", _.double())
  it must "behave like 0.7.2 for top level floats" in check[Float]("TopLevelFloat", _.float())
}
