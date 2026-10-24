package io.mpjsons.fuzz

import java.nio.charset.StandardCharsets

import io.mpjsons.impl.StringIterator
import io.mpjsons.{MPJsons, MPJsonsConfig, NullPolicy, jsonName}
import org.scalatest.flatspec.AnyFlatSpec

import scala.reflect.runtime.universe._
import scala.util.Random

/** Field names of equal length, which jsonb orders by bytes. */
case class ManyFields(id: Int, bb: Long, ab: Int, ba: String, name: String, zz: Option[Int], tags: List[String],
                      kind: String, abcde: Double, createdAt: Long, nested: Option[ManyFields])

/**
 * A name whose UTF-8 length (7 bytes) differs from its length (4 chars), so jsonb puts it after "abcde". Renamed with
 * @jsonName, which the 0.7.2 copy does not see (it reads its own io.mpjsons.legacy.jsonName), so this model is checked
 * against the current reader itself.
 */
case class NonAsciiNames(@jsonName("żółw") turtle: String, abcde: Int, id: Long, @jsonName("é") e: Int)

/**
 * Re-renders JSON with the same values in other shapes: key order (as is, PostgreSQL jsonb's, reversed as 0.6.x wrote
 * it, shuffled), whitespace between tokens, and the escaping of strings and keys.
 */
object Reshaper {
  private sealed trait J
  private final case class JObj(fields: List[(String, J)]) extends J
  private final case class JArr(values: List[J]) extends J
  private final case class JStr(value: String) extends J
  private final case class JRaw(text: String) extends J
}

final class Reshaper(r: Random) {

  import Reshaper._

  sealed trait KeyOrder
  case object AsIs extends KeyOrder
  case object Jsonb extends KeyOrder
  case object Reversed extends KeyOrder
  case object Shuffled extends KeyOrder

  /** jsonb's text: keys sorted by UTF-8 length then bytes, ", " and ": ", only quote, backslash and controls escaped. */
  def jsonb(json: String): String = render(json, Jsonb, jsonbSpacing = true, randomWhitespace = false, randomEscapes = false)

  def render(json: String, order: KeyOrder, jsonbSpacing: Boolean, randomWhitespace: Boolean, randomEscapes: Boolean): String = {
    val out = new java.lang.StringBuilder
    write(new Parser(json).value(), order, jsonbSpacing, randomWhitespace, randomEscapes, out)
    out.toString
  }

  private def ws(randomWhitespace: Boolean, out: java.lang.StringBuilder): Unit = {
    if (randomWhitespace) {
      for (_ <- 0 until r.nextInt(3)) out.append(" \t\n\r\u2003".charAt(r.nextInt(5)))
    }
  }

  private def write(value: J, order: KeyOrder, jsonbSpacing: Boolean, randomWhitespace: Boolean, randomEscapes: Boolean,
                    out: java.lang.StringBuilder): Unit = value match {
    case JObj(fields) =>
      val ordered = order match {
        case AsIs => fields
        case Reversed => fields.reverse
        case Shuffled => r.shuffle(fields)
        case Jsonb => fields.sortWith { case ((a, _), (b, _)) =>
          val ab = a.getBytes(StandardCharsets.UTF_8)
          val bb = b.getBytes(StandardCharsets.UTF_8)
          if (ab.length != bb.length) ab.length < bb.length else java.util.Arrays.compareUnsigned(ab, bb) < 0
        }
      }
      out.append('{')
      ws(randomWhitespace, out)
      ordered.zipWithIndex.foreach { case ((key, v), i) =>
        if (i > 0) {
          out.append(if (jsonbSpacing) ", " else ",")
          ws(randomWhitespace, out)
        }
        string(key, randomEscapes, out)
        ws(randomWhitespace, out)
        out.append(if (jsonbSpacing) ": " else ":")
        ws(randomWhitespace, out)
        write(v, order, jsonbSpacing, randomWhitespace, randomEscapes, out)
        ws(randomWhitespace, out)
      }
      out.append('}')
    case JArr(values) =>
      out.append('[')
      ws(randomWhitespace, out)
      values.zipWithIndex.foreach { case (v, i) =>
        if (i > 0) {
          out.append(if (jsonbSpacing) ", " else ",")
          ws(randomWhitespace, out)
        }
        write(v, order, jsonbSpacing, randomWhitespace, randomEscapes, out)
        ws(randomWhitespace, out)
      }
      out.append(']')
    case JStr(s) => string(s, randomEscapes, out)
    case JRaw(text) => out.append(text)
  }

