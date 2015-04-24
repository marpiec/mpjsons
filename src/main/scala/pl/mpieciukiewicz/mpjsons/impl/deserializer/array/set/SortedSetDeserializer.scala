package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.set

import pl.mpieciukiewicz.mpjsons.impl.{StringIterator, DeserializerFactory}
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

import scala.collection.SortedSet
import scala.collection.immutable.ListSet

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

//Unsupported because of missing ordering type class

//class SortedSetDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type)
//  extends AbstractJsonArrayDeserializer[E, SortedSet[E]](deserializerFactory, tpe) {
//
//  override def deserialize(jsonIterator: StringIterator): SortedSet[E] = {
//    SortedSet(deserializeArray(jsonIterator, tpe): _*)
//  }
//
//}
