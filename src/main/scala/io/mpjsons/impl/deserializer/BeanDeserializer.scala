package io.mpjsons.impl.deserializer

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.deserializer.jsontypes.{AbstractFloatingPointDeserializer, AbstractStringDeserializer}
import io.mpjsons.impl.util.reflection.ReflectionUtil
import io.mpjsons.impl.util.{Context, ObjectConstructionUtil, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import java.lang.reflect.Field
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class BeanDeserializer[T](deserializerFactory: DeserializerFactory,
                          private val tpe: Type, private val context: Context, private val ignoreNonExistingFields: Boolean = false) extends JsonTypeDeserializer[T] {


  val clazz = TypesUtil.getClassFromType[T](tpe)
  val constructor = try {
    clazz.getConstructor()
  } catch {
    case e: NoSuchMethodException => null
  }
  val fields = ReflectionUtil.getAllAccessibleFields(tpe)

  /**
   * Keys accepted for each field: its JSON name, plus the Scala field name as a fallback when `@jsonName`
   * renamed it (so JSON written before the rename still reads). A fallback is not registered when another
   * field already owns that name as its JSON name.
   * Lazy val to prevent StackOverflow while construction recursive type deserializer.
   */
  lazy val fieldTable: BeanFieldTable = {
    val typeName = tpe.toString
    val jsonNames = fields.map(_.jsonName).toSet
    val entries = fields.flatMap { field =>
      val deserializer = deserializerFactory.getDeserializer(field.tpe, context)
      val primary = BeanField(field.jsonName, field.field, field.tpe, typeName, deserializer)
      val scalaName = field.field.getName
      if (scalaName != field.jsonName && !jsonNames.contains(scalaName)) {
        Array(primary, BeanField(scalaName, field.field, field.tpe, typeName, deserializer))
      } else {
        Array(primary)
      }
    }
    new BeanFieldTable(entries)
  }
  /** Option fields are initialised to None when construction left them null, so an absent key reads as None. */
  private lazy val optionFields: Array[Field] = fields.filter(_.tpe <:< BeanDeserializer.optionType).map(_.field)


  override def deserialize(jsonIterator: StringIterator): T = {

    jsonIterator.consumeObjectStart()
    val instance = ObjectConstructionUtil.createInstance[T](clazz, constructor, context)
    val table = fieldTable
    initialiseOptions(instance.asInstanceOf[AnyRef])

    jsonIterator.skipWhitespaceChars()
    while (jsonIterator.currentChar != '}') {

      val field = table.find(jsonIterator)

      if (field != null) {
        jsonIterator.consumeFieldValueSeparator()
        field.set(instance.asInstanceOf[AnyRef], jsonIterator)
      } else {
        val identifier = IdentifierDeserializer.deserialize(jsonIterator)
        jsonIterator.consumeFieldValueSeparator()
        if (ignoreNonExistingFields) {
          skipValue(jsonIterator)
        } else {
          throw new IllegalArgumentException(s"No field [$identifier] in type [$tpe]. Types: ${context.typesStackMessage}")
        }
      }

      jsonIterator.afterValue('}')
    }

    jsonIterator.nextCharOrNullIfLast

    instance
  }

  private def initialiseOptions(instance: AnyRef): Unit = {
    val options = optionFields
    var index = 0
    while (index < options.length) {
      val field = options(index)
      if (field.get(instance) == null) {
        field.set(instance, None)
      }
      index += 1
    }
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
    jsonIterator.skipWhitespaceChars()
    while (jsonIterator.currentChar != '}') {
      IdentifierDeserializer.deserialize(jsonIterator)
      jsonIterator.consumeFieldValueSeparator()
      skipValue(jsonIterator)
      jsonIterator.afterValue('}')
    }
    jsonIterator.nextCharOrNullIfLast
  }

  def skipArray(jsonIterator: StringIterator): Unit = {
    jsonIterator.consumeArrayStart()
    jsonIterator.skipWhitespaceChars()
    while (jsonIterator.currentChar != ']') {
      skipValue(jsonIterator)
      jsonIterator.afterValue(']')
    }
    jsonIterator.nextChar()
  }

  def skipString(jsonIterator: StringIterator): Unit = {
    AbstractStringDeserializer.readString(jsonIterator)
  }

  def skipNumber(jsonIterator: StringIterator): Unit = {
    AbstractFloatingPointDeserializer.skipNumber(jsonIterator)
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

object BeanDeserializer {
  private val optionType = typeOf[Option[_]]
}
