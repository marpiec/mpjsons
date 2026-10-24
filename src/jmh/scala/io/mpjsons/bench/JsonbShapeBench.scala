package io.mpjsons.bench

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

import scala.reflect.runtime.universe._

/** Strings like the ones in stored process definitions: expressions, HTML and JSON embedded in a string. */
case class EscapedDoc(expression: String, html: String, embeddedJson: String, label: String)

case class EscapedDocs(docs: List[EscapedDoc])

object EscapedData {
  private val random = new scala.util.Random(7)

  private def word(): String = new String(Array.fill(3 + random.nextInt(8))(('a' + random.nextInt(26)).toChar))

  val docs: EscapedDocs = EscapedDocs(List.tabulate(200) { i =>
    EscapedDoc(
      expression = "if (" + word() + " > " + random.nextInt(100) + ") {\n  return \"" + word() + "\" + " + word() + ";\n} else {\n\treturn \"" + word() + "\";\n}",
      html = "<div class=\"row\"><span style=\"color:red\">" + word() + " ąęó " + word() + "</span>\n<a href=\"/" + word() + "/" + word() + "\">" + word() + "</a></div>",
      embeddedJson = "{\"id\":" + random.nextInt(100000) + ",\"name\":\"" + word() + "\",\"tags\":[\"" + word() + "\",\"" + word() + "\"],\"nested\":{\"value\":\"" + word() + "\"}}",
      label = word() + " " + word())
  })
}

/**
 * Rewrites JSON as PostgreSQL prints a jsonb value: object keys sorted by length, then by bytes (jsonb does not keep key
 * order), ", " and ": " separators, strings escaped the way jsonb does it ('/' not escaped, non-ASCII kept), numbers in
 * plain decimal notation. Values are unchanged.
 */
object JsonbShape {

  private sealed trait J
  private final case class JObj(fields: List[(String, J)]) extends J
  private final case class JArr(values: List[J]) extends J
  private final case class JStr(value: String) extends J
  private final case class JRaw(text: String) extends J

  def apply(json: String): String = apply(json, sortKeys = true, spaces = true)

  /** Keys of every object in reverse order (as 0.6.x wrote them), compact. */
  def reversed(json: String): String = {
    val out = new java.lang.StringBuilder
    render(new Parser(json).value(), out, KeyOrder.Reversed, ",", ":")
    out.toString
  }

  private object KeyOrder extends Enumeration {
    val AsIs, Sorted, Reversed = Value
  }

  /** Only one aspect of the jsonb shape, to measure each on its own. */
  def apply(json: String, sortKeys: Boolean, spaces: Boolean): String = {
    val parser = new Parser(json)
    val value = parser.value()
    val out = new java.lang.StringBuilder
    render(value, out, if (sortKeys) KeyOrder.Sorted else KeyOrder.AsIs, if (spaces) ", " else ",", if (spaces) ": " else ":")
    out.toString
  }

  private def render(value: J, out: java.lang.StringBuilder, order: KeyOrder.Value, comma: String, colon: String): Unit = value match {
    case JObj(fields) =>
      val sorted = if (order == KeyOrder.Reversed) fields.reverse else if (order == KeyOrder.AsIs) fields else fields.sortWith { case ((a, _), (b, _)) =>
        val ab = a.getBytes("UTF-8")
        val bb = b.getBytes("UTF-8")
        if (ab.length != bb.length) ab.length < bb.length else java.util.Arrays.compareUnsigned(ab, bb) < 0
      }
      out.append('{')
      sorted.zipWithIndex.foreach { case ((key, v), i) =>
        if (i > 0) out.append(comma)
        renderString(key, out)
        out.append(colon)
        render(v, out, order, comma, colon)
      }
      out.append('}')
    case JArr(values) =>
      out.append('[')
      values.zipWithIndex.foreach { case (v, i) =>
        if (i > 0) out.append(comma)
        render(v, out, order, comma, colon)
      }
      out.append(']')
    case JStr(s) => renderString(s, out)
    case JRaw(text) =>
      val isNumber = text.nonEmpty && (text.charAt(0) == '-' || Character.isDigit(text.charAt(0)))
      if (isNumber && text.exists(c => c == 'e' || c == 'E')) out.append(new java.math.BigDecimal(text).toPlainString) else out.append(text)
  }

  /** jsonb's escape_json: quote, backslash and control characters only. */
  private def renderString(s: String, out: java.lang.StringBuilder): Unit = {
    out.append('"')
    s.foreach {
      case '"' => out.append("\\\"")
      case '\\' => out.append("\\\\")
      case '\b' => out.append("\\b")
      case '\f' => out.append("\\f")
      case '\n' => out.append("\\n")
      case '\r' => out.append("\\r")
      case '\t' => out.append("\\t")
      case c if c < ' ' => out.append(f"\\u${c.toInt}%04x")
      case c => out.append(c)
    }
    out.append('"')
  }

  private final class Parser(s: String) {
    private var i = 0

    private def ws(): Unit = while (i < s.length && Character.isWhitespace(s.charAt(i))) i += 1

