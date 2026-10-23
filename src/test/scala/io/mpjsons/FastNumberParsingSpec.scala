package io.mpjsons

import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.jsontypes.AbstractFloatingPointDeserializer
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import scala.util.Random

/** Fast floating point parsing must give exactly the same results (and failures) as java.lang parsers. */
class FastNumberParsingSpec extends AnyFlatSpec with Matchers {

  private val random = new Random(12345)

  private def readDouble(text: String): Either[String, Long] = try {
    Right(java.lang.Double.doubleToRawLongBits(AbstractFloatingPointDeserializer.readDouble(new StringIterator(text))))
  } catch {
    case e: NumberFormatException => Left("NumberFormatException")
  }

  private def parseDouble(text: String): Either[String, Long] = try {
    Right(java.lang.Double.doubleToRawLongBits(java.lang.Double.parseDouble(text)))
  } catch {
    case e: NumberFormatException => Left("NumberFormatException")
  }

  private def readFloat(text: String): Either[String, Int] = try {
    Right(java.lang.Float.floatToRawIntBits(AbstractFloatingPointDeserializer.readFloat(new StringIterator(text))))
  } catch {
    case e: NumberFormatException => Left("NumberFormatException")
  }

  private def parseFloat(text: String): Either[String, Int] = try {
    Right(java.lang.Float.floatToRawIntBits(java.lang.Float.parseFloat(text)))
  } catch {
    case e: NumberFormatException => Left("NumberFormatException")
  }

  private def checkDouble(text: String): Unit = {
    withClue(s"double [$text]") {
      readDouble(text) mustBe parseDouble(text)
    }
  }

  private def checkFloat(text: String): Unit = {
    withClue(s"float [$text]") {
      readFloat(text) mustBe parseFloat(text)
    }
  }

  private def randomDecimal(): String = {
    val digits = 1 + random.nextInt(19)
    val mantissa = Seq.fill(digits)(('0' + random.nextInt(10)).toChar).mkString
    val point = random.nextInt(digits + 1)
    val withPoint = if (point == digits || random.nextBoolean()) mantissa else mantissa.substring(0, point) + "." + mantissa.substring(point)
    val sign = if (random.nextBoolean()) "-" else ""
    val exponent = random.nextInt(5) match {
      case 0 => ""
      case 1 => "e" + (random.nextInt(700) - 350)
      case 2 => "E+" + random.nextInt(30)
      case 3 => "E-" + random.nextInt(30)
      case _ => "e" + (random.nextInt(60) - 30)
    }
    sign + withPoint + exponent
  }

  private val hardCases = Seq(
    "0", "-0", "0.0", "-0.0", "0e0", "0e999999", "-0e-999999", "1", "-1", "9007199254740992", "9007199254740993",
    "9007199254740994", "9007199254740995", "123456789012345678", "999999999999999999",
    "2.2250738585072011e-308", "2.2250738585072012e-308", "2.2250738585072014e-308", "4.9e-324", "5e-324", "2.4703282292062327e-324",
    "2.4703282292062328e-324", "1.7976931348623157e308", "1.7976931348623158e308", "1.7976931348623159e308", "1e308", "1e309",
    "1e23", "8.98846567431158e307", "4.940656458412465441765687928682213723651e-324", "0.1", "0.2", "0.3",
    "1.00000005960464477539062499999", "3.4028235e38", "3.4028236e38", "1.4e-45", "7.0e-46", "1.17549435E-38",
    "1e-350", "1e-400", "1e400", "123.456e-2", "00012.5", "-00", "1.", ".5", "-.5", "+1", "+-1", "1e", "1e+", "e5",
    "-", ".", "1-2", "1..2", "1ee2", "1e2.5", "1.5E", "--1", "1+", "12345678901234567890", "1234567890123456789012345")

  "Fast double parsing" must "be the same as Double.parseDouble for hard cases" in {
    hardCases.foreach(checkDouble)
  }

  it must "be the same as Double.parseDouble for Double.toString of random doubles" in {
    for (_ <- 0 until 300000) {
      val d = java.lang.Double.longBitsToDouble(random.nextLong())
      if (!d.isNaN && !d.isInfinite) {
        checkDouble(java.lang.Double.toString(d))
      }
      checkDouble(java.lang.Double.toString(random.nextDouble() * math.pow(10, random.nextInt(40) - 20)))
    }
  }

  it must "be the same as Double.parseDouble for random decimals" in {
    for (_ <- 0 until 500000) {
      checkDouble(randomDecimal())
    }
  }

  it must "be the same as Double.parseDouble for numbers close to halfway between doubles" in {
    for (_ <- 0 until 100000) {
      val d = math.abs(java.lang.Double.longBitsToDouble(random.nextLong()))
      if (!d.isNaN && !d.isInfinite && d != 0) {
        val next = Math.nextUp(d)
        val halfway = new java.math.BigDecimal(d).add(new java.math.BigDecimal(next)).divide(java.math.BigDecimal.valueOf(2))
        checkDouble(halfway.round(new java.math.MathContext(17)).toString)
        checkDouble(halfway.round(new java.math.MathContext(18)).toString)
      }
    }
  }

  "Fast float parsing" must "be the same as Float.parseFloat" in {
    hardCases.foreach(checkFloat)
    for (_ <- 0 until 300000) {
      checkFloat(randomDecimal())
      val f = java.lang.Float.intBitsToFloat(random.nextInt())
      if (!f.isNaN && !f.isInfinite) checkFloat(java.lang.Float.toString(f))
      checkFloat(java.lang.Float.toString(random.nextInt(100000) / 100.0f))
    }
  }

}
