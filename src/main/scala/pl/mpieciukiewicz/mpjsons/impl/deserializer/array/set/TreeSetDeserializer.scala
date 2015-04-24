package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.set

import pl.mpieciukiewicz.mpjsons.impl.{StringIterator, DeserializerFactory}
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

import scala.collection.immutable.TreeSet
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

//Unsupported because of missing ordering type class

//class TreeSetDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type)
//  extends AbstractJsonArrayDeserializer[E, TreeSet[E]](deserializerFactory, tpe) {
//
//  override def deserialize(jsonIterator: StringIterator): TreeSet[E] = {
//    TreeSet(deserializeArray(jsonIterator, tpe): _*)
//  }
//
//}
