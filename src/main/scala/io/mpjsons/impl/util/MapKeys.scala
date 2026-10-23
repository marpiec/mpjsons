package io.mpjsons.impl.util

import scala.reflect.runtime.universe._

/**
 * Support for writing maps as JSON objects: which key types can be rendered as object keys, and how to parse
 * such a key back from its string form.
 */
object MapKeys {

  private val supported: List[(Type, String => Any)] = List(
    typeOf[String] -> ((s: String) => s),
    typeOf[Int] -> ((s: String) => s.toInt),
    typeOf[java.lang.Integer] -> ((s: String) => java.lang.Integer.valueOf(s)),
    typeOf[Long] -> ((s: String) => s.toLong),
    typeOf[java.lang.Long] -> ((s: String) => java.lang.Long.valueOf(s)),
    typeOf[Short] -> ((s: String) => s.toShort),
    typeOf[java.lang.Short] -> ((s: String) => java.lang.Short.valueOf(s)),
    typeOf[Byte] -> ((s: String) => s.toByte),
    typeOf[java.lang.Byte] -> ((s: String) => java.lang.Byte.valueOf(s)),
    typeOf[Boolean] -> ((s: String) => s.toBoolean),
    typeOf[java.lang.Boolean] -> ((s: String) => java.lang.Boolean.valueOf(s)),
    typeOf[Char] -> ((s: String) => singleChar(s)),
    typeOf[java.lang.Character] -> ((s: String) => java.lang.Character.valueOf(singleChar(s))))

  private def singleChar(s: String): Char = {
    if (s.length == 1) s.charAt(0) else throw new IllegalArgumentException("Char map key must be exactly one character: [" + s + "]")
  }

  /** True when a map with this key type can be written as a JSON object. */
  def supportsObjectForm(keyType: Type): Boolean = supported.exists(_._1.typeSymbol == keyType.typeSymbol)

  /** Parser from the object-key string to the key value, or None when the key type has no object form. */
  def parser(keyType: Type): Option[String => Any] = supported.find(_._1.typeSymbol == keyType.typeSymbol).map(_._2)
}
