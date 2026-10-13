package io.mpjsons.impl.util

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
    typeOf[String], typeOf[StringBuilder])

  // Existential types compare correctly with <:< (List[Int] <:< Iterable[_]); Array is matched by symbol because
  // the erasure of Array[_] is Object.
  private val arrayLikeTypes: List[Type] = List(
    typeOf[Iterable[_]], typeOf[scala.collection.Iterable[_]], typeOf[scala.collection.BitSet],
    typeOf[Product1[_]], typeOf[Product2[_, _]], typeOf[Product3[_, _, _]], typeOf[Product4[_, _, _, _]],
    typeOf[Product5[_, _, _, _, _]], typeOf[Product6[_, _, _, _, _, _]])

  private val mapType = typeOf[scala.collection.Map[_, _]]

  /** True when the serialized form of `tpe` is a JSON object (bean, Option, Either, singleton, time type, ...). */
  def isObjectLike(tpe: Type): Boolean = {
    val symbol = tpe.typeSymbol
    if (scalarTypes.exists(_.typeSymbol == symbol)) {
      false
    } else if (symbol == definitions.ArrayClass) {
      false
    } else if (tpe <:< mapType) {
      false // written as an array of pairs
    } else if (arrayLikeTypes.exists(t => tpe <:< t)) {
      false
    } else {
      true
    }
  }
}
