package io.mpjsons.impl.util

import scala.collection.immutable.Map
import scala.reflect.runtime.universe._

case class Context(typesStack: List[Type], typeParams: Map[Symbol, Type]) {
  def typesStackMessage = if(typesStack.isEmpty) {
    "(empty)"
  } else {
    "->" + typesStack.reverse.mkString("\n-> ")
  }
}
