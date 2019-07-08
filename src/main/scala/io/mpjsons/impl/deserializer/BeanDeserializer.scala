package io.mpjsons.impl.deserializer

import java.lang.reflect.Field

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.deserializer.jsontypes.{AbstractFloatingPointDeserializer, AbstractStringDeserializer}
import io.mpjsons.impl.util.reflection.ReflectionUtil
import io.mpjsons.impl.util.{Context, ObjectConstructionUtil, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class BeanDeserializer[T](deserializerFactory: DeserializerFactory,
                          private val tpe: Type, private val context: Context) extends JsonTypeDeserializer[T] {


  val clazz = TypesUtil.getClassFromType[T](tpe)
  val constructor = try {
    clazz.getConstructor()
  } catch {
    case e: NoSuchMethodException => null
  }
  lazy val fields = ReflectionUtil.getAllAccessibleFields(tpe)
  /** Lazy val to prevent StackOverflow while construction recursive type deserializer */
  lazy val fieldsByName: Map[String, (Field, JsonTypeDeserializer[AnyRef])] = fields.map { field =>
    field.field.getName ->(field.field, deserializerFactory.getDeserializer(field.tpe, context).asInstanceOf[JsonTypeDeserializer[AnyRef]])
  }.toMap


  override def deserialize(jsonIterator: StringIterator): T = {

    jsonIterator.consumeObjectStart()
    val instance = ObjectConstructionUtil.createInstance[T](clazz, constructor, context)

    while (jsonIterator.currentChar != '}') {

      val identifier = IdentifierDeserializer.deserialize(jsonIterator)

      jsonIterator.consumeFieldValueSeparator()

      fieldsByName.get(identifier) match {
        case Some((field, deserializer)) =>
          val value = deserializer.deserialize(jsonIterator)

          field.set(instance, value)

          jsonIterator.skipWhitespaceChars()
          if (jsonIterator.currentChar == ',') {
            jsonIterator.nextChar()
          }
        case None if deserializerFactory.ignoreNonExistingFields =>
          skipValue(jsonIterator)
          jsonIterator.skipWhitespaceChars()
          if (jsonIterator.currentChar == ',') {
            jsonIterator.nextChar()
          }
        case None => throw new IllegalArgumentException(s"No field [$identifier] in type [$tpe]. Types: ${context.typesStackMessage}")
      }
    }

    jsonIterator.nextCharOrNullIfLast

    instance
  }


  private def skipValue(jsonIterator: StringIterator): Unit = {
    jsonIterator.skipWhitespaceChars()
    jsonIterator.currentChar match {
      case '{' => skipObject(jsonIterator)
      case '[' => skipArray(jsonIterator)
      case '"' => skipString(jsonIterator)
      case '-' | '0' | '1' | '2'| '3'| '4'| '5'| '6'| '7'| '8'| '9' => skipNumber(jsonIterator)
      case 'f' | 't' | 'n'  => skipBooleanOrNull(jsonIterator)
      case c => throw new IllegalArgumentException(s"Invalid start of value: [$c]")
    }
  }



  def skipObject(jsonIterator: StringIterator): Unit = {
    jsonIterator.consumeObjectStart()
    while (jsonIterator.currentChar != '}') {
      IdentifierDeserializer.deserialize(jsonIterator)
      jsonIterator.consumeFieldValueSeparator()
      skipValue(jsonIterator)
      jsonIterator.skipWhitespaceChars()
      if (jsonIterator.currentChar == ',') {
        jsonIterator.nextChar()
      }
    }
    jsonIterator.nextCharOrNullIfLast
  }

  def skipArray(jsonIterator: StringIterator): Unit = {
    jsonIterator.consumeArrayStart()
    jsonIterator.skipWhitespaceChars()
    while (jsonIterator.currentChar != ']') {
      skipValue(jsonIterator)
      jsonIterator.skipWhitespaceChars()
      if (jsonIterator.currentChar == ',') {
        jsonIterator.nextChar()
      }
    }
    jsonIterator.nextChar()
  }

  def skipString(jsonIterator: StringIterator): Unit = {
    AbstractStringDeserializer.readString(jsonIterator)
  }

  def skipNumber(jsonIterator: StringIterator): Unit = {
    AbstractFloatingPointDeserializer.readNumberString(jsonIterator)
  }

  def skipBooleanOrNull(jsonIterator: StringIterator): Unit = {
    val booleanString = new StringBuilder()

    jsonIterator.skipWhitespaceChars()

    while (jsonIterator.isCurrentCharASmallLetter) {
      booleanString.append(jsonIterator.currentChar)
      jsonIterator.nextChar()
    }

    booleanString.toString() match {
      case "true" | "false" | "null" => ()
      case _ => throw new IllegalArgumentException(s"Invalid boolean value: $booleanString")
    }
  }

}
