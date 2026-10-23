package io.mpjsons.impl.deserializer.jsontypes

import java.math.BigInteger

/**
 * Eisel-Lemire algorithm converting decimal mantissa and exponent to the nearest double
 * (D. Lemire, "Number Parsing at a Gigabyte per Second", Software: Practice and Experience, 2021).
 * Port of Go's strconv eiselLemire64. It either returns correctly rounded result (the same as
 * java.lang.Double.parseDouble) or signals that the input has to be parsed by a slower, exact algorithm.
 */
object EiselLemire {

  private final val MinExponent = -348
  private final val MaxExponent = 347

  /**
   * 128-bit approximations (rounded down) of powers of ten, normalized so that the most significant bit is set,
   * stored as (low, high) pairs: powers(2 * i) is low and powers(2 * i + 1) is high 64 bits of 10^(i + MinExponent).
   * Same values as Go's detailedPowersOfTen table (mantissa of 10^e is equal to mantissa of 5^e).
   */
  private val powers: Array[Long] = {
    val table = new Array[Long]((MaxExponent - MinExponent + 1) * 2)
    var exponent = MinExponent
    while (exponent <= MaxExponent) {
      val exact: BigInteger = if (exponent >= 0) {
        BigInteger.valueOf(5).pow(exponent)
      } else {
        val power = BigInteger.valueOf(5).pow(-exponent)
        BigInteger.ONE.shiftLeft(power.bitLength() + 130).divide(power)
      }
      val bits = exact.bitLength()
      val value = if (bits <= 128) exact.shiftLeft(128 - bits) else exact.shiftRight(bits - 128)
      val index = (exponent - MinExponent) * 2
      table(index) = value.longValue()
      table(index + 1) = value.shiftRight(64).longValue()
      exponent += 1
    }
    table
  }

  /** Result value meaning that the algorithm cannot decide and slower algorithm has to be used. */
  final val Failure: Long = -1L

  @inline private def unsignedLess(a: Long, b: Long): Boolean = (a ^ Long.MinValue) < (b ^ Long.MinValue)

  /**
   * @param mantissa decimal mantissa, non negative
   * @param exponent decimal exponent
   * @return bits of the double (without sign) mantissa * 10^exponent, or Failure
   */
  def toDoubleBits(mantissa: Long, exponent: Int): Long = {
    if (mantissa == 0) {
      0L
    } else if (exponent < MinExponent || exponent > MaxExponent) {
      Failure
    } else {
      // Normalization
      val clz = java.lang.Long.numberOfLeadingZeros(mantissa)
      val man = mantissa << clz
      var retExp2 = (((217706L * exponent) >> 16) + 64 + 1023) - clz

      // Multiplication
      val index = (exponent - MinExponent) * 2
      val powerLow = powers(index)
      val powerHigh = powers(index + 1)
      var xHi = Math.unsignedMultiplyHigh(man, powerHigh)
      var xLo = man * powerHigh

      // Wider approximation
      if ((xHi & 0x1FF) == 0x1FF && unsignedLess(xLo + man, man)) {
        val yHi = Math.unsignedMultiplyHigh(man, powerLow)
        val yLo = man * powerLow
        var mergedHi = xHi
        val mergedLo = xLo + yHi
        if (unsignedLess(mergedLo, xLo)) {
          mergedHi += 1
        }
        if ((mergedHi & 0x1FF) == 0x1FF && mergedLo + 1 == 0 && unsignedLess(yLo + man, man)) {
          return Failure
        }
        xHi = mergedHi
        xLo = mergedLo
      }

      // Shifting to 54 bits
      val msb = xHi >>> 63
      var retMantissa = xHi >>> (msb + 9).toInt
      retExp2 -= 1 ^ msb

      // Half-way ambiguity
      if (xLo == 0 && (xHi & 0x1FF) == 0 && (retMantissa & 3) == 1) {
        return Failure
      }

      // From 54 to 53 bits
      retMantissa += retMantissa & 1
      retMantissa >>>= 1
      if ((retMantissa >>> 53) > 0) {
        retMantissa >>>= 1
        retExp2 += 1
      }
      // Zero or underflow means subnormal space, 0x7FF or above means Inf/NaN space
      if (!unsignedLess(retExp2 - 1, 0x7FFL - 1)) {
        Failure
      } else {
        (retExp2 << 52) | (retMantissa & 0x000FFFFFFFFFFFFFL)
      }
    }
  }
}