  private def string(s: String, randomEscapes: Boolean, out: java.lang.StringBuilder): Unit = {
    out.append('"')
    s.foreach { c =>
      if (randomEscapes && r.nextInt(8) == 0) {
        val hex = f"${c.toInt}%04x"
        out.append("\\u").append(if (r.nextBoolean()) hex else hex.toUpperCase)
      } else c match {
        case '"' => out.append("\\\"")
        case '\\' => out.append("\\\\")
        case '/' if randomEscapes && r.nextBoolean() => out.append("\\/")
        case '\b' => out.append("\\b")
        case '\f' => out.append("\\f")
        case '\n' => out.append("\\n")
        case '\r' => out.append("\\r")
        case '\t' => out.append("\\t")
        case c if c < ' ' => out.append(f"\\u${c.toInt}%04x")
        case c => out.append(c)
      }
    }
    out.append('"')
  }

  private final class Parser(s: String) {
    private var i = 0

    private def skip(): Unit = while (i < s.length && Character.isWhitespace(s.charAt(i))) i += 1

    def value(): J = {
      skip()
      s.charAt(i) match {
        case '{' =>
          i += 1
          skip()
          var fields = List.empty[(String, J)]
          if (s.charAt(i) == '}') i += 1
          else {
            var more = true
            while (more) {
              skip()
              val key = string()
              skip()
              i += 1
              fields ::= key -> value()
              skip()
              more = s.charAt(i) == ','
              i += 1
            }
          }
          JObj(fields.reverse)
        case '[' =>
          i += 1
          skip()
          var values = List.empty[J]
          if (s.charAt(i) == ']') i += 1
          else {
            var more = true
            while (more) {
              values ::= value()
              skip()
              more = s.charAt(i) == ','
              i += 1
            }
          }
          JArr(values.reverse)
        case '"' => JStr(string())
        case _ =>
          val start = i
          while (i < s.length && ",}] \n\t\r".indexOf(s.charAt(i).toInt) < 0) i += 1
          JRaw(s.substring(start, i))
      }
    }

    private def string(): String = {
      i += 1
      val out = new java.lang.StringBuilder
      while (s.charAt(i) != '"') {
        if (s.charAt(i) == '\\') {
          i += 1
          s.charAt(i) match {
            case 'b' => out.append('\b')
            case 'f' => out.append('\f')
            case 'n' => out.append('\n')
            case 'r' => out.append('\r')
            case 't' => out.append('\t')
            case 'u' => out.append(Integer.parseInt(s.substring(i + 1, i + 5), 16).toChar); i += 4
            case c => out.append(c)
          }
        } else {
          out.append(s.charAt(i))
        }
        i += 1
      }
      i += 1
      out.toString
    }
  }
}

/**
 * Reading JSON in other shapes than mpjsons writes (jsonb key order and spacing, other key orders, whitespace,
 * escapes) must give exactly what the 0.7.2 copy gives: same value or failure, same final iterator state.
 */
class ReshapedInputDifferentialSpec extends AnyFlatSpec {

  private val valuesPerModel = Integer.getInteger("fuzz.values", 60).intValue()
  private val seed = java.lang.Long.getLong("fuzz.seed", 20261024L).longValue()

  private case class Instances(name: String, current: MPJsons, legacy: io.mpjsons.legacy.MPJsons, strict: Boolean)

