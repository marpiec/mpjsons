package io.mpjsons.impl.deserializer

import java.lang.reflect.Field

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.util.reflection.ReflectionUtil
import io.mpjsons.impl.util.{ObjectConstructionUtil, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class BeanDeserializer[T](deserializerFactory: DeserializerFactory,
                                private val tpe: Type, private val context: Map[Symbol, Type]) extends JsonTypeDeserializer[T] {



  val clazz = TypesUtil.getClassFromType[T](tpe)
  val constructor = try{clazz.getConstructor()} catch {case e: NoSuchMethodException => null}
  lazy val fields = ReflectionUtil.getAllAccessibleFields(tpe)
  /** Lazy val to prevent StackOverflow while construction recursive type deserializer */
  lazy val fieldsByName: Map[String,(Field, JsonTypeDeserializer[AnyRef])] = fields.map {field =>
    field.field.getName -> (field.field, deserializerFactory.getDeserializer(field.tpe, context ++ tpe.typeSymbol.typeSignature.typeParams.zip(tpe.typeArgs).toMap).asInstanceOf[JsonTypeDeserializer[AnyRef]])}.toMap



  override def deserialize(jsonIterator: StringIterator): T = {

    jsonIterator.consumeObjectStart()
    val instance = ObjectConstructionUtil.createInstance[T](clazz, constructor)

    while (jsonIterator.currentChar != '}') {

      val identifier = IdentifierDeserializer.deserialize(jsonIterator)

      jsonIterator.consumeFieldValueSeparator()

      val (field, deserializer) = fieldsByName.getOrElse(identifier, throw new IllegalArgumentException(s"No field $identifier in type $tpe"))
      val value = deserializer.deserialize(jsonIterator)

      field.set(instance, value)

      jsonIterator.skipWhitespaceChars()
      if (jsonIterator.currentChar == ',') {
        jsonIterator.nextChar()
      }
    }

    jsonIterator.nextCharOrNullIfLast

    instance
  }

}
