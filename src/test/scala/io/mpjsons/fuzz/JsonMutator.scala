package io.mpjsons.fuzz

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

/**
 * Produces variants of canonical JSON that exercise the lenient JSON flavor accepted by mpjsons
 * (any Character.isWhitespace whitespace, missing and trailing commas, unquoted keys, number spellings, unknown fields)
 * and also random corruptions.
 */
class JsonMutator(r: Random) {

  private val whitespaces = Array(" ", "  ", "\t", "\n", "\r\n", "\u000B", "\u000C", "\u001C", "\u001F", " ", "　", " ", " \n\t ")

  def tokenize(json: String): ArrayBuffer[String] = {
    val tokens = ArrayBuffer[String]()
    var i = 0
    while (i < json.length) {
      val c = json.charAt(i)
      if ("{}[]:,".indexOf(c) >= 0) {
        tokens += c.toString
        i += 1
      } else if (c == '"') {
        var j = i + 1
        while (json.charAt(j) != '"') {
          if (json.charAt(j) == '\\') j += 1
          j += 1
        }
        tokens += json.substring(i, j + 1)
        i = j + 1
      } else {
        var j = i
        while (j < json.length && "{}[]:,".indexOf(json.charAt(j)) < 0) j += 1
        tokens += json.substring(i, j)
        i = j
      }
    }
    tokens
  }

  private def isNumber(t: String) = t.nonEmpty && (t.charAt(0) == '-' || t.charAt(0).isDigit)

  private def randomValueTokens(depth: Int): Seq[String] = r.nextInt(if (depth > 2) 4 else 6) match {
    case 0 =>
      val units = Array("a", "b", "\\\"", "\\\\", "\\n", "\\u0041", "\\/", "x", "{", "}", "[", "]", ",", ":", " ")
      Seq("\"" + Seq.fill(r.nextInt(8))(units(r.nextInt(units.length))).mkString + "\"")
    case 1 => Seq(Seq("1", "-2.5", "0", "3e10", "-1.25E-3", "123456789012345678901234567890")(r.nextInt(6)))
    case 2 => Seq(Seq("true", "false", "null")(r.nextInt(3)))
    case 3 => Seq("\"" + "x" * r.nextInt(5) + "\"")
    case 4 =>
      val elems = Seq.fill(r.nextInt(4))(randomValueTokens(depth + 1))
      Seq("[") ++ (if (elems.isEmpty) Nil else elems.reduce(_ ++ Seq(",") ++ _)) ++ Seq("]")
    case _ =>
      val elems = Seq.fill(r.nextInt(4))(Seq("\"k" + r.nextInt(3) + "\"", ":") ++ randomValueTokens(depth + 1))
      Seq("{") ++ (if (elems.isEmpty) Nil else elems.reduce(_ ++ Seq(",") ++ _)) ++ Seq("}")
  }

  /** @return (variant, true if the variant was randomly corrupted) */
  def mutate(json: String, injectUnknownFields: Boolean): (String, Boolean) = {
    val tokens = tokenize(json)
    val level = r.nextInt(10)

    if (level >= 2) {
      // structural lenience: unquote keys, drop commas, trailing commas, number spellings
      var i = 0
      val out = ArrayBuffer[String]()
      while (i < tokens.length) {
        val t = tokens(i)
        val next = if (i + 1 < tokens.length) tokens(i + 1) else ""
        if (t.startsWith("\"") && next == ":" && r.nextInt(6) == 0 && t.forall(ch => ch.isLetterOrDigit || ch == '"')) {
          out += t.substring(1, t.length - 1)
        } else if (t == "," && r.nextInt(12) == 0) {
          // dropped comma
        } else if (next != ":" && (isNumber(t) || t.startsWith("\"") || t == "true" || t == "false") && r.nextInt(25) == 0) {
          out += "null"
        } else if (isNumber(t) && r.nextInt(5) == 0) {
          out += (r.nextInt(6) match {
            case 0 if t.charAt(0).isDigit => "0" + t
            case 1 => t + ".0"
            case 2 => t.replace('E', 'e')
            case 3 => "+" + t
            case 4 => t + "e0"
            case _ => t
          })
        } else {
          if ((t == "}" || t == "]") && r.nextInt(10) == 0 && out.nonEmpty && out.last != "{" && out.last != "[" && out.last != ",") {
            out += ","
          }
          if (injectUnknownFields && t == "}" && out.nonEmpty && out.last != "{" && r.nextInt(3) == 0) {
            out += ","
            out ++= Seq("\"zz" + r.nextInt(3) + "\"", ":") ++ randomValueTokens(0)
          }
          out += t
          if (injectUnknownFields && t == "{" && next.startsWith("\"") && r.nextInt(3) == 0) {
            out ++= Seq("\"yy" + r.nextInt(3) + "\"", ":") ++ randomValueTokens(0) :+ ","
          }
        }
        i += 1
      }
      tokens.clear()
      tokens ++= out
    }

    val variant = new StringBuilder
    val wsProbability = if (level == 0) 0 else 1 + r.nextInt(4)
    def gap(): Unit = {
      if (wsProbability > 0 && r.nextInt(5) < wsProbability) {
        variant.append(whitespaces(r.nextInt(whitespaces.length)))
      }
    }
    for (t <- tokens) {
      gap()
      variant.append(t)
    }
    gap()

    val v = variant.toString
    if (level == 9 && v.nonEmpty) {
      val chars = "{}[]:,\" \\a1-e.0tn"
      val idx = r.nextInt(v.length)
      val corrupted = r.nextInt(3) match {
        case 0 => v.substring(0, idx) + v.substring(idx + 1)
        case 1 => v.substring(0, idx) + chars.charAt(r.nextInt(chars.length)) + v.substring(idx)
        case _ => v.substring(0, idx)
      }
      (corrupted, true)
    } else {
      (v, false)
    }
  }
}
