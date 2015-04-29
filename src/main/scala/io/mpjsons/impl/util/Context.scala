package io.mpjsons.impl.util

import scala.collection.immutable.Map
import scala.reflect.runtime.universe._

case class Context(typesStack: List[Type], typeParams: Map[Symbol, Type]) {
  def typesStackMessage = typesStack.reverse.mkString(" -> ")
}
