package io.mpjsons.impl.factoryimpl

import io.mpjsons.impl.util.Context

import scala.reflect.runtime.universe._

/**
 * Substitutes type parameters with the concrete types bound in the given context.
 */
private[factoryimpl] object TypeParamsResolver {

  /**
   * Returns a copy of `tpe` in which every type parameter symbol that is bound in `context.typeParams`
   * is replaced by its binding, recursively (so `List[T]` becomes `List[Int]` when `T -> Int`).
   * Throws if an unbound type parameter is encountered, because no (de)serializer can be built for it.
   */
  def resolve(tpe: Type, context: Context): Type = {
    if (tpe.typeSymbol.isParameter) {
      bound(tpe, context)
    } else if (context.typeParams.isEmpty || tpe.typeArgs.isEmpty) {
      tpe
    } else {
      tpe.map { t =>
        if (t.typeSymbol.isParameter) bound(t, context) else t
      }
    }
  }

  private def bound(param: Type, context: Context): Type = {
    context.typeParams.getOrElse(param.typeSymbol, {
      throw new IllegalArgumentException("Unbound type parameter " + param + ", be sure to define types everywhere. Types: " + context.typesStackMessage)
    })
  }
}