  private def instances(name: String, ignoreNonExistingFields: Boolean = false, strict: Boolean = false, omitNulls: Boolean = false) = {
    val current = new MPJsons(MPJsonsConfig(ignoreNonExistingFields = ignoreNonExistingFields, strict = strict,
      nullPolicy = if (omitNulls) NullPolicy.Omit else NullPolicy.Fail))
    val legacy = new io.mpjsons.legacy.MPJsons(io.mpjsons.legacy.MPJsonsConfig(ignoreNonExistingFields = ignoreNonExistingFields, strict = strict,
      nullPolicy = if (omitNulls) io.mpjsons.legacy.NullPolicy.Omit else io.mpjsons.legacy.NullPolicy.Fail))
    current.markTypedClass[Animal]()
    legacy.markTypedClass[Animal]()
    Instances(name, current, legacy, strict)
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
    val reshaper = new Reshaper(random)
    val tpe = tag.tpe
    var checked = 0
    for (_ <- 0 until valuesPerModel) {
      val json = serializer.serialize[T](generate(gen))
      val variants = Seq(
        reshaper.jsonb(json),
        reshaper.render(json, reshaper.Jsonb, jsonbSpacing = false, randomWhitespace = false, randomEscapes = false),
        reshaper.render(json, reshaper.Reversed, jsonbSpacing = false, randomWhitespace = false, randomEscapes = false),
        reshaper.render(json, reshaper.Shuffled, jsonbSpacing = true, randomWhitespace = false, randomEscapes = false),
        reshaper.render(json, reshaper.AsIs, jsonbSpacing = false, randomWhitespace = true, randomEscapes = false),
        reshaper.render(json, reshaper.Shuffled, jsonbSpacing = false, randomWhitespace = true, randomEscapes = true),
        reshaper.render(json, reshaper.Jsonb, jsonbSpacing = true, randomWhitespace = false, randomEscapes = true))
      for ((variant, v) <- variants.zipWithIndex; instances <- configurations) {
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
          fail(s"""Difference for $name, variant $v (configuration: ${instances.name})
                  |current error: ${errorOf(instances.current.deserialize[T](variant))}
                  |legacy error:  ${errorOf(instances.legacy.deserialize[T](variant))}
                  |current: ${currentOutcomes.toString.take(1500)}
                  |legacy:  ${legacyOutcomes.toString.take(1500)}
                  |input:   ${variant.take(1500)}""".stripMargin)
        }
        checked += 1
      }
    }
    info(s"$name: $checked inputs checked")
  }

  private def manyFields(gen: Gen, depth: Int = 0): ManyFields = ManyFields(gen.int(), gen.long(), gen.int(), gen.string(), gen.string(),
    if (gen.size(1) == 0) None else Some(gen.int()), List.fill(gen.size(3))(gen.string()), gen.string(), gen.double(), gen.long(),
    if (depth < 2 && gen.size(1) > 0) Some(manyFields(gen, depth + 1)) else None)

  "Reading reshaped input" must "match 0.7.2 for primitives" in check[Prims]("Prims", _.prims())
  it must "match 0.7.2 for java boxed primitives" in check[Boxed]("Boxed", _.boxed())
  it must "match 0.7.2 for collections" in check[Colls]("Colls", _.colls())
  it must "match 0.7.2 for maps" in check[Maps]("Maps", _.maps())
  it must "match 0.7.2 for arrays" in check[Arrays]("Arrays", _.arrays())
  it must "match 0.7.2 for options, eithers and tuples" in check[Opts]("Opts", _.opts())
  it must "match 0.7.2 for typed classes" in check[Zoo]("Zoo", _.zoo())
  it must "match 0.7.2 for recursive types" in check[io.mpjsons.fuzz.Tree]("Tree", _.tree())
  it must "match 0.7.2 for beans" in check[Bean]("Bean", _.bean())
  it must "match 0.7.2 for beans with final fields" in check[BeanWithFinals]("BeanWithFinals", _.beanWithFinals())
  it must "match 0.7.2 for generic types" in check[UsesGeneric]("UsesGeneric", _.usesGeneric())
  it must "match 0.7.2 for many fields with tied and non-ASCII names" in check[ManyFields]("ManyFields", g => manyFields(g))

  it must "read renamed and non-ASCII keys in every key order" in {
    val r = new Random(seed)
    val reshaper = new Reshaper(r)
    val current = new MPJsons()
    for (_ <- 0 until 500) {
      val value = NonAsciiNames(new Gen(r).string(), r.nextInt(), r.nextLong(), r.nextInt())
      val json = serializer.serialize(value)
      val variants = Seq(json, reshaper.jsonb(json),
        reshaper.render(json, reshaper.Reversed, jsonbSpacing = false, randomWhitespace = true, randomEscapes = true),
        reshaper.render(json, reshaper.Shuffled, jsonbSpacing = true, randomWhitespace = false, randomEscapes = true))
      for (variant <- variants) {
        val read = current.deserialize[NonAsciiNames](variant)
        if (read != value) fail(s"Read $read instead of $value from $variant")
      }
    }
  }

  it must "read strings with any escapes like 0.7.2, including errors" in {
    val r = new Random(seed)
    val current = new MPJsons()
    val legacy = new io.mpjsons.legacy.MPJsons()
    def full(f: => Any): String = try String.valueOf(f) catch {
      case e: Throwable => "FAILURE " + e.getClass.getName.replace("io.mpjsons.legacy.", "io.mpjsons.") + ": " + e.getMessage
    }
    val pieces = Array("a", "ż", "\\\"", "\\\\", "\\/", "\\n", "\\t", "\\b", "\\f", "\\r", "\\u0041", "\\u00e9", "\\u00E9", "\\ud83d\\ude00",
      "\\u+041", "\\u-041", "\\uFF11FF", "\\u\uFF11\uFF12\uFF13\uFF14", "\\u12", "\\x", "\\", "\"", "/", "  ", "abc")
    var checked = 0
    for (_ <- 0 until 20000) {
      val body = Seq.fill(r.nextInt(12))(pieces(r.nextInt(pieces.length))).mkString
      val json = r.nextInt(10) match {
        case 0 => "\"" + body // unterminated
        case 1 => "  \"" + body + "\"  "
        case _ => "\"" + body + "\""
      }
      val c = full(current.deserialize[String](json))
      val l = full(legacy.deserialize[String](json))
      if (c != l) fail(s"Difference for input [$json]\ncurrent: $c\nlegacy:  $l")
      checked += 1
    }
    info(s"$checked strings checked")
  }
}
