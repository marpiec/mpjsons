package io.mpjsons.impl.deserializer

import io.mpjsons.{JsonTypeDeserializer, MPJsonsConfig}
import io.mpjsons.impl.deserializer.jsontypes.{AbstractFloatingPointDeserializer, AbstractStringDeserializer}
import io.mpjsons.impl.util.reflection.{FieldWithTypeInfo, ReflectionUtil}
import io.mpjsons.impl.util.{Context, ObjectConstructionUtil, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import java.lang.reflect.{Constructor, Field}
import scala.reflect.runtime.universe._

/**
 * Deserializes JSON objects into beans and case classes.
 *
 * Default mode: allocate the instance (no-arg constructor, or without any constructor) and write fields
 * reflectively, including final ones. Constructor mode (`MPJsonsConfig(caseClassesViaConstructor = true)`,
 * case classes only): read the values into an argument array and call the primary constructor, so no final
 * field is ever mutated; absent keys get zero / false / null / None, like in the default mode.
 * @author Marcin Pieciukiewicz
 */

class BeanDeserializer[T](deserializerFactory: DeserializerFactory,
                          private val tpe: Type, private val context: Context, config: MPJsonsConfig) extends JsonTypeDeserializer[T] {

  def this(deserializerFactory: DeserializerFactory, tpe: Type, context: Context, ignoreNonExistingFields: Boolean) =
    this(deserializerFactory, tpe, context, MPJsonsConfig(ignoreNonExistingFields = ignoreNonExistingFields))

  def this(deserializerFactory: DeserializerFactory, tpe: Type, context: Context) =
    this(deserializerFactory, tpe, context, MPJsonsConfig())

  private val ignoreNonExistingFields = config.ignoreNonExistingFields

  val clazz = TypesUtil.getClassFromType[T](tpe)
  val constructor = try {
    clazz.getConstructor()
  } catch {
    case e: NoSuchMethodException => null
  }
  val fields = ReflectionUtil.getAllAccessibleFields(tpe)

  /** Primary constructor parameter names in order, when the type is to be built through its constructor. */
  private val constructorParams: Array[String] =
    if (config.caseClassesViaConstructor) BeanDeserializer.caseClassConstructorParams(tpe, fields) else null
  private val primaryConstructor: Constructor[T] =
    if (constructorParams == null) null else BeanDeserializer.findConstructor(clazz, tpe, constructorParams)
  private val useConstructor = primaryConstructor != null

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
      val argIndex = if (useConstructor) constructorParams.indexOf(field.field.getName) else -1
      val primary = BeanField(field.jsonName, field.field, field.tpe, typeName, deserializer, argIndex, field.nullable, config)
      val scalaName = field.field.getName
      if (scalaName != field.jsonName && !jsonNames.contains(scalaName)) {
        Array(primary, BeanField(scalaName, field.field, field.tpe, typeName, deserializer, argIndex, field.nullable, config))
      } else {
        Array(primary)
      }
    }
    new BeanFieldTable(entries)
  }
  /** Option fields are initialised to None when construction left them null, so an absent key reads as None. */
  private lazy val optionFields: Array[Field] = fields.filter(_.tpe <:< BeanDeserializer.optionType).map(_.field)
  /** Constructor arguments for absent keys: zero / false for primitives, None for Options, null otherwise. */
  private lazy val argumentDefaults: Array[AnyRef] =
    if (useConstructor) BeanDeserializer.argumentDefaults(primaryConstructor, constructorParams, fields) else null


  override def deserialize(jsonIterator: StringIterator): T = {
    if (useConstructor) deserializeViaConstructor(jsonIterator) else deserializeIntoAllocatedInstance(jsonIterator)
  }

  private def deserializeIntoAllocatedInstance(jsonIterator: StringIterator): T = {

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
        skipOrFail(jsonIterator)
      }

      jsonIterator.afterValue('}')
    }

    jsonIterator.nextCharOrNullIfLast

    instance
  }

  private def deserializeViaConstructor(jsonIterator: StringIterator): T = {

    jsonIterator.consumeObjectStart()
    val table = fieldTable
    val arguments = argumentDefaults.clone()
    var lateFields: List[(BeanField, AnyRef)] = Nil

    jsonIterator.skipWhitespaceChars()
    while (jsonIterator.currentChar != '}') {

      val field = table.find(jsonIterator)

      if (field != null) {
        jsonIterator.consumeFieldValueSeparator()
        val value = field.readValue(jsonIterator)
        if (field.argIndex >= 0) {
          arguments(field.argIndex) = value
        } else {
          lateFields = (field, value) :: lateFields
        }
      } else {
        skipOrFail(jsonIterator)
      }

      jsonIterator.afterValue('}')
    }

    jsonIterator.nextCharOrNullIfLast

    val instance = try {
      primaryConstructor.newInstance(arguments: _*)
    } catch {
      case e: java.lang.reflect.InvocationTargetException =>
        throw new IllegalArgumentException("Constructor of " + tpe + " failed: " + e.getCause, e.getCause)
    }
    if (lateFields.nonEmpty) {
      lateFields.foreach { case (field, value) => field.field.set(instance, value) }
    }
    initialiseOptions(instance.asInstanceOf[AnyRef])
    instance
  }

  private def skipOrFail(jsonIterator: StringIterator): Unit = {
    val identifier = IdentifierDeserializer.deserialize(jsonIterator)
    jsonIterator.consumeFieldValueSeparator()
    if (ignoreNonExistingFields) {
      skipValue(jsonIterator)
    } else {
      throw new IllegalArgumentException(s"No field [$identifier] in type [$tpe]. Types: ${context.typesStackMessage}")
    }
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

  /**
   * The primary constructor's parameter names when `tpe` is a concrete case class whose parameters are all
   * backed by fields; null otherwise (the allocation path is used then).
   */
  private def caseClassConstructorParams(tpe: Type, fields: Array[FieldWithTypeInfo]): Array[String] = {
    val symbol = tpe.typeSymbol
    if (!symbol.isClass || !symbol.asClass.isCaseClass || symbol.isAbstract) {
      null
    } else {
      val constructor = symbol.asClass.primaryConstructor
      if (!constructor.isMethod) {
        null
      } else {
        val params = constructor.asMethod.paramLists.flatten.map(_.name.toString.trim).toArray
        val fieldNames = fields.map(_.field.getName).toSet
        if (params.forall(fieldNames.contains)) params else null
      }
    }
  }

  /** The Java constructor whose parameter types match the fields of the given names, or null. */
  private def findConstructor[T](clazz: Class[T], tpe: Type, params: Array[String]): Constructor[T] = {
    val fieldTypes = params.map(name => ReflectionUtil.getAllAccessibleFields(tpe).find(_.field.getName == name).get.field.getType)
    clazz.getDeclaredConstructors.find { c =>
      c.getParameterCount == params.length && c.getParameterTypes.zip(fieldTypes).forall { case (p, f) => p.isAssignableFrom(f) || p == f }
    } match {
      case Some(c) =>
        c.setAccessible(true)
        c.asInstanceOf[Constructor[T]]
      case None => null
    }
  }

  private def argumentDefaults(constructor: Constructor[_], params: Array[String], fields: Array[FieldWithTypeInfo]): Array[AnyRef] = {
    val parameterTypes = constructor.getParameterTypes
    params.indices.map { index =>
      val parameterType = parameterTypes(index)
      val field = fields.find(_.field.getName == params(index)).get
      if (parameterType == java.lang.Integer.TYPE) java.lang.Integer.valueOf(0)
      else if (parameterType == java.lang.Long.TYPE) java.lang.Long.valueOf(0L)
      else if (parameterType == java.lang.Boolean.TYPE) java.lang.Boolean.FALSE
      else if (parameterType == java.lang.Double.TYPE) java.lang.Double.valueOf(0.0)
      else if (parameterType == java.lang.Float.TYPE) java.lang.Float.valueOf(0.0f)
      else if (parameterType == java.lang.Short.TYPE) java.lang.Short.valueOf(0.toShort)
      else if (parameterType == java.lang.Byte.TYPE) java.lang.Byte.valueOf(0.toByte)
      else if (parameterType == java.lang.Character.TYPE) java.lang.Character.valueOf('\u0000')
      else if (field.tpe <:< optionType) None
      else null
    }.toArray
  }
}