    def value(): J = {
      ws()
      s.charAt(i) match {
        case '{' =>
          i += 1
          ws()
          var fields = List.empty[(String, J)]
          if (s.charAt(i) == '}') i += 1
          else {
            var more = true
            while (more) {
              ws()
              val key = string()
              ws()
              i += 1 // ':'
              fields ::= key -> value()
              ws()
              more = s.charAt(i) == ','
              i += 1
            }
          }
          JObj(fields.reverse)
        case '[' =>
          i += 1
          ws()
          var values = List.empty[J]
          if (s.charAt(i) == ']') i += 1
          else {
            var more = true
            while (more) {
              values ::= value()
              ws()
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
 * Reading JSON in the shape a jsonb column returns it (see JsonbShape) against the compact JSON mpjsons writes,
 * for the 0.7.2 copy and the current implementation.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
class JsonbShapeBench {

  @Param(Array("legacy", "current"))
  var impl: String = _

  /**
   * compact: as mpjsons writes; spaced: jsonb separators only; sorted: jsonb key order only; jsonb: both; reversed:
   * compact with every object's keys in reverse order (as 0.6.x wrote them).
   */
  @Param(Array("compact", "spaced", "sorted", "jsonb", "reversed"))
  var shape: String = _

  /**
   * latin1: the fixtures as they are; polish: one Polish letter in a string value, which makes Java store the whole
   * input as UTF-16 (the escaped documents always contain Polish letters).
   */
  @Param(Array("latin1", "polish"))
  var text: String = _

  var userData: User = _
  var ordersData: Orders = _

  var userJson: String = _
  var ordersJson: String = _
  var drawingJson: String = _
  var docsJson: String = _

  var user: String => AnyRef = _
  var orders: String => AnyRef = _
  var drawing: String => AnyRef = _
  var docs: String => AnyRef = _
  var ordersApi: String => AnyRef = _

  @Setup
  def setup(): Unit = {
    val writer = new io.mpjsons.MPJsons()
    writer.markTypedClass[Shape]()
    def input(json: String): String = shape match {
      case "compact" => json
      case "spaced" => JsonbShape(json, sortKeys = false, spaces = true)
      case "sorted" => JsonbShape(json, sortKeys = true, spaces = false)
      case "jsonb" => JsonbShape(json)
      case "reversed" => JsonbShape.reversed(json)
    }
    userData = if (text == "polish") Data.smallUser.copy(firstName = "Paweł") else Data.smallUser
    ordersData = if (text == "polish") Orders(Data.orders.orders.updated(0, Data.orders.orders(0).copy(note = Some("zażółć")))) else Data.orders
    userJson = input(writer.serialize(userData))
    ordersJson = input(writer.serialize(ordersData))
    drawingJson = input(writer.serialize(Data.drawing))
    docsJson = input(writer.serialize(EscapedData.docs))

    if (impl == "legacy") {
      val m = new io.mpjsons.legacy.MPJsons()
      m.markTypedClass[Shape]()
      user = staticOf(m.buildStaticDeserializer[User])
      orders = staticOf(m.buildStaticDeserializer[Orders])
      drawing = staticOf(m.buildStaticDeserializer[Drawing])
      docs = staticOf(m.buildStaticDeserializer[EscapedDocs])
      val ordersType = typeOf[Orders]
      ordersApi = json => m.deserialize[Orders](json, ordersType)
    } else {
      val m = new io.mpjsons.MPJsons()
      m.markTypedClass[Shape]()
      user = staticOf(m.buildStaticDeserializer[User])
      orders = staticOf(m.buildStaticDeserializer[Orders])
      drawing = staticOf(m.buildStaticDeserializer[Drawing])
      docs = staticOf(m.buildStaticDeserializer[EscapedDocs])
      val ordersType = typeOf[Orders]
      ordersApi = json => m.deserialize[Orders](json, ordersType)
    }

    // both shapes must read the same values
    require(orders(ordersJson) == ordersData, "orders differ for " + impl + "/" + shape)
    require(drawing(drawingJson) == Data.drawing, "drawing differs for " + impl + "/" + shape)
    require(docs(docsJson) == EscapedData.docs, "docs differ for " + impl + "/" + shape)
    require(user(userJson) == userData, "user differs for " + impl + "/" + shape)
  }

  private def staticOf[T](d: io.mpjsons.legacy.StaticDeserializer[T]): String => AnyRef = json => d.deserialize(json).asInstanceOf[AnyRef]

  private def staticOf[T](d: io.mpjsons.StaticDeserializer[T]): String => AnyRef = json => d.deserialize(json).asInstanceOf[AnyRef]

  @Benchmark def readUser(): AnyRef = user(userJson)
  @Benchmark def readOrders(): AnyRef = orders(ordersJson)
  /** As Neula reads: deserialize(json, tpe) on the instance, which looks the deserializer up per call. */
  @Benchmark def readOrdersByType(): AnyRef = ordersApi(ordersJson)
  @Benchmark def readTypedShapes(): AnyRef = drawing(drawingJson)
  @Benchmark def readEscapedDocs(): AnyRef = docs(docsJson)
}

/** Prints the beginning of each fixture in both shapes (sbt "Jmh/runMain io.mpjsons.bench.JsonbShapeSample"). */
object JsonbShapeSample {
  def main(args: Array[String]): Unit = {
    val writer = new io.mpjsons.MPJsons()
    writer.markTypedClass[Shape]()
    Seq("user" -> writer.serialize(Data.smallUser), "orders" -> writer.serialize(Data.orders),
      "drawing" -> writer.serialize(Data.drawing), "docs" -> writer.serialize(EscapedData.docs)).foreach { case (name, json) =>
      println("compact " + name + " (" + json.length + "): " + json.take(230))
      val jsonb = JsonbShape(json)
      println("jsonb   " + name + " (" + jsonb.length + "): " + jsonb.take(230))
    }
  }
}
