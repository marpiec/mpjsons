package io.mpjsons.legacy.impl.util

import io.mpjsons.legacy.MPJsonsConfig

import scala.reflect.runtime.universe._

/**
 * Tells, from a Scala type, which JSON token its serialized form starts with. Used to decide whether a
 * compact (inline) Option value can be told apart from the historical {"value":...} object form.
 */
object JsonShapes {

  private val scalarTypes: List[Type] = List(
    typeOf[Int], typeOf[Long], typeOf[Short], typeOf[Byte], typeOf[Double], typeOf[Float], typeOf[Boolean], typeOf[Char],
    typeOf[java.lang.Integer], typeOf[java.lang.Long], typeOf[java.lang.Short], typeOf[java.lang.Byte],
    typeOf[java.lang.Double], typeOf[java.lang.Float], typeOf[java.lang.Boolean], typeOf[java.lang.Character],
    typeOf[String], typeOf[StringBuilder], typeOf[BigDecimal], typeOf[BigInt], typeOf[java.math.BigDecimal], typeOf[java.math.BigInteger],
    typeOf[java.util.UUID], typeOf[java.time.Instant], typeOf[java.time.OffsetDateTime], typeOf[java.time.ZonedDateTime])

  private val timeTypes: List[Type] = List(
    typeOf[java.time.LocalDate], typeOf[java.time.LocalTime], typeOf[java.time.LocalDateTime], typeOf[java.time.Duration])

  // Existential types compare correctly with <:< (List[Int] <:< Iterable[_]); Array is matched by symbol because
  // the erasure of Array[_] is Object.
  private val arrayLikeTypes: List[Type] = List(
    typeOf[Iterable[_]], typeOf[scala.collection.Iterable[_]], typeOf[scala.collection.BitSet],
    typeOf[Product1[_]], typeOf[Product2[_, _]], typeOf[Product3[_, _, _]], typeOf[Product4[_, _, _, _]],
    typeOf[Product5[_, _, _, _, _]], typeOf[Product6[_, _, _, _, _, _]])

  private val mapType = typeOf[scala.collection.Map[_, _]]

  /** True when the serialized form of `tpe` is a JSON object (bean, Option, Either, singleton, time type, ...). */
  def isObjectLike(tpe: Type, config: MPJsonsConfig): Boolean = {
    val symbol = tpe.typeSymbol
    if (scalarTypes.exists(_.typeSymbol == symbol)) {
      false
    } else if (symbol == definitions.ArrayClass) {
      false
    } else if (timeTypes.exists(_.typeSymbol == symbol)) {
      !config.isoTime // object form unless written as ISO strings
    } else if (tpe <:< mapType) {
      // written as an array of pairs, unless the object form is on and the key type supports it
      config.mapsAsObjects && MapKeys.supportsObjectForm(TypesUtil.getDoubleSubElementsType(tpe)._1)
    } else if (arrayLikeTypes.exists(t => tpe <:< t)) {
      false
    } else {
      true
    }
  }
}
