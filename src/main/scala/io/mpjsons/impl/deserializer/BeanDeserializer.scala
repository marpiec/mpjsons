package io.mpjsons.impl.deserializer

import io.mpjsons.impl.util.Messages

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
    val writers = fieldWriters
    val entries = fields.flatMap { field =>
      // A skipped field keeps its keys so that stored JSON still reads, but the value is discarded; no deserializer is
      // built, so its type may have no reader (e.g. an unmarked trait).
      def entry(name: String): BeanField =
        if (field.skipSerialization) {
          new BeanField.SkippedField(name, field.field, skipValue)
        } else {
          val argIndex = if (useConstructor) constructorParams.indexOf(field.field.getName) else -1
          val beanField = BeanField(name, field.field, field.tpe, typeName, deserializerFactory.getDeserializer(field.tpe, context), argIndex, field.nullable, config)
          beanField.absentSlot = absentDefaults.indexWhere(_.field == field.field)
          if (writers == null) beanField else beanField.useWriters(writers, fields.indexOf(field))
        }
      val scalaName = field.field.getName
      if (scalaName != field.jsonName && !jsonNames.contains(scalaName)) {
        Array(entry(field.jsonName), entry(scalaName))
      } else {
        Array(entry(field.jsonName))
      }
    }
    new BeanFieldTable(entries)
  }
  /**
   * Writers of all fields, in `fields` order (see FieldWriters); only when values are written into an allocated
   * instance, the constructor path passes them as arguments.
   */
  private lazy val fieldWriters: FieldWriters = if (useConstructor) null else FieldWriters.forFields(fields.map(_.field))
  /**
   * Fields that get a value when no key was read for them and construction left them null: None for Option fields
   * (an absent key reads as None), the empty value for `@omitEmpty` fields.
   */
  private lazy val absentDefaults: Array[BeanDeserializer.AbsentDefault] =
    fields.filter(field => field.tpe <:< BeanDeserializer.optionType || field.omitEmpty).map { field =>
      val writerSlot = fields.indexOf(field)
      val argIndex = if (useConstructor) constructorParams.indexOf(field.field.getName) else -1
      if (field.tpe <:< BeanDeserializer.optionType) {
        new BeanDeserializer.AbsentDefault(field.field, writerSlot, argIndex, None, null)
      } else {
        emptyDefault(field, writerSlot, argIndex)
      }
    }
  /** Constructor arguments for absent keys: zero / false for primitives, None for Options, empty for `@omitEmpty`. */
  private lazy val argumentDefaults: Array[AnyRef] = if (useConstructor) {
    val arguments = BeanDeserializer.argumentDefaults(primaryConstructor)
    for (default <- absentDefaults if default.argIndex >= 0) {
      arguments(default.argIndex) = default.value()
    }
    arguments
  } else {
    null
  }
  /** Constructor arguments whose absent value may be mutable: each object gets its own. */
  private lazy val freshArguments: Array[BeanDeserializer.AbsentDefault] = absentDefaults.filter(d => d.argIndex >= 0 && !d.shared)

  /**
   * An absent `@omitEmpty` key reads as `[]` would (`""` for a String; `{}` where only that is understood), through
   * the field's own deserializer, so converters and transforms registered for its type apply. An immutable result is
   * shared; anything else is read again for each object.
   */
  private def emptyDefault(field: FieldWithTypeInfo, writerSlot: Int, argIndex: Int): BeanDeserializer.AbsentDefault = {
    val deserializer = deserializerFactory.getDeserializer[AnyRef](field.tpe, context)
    val candidates = if (field.tpe <:< typeOf[String]) List("\"\"") else List("[]", "{}")
    def read(json: String): AnyRef = {
      val jsonIterator = new StringIterator(json)
      val value = deserializer.deserialize(jsonIterator)
      jsonIterator.expectEnd()
      value
    }
    val (json, empty) = candidates.iterator.map(json => json -> scala.util.Try(read(json))).collectFirst {
      case (json, scala.util.Success(value)) => (json, value)
    }.getOrElse(throw new IllegalArgumentException("@omitEmpty on field " + field.field.getName + " of " + tpe +
      ": its deserializer reads none of " + candidates.mkString(", ") + ", so an absent key cannot be read as empty"))
    if (empty == null || empty.isInstanceOf[String] || empty.getClass.isArray || empty.isInstanceOf[scala.collection.immutable.Iterable[_]]) {
      new BeanDeserializer.AbsentDefault(field.field, writerSlot, argIndex, empty, null)
    } else {
      new BeanDeserializer.AbsentDefault(field.field, writerSlot, argIndex, null, () => read(json))
    }
  }


  override def deserialize(jsonIterator: StringIterator): T = {
    if (useConstructor) deserializeViaConstructor(jsonIterator) else deserializeIntoAllocatedInstance(jsonIterator)
  }

  private def deserializeIntoAllocatedInstance(jsonIterator: StringIterator): T = {

    jsonIterator.consumeObjectStart()
    val instance = ObjectConstructionUtil.createInstance[T](clazz, constructor, context)
    val table = fieldTable
    // Fields with a value for absent keys (Option, @omitEmpty) still unset after the loop get it at the end (see
    // initialiseAbsentFields); with more than 64 of them, all are initialised up front instead.
    val trackAbsent = absentDefaults.length <= 64
    if (!trackAbsent) {
      initialiseNullFields(instance.asInstanceOf[AnyRef])
    }
    // Per-call state lives in locals only: the deserializer is shared between threads.
    var assigned = 0L

    var previous: BeanField = null // the last field read, which predicts the next key (see BeanFieldTable)

    jsonIterator.skipWhitespaceChars()
    while (jsonIterator.currentChar != '}') {

      var field = table.matchExpected(jsonIterator, if (previous == null) table.first else previous.next)
      if (field == null) {
        field = table.find(jsonIterator)
        if (field != null) {
          table.learn(previous, field)
        }
      }

      if (field != null) {
        previous = field
        jsonIterator.consumeFieldValueSeparator()
        field.set(instance.asInstanceOf[AnyRef], jsonIterator)
        if (field.absentSlot >= 0) {
          assigned |= 1L << field.absentSlot
        }
      } else {
        skipOrFail(jsonIterator)
      }

      jsonIterator.afterValue('}')
    }

    jsonIterator.nextCharOrNullIfLast

    if (trackAbsent) {
      initialiseAbsentFields(instance.asInstanceOf[AnyRef], assigned)
    }

    instance
  }

  private def deserializeViaConstructor(jsonIterator: StringIterator): T = {

    jsonIterator.consumeObjectStart()
    val table = fieldTable
    val arguments = argumentDefaults.clone()
    var index = 0
    while (index < freshArguments.length) {
      val default = freshArguments(index)
      arguments(default.argIndex) = default.value()
      index += 1
    }
    var lateFields: List[(BeanField, AnyRef)] = Nil

    var previous: BeanField = null // the last field read, which predicts the next key (see BeanFieldTable)

    jsonIterator.skipWhitespaceChars()
    while (jsonIterator.currentChar != '}') {

      var field = table.matchExpected(jsonIterator, if (previous == null) table.first else previous.next)
      if (field == null) {
        field = table.find(jsonIterator)
        if (field != null) {
          table.learn(previous, field)
        }
      }

      if (field != null) {
        previous = field
        jsonIterator.consumeFieldValueSeparator()
        val value = field.readValue(jsonIterator)
        if (field.argIndex >= 0) {
          arguments(field.argIndex) = value
        } else if (!field.skipped) {
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
    initialiseAfterConstruction(instance.asInstanceOf[AnyRef], lateFields)
    instance
  }

  private def skipOrFail(jsonIterator: StringIterator): Unit = {
    if (ignoreNonExistingFields) {
      IdentifierDeserializer.skip(jsonIterator)
      jsonIterator.consumeFieldValueSeparator()
      skipValue(jsonIterator)
    } else {
      val identifier = IdentifierDeserializer.deserialize(jsonIterator)
      jsonIterator.consumeFieldValueSeparator()
      throw new IllegalArgumentException(s"No field [${Messages.abbreviate(identifier)}] in type [$tpe]. Types: ${context.typesStackMessage}")
    }
  }

  /**
   * Same result as `initialiseNullFields` before the key loop: a field of `absentDefaults` that no key was written to
   * gets its value when construction left it null. An instance allocated without a constructor has only null fields,
   * so it is not read, and the value is written through the generated writers like any other.
   */
  private def initialiseAbsentFields(instance: AnyRef, assigned: Long): Unit = {
    val defaults = absentDefaults
    var index = 0
    while (index < defaults.length) {
      if ((assigned & (1L << index)) == 0) {
        val default = defaults(index)
        if (constructor == null) {
          fieldWriters.setObject(instance, default.writerSlot, default.value())
        } else if (default.field.get(instance) == null) {
          default.field.set(instance, default.value())
        }
      }
      index += 1
    }
  }

  private def initialiseNullFields(instance: AnyRef): Unit = {
    for (default <- absentDefaults if default.field.get(instance) == null) {
      default.field.set(instance, default.value())
    }
  }

  /**
   * Constructor mode: constructor arguments already hold the values for absent keys. Option fields still null get
   * None (as they always did), and `@omitEmpty` fields that are not constructor parameters get their empty value when
   * no key was read for them and the class left them null.
   */
  private def initialiseAfterConstruction(instance: AnyRef, lateFields: List[(BeanField, AnyRef)]): Unit = {
    for (default <- absentDefaults) {
      val absent = default.isOption || (default.argIndex < 0 && !lateFields.exists(_._1.field == default.field))
      if (absent && default.field.get(instance) == null) {
        default.field.set(instance, default.value())
      }
    }
  }


  private def skipValue(jsonIterator: StringIterator): Unit = {
    jsonIterator.skipWhitespaceChars()
    jsonIterator.currentChar match {
      case '{' => if (config.maxDepth > 0) DepthLimit.nested(jsonIterator, config.maxDepth)(skipObject(jsonIterator)) else skipObject(jsonIterator)
      case '[' => if (config.maxDepth > 0) DepthLimit.nested(jsonIterator, config.maxDepth)(skipArray(jsonIterator)) else skipArray(jsonIterator)
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
      IdentifierDeserializer.skip(jsonIterator)
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
    AbstractStringDeserializer.skipString(jsonIterator)
  }

  def skipNumber(jsonIterator: StringIterator): Unit = {
    AbstractFloatingPointDeserializer.skipNumber(jsonIterator)
  }

  def skipBooleanOrNull(jsonIterator: StringIterator): Unit = {
    jsonIterator.skipWhitespaceChars()

    val input = jsonIterator.stringValue
    val length = jsonIterator.stringLength
    val start = jsonIterator.nextIndex - 1
    var end = start
    while (end < length && { val c = input.charAt(end); c >= 'a' && c <= 'z' }) {
      end += 1
    }
    jsonIterator.seek(end)

    if (!jsonIterator.rangeEquals(start, end, "true") && !jsonIterator.rangeEquals(start, end, "false") && !jsonIterator.rangeEquals(start, end, "null")) {
      throw new IllegalArgumentException(s"Invalid boolean value: ${Messages.abbreviate(input.substring(start, end))}")
    }
  }

}

object BeanDeserializer {
  private val optionType = typeOf[Option[_]]

  /**
   * The value a field gets when its key is absent: `sharedValue`, or a new one from `newValue` for each object.
   * `writerSlot` is the field's index in the bean's FieldWriters; `argIndex` its constructor position, or -1.
   */
  private final class AbsentDefault(val field: Field, val writerSlot: Int, val argIndex: Int, sharedValue: AnyRef, newValue: () => AnyRef) {
    def shared: Boolean = newValue == null
    def isOption: Boolean = sharedValue eq None
    def value(): AnyRef = if (newValue == null) sharedValue else newValue()
  }

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

  /** Zero / false for primitive parameters, null otherwise; Options and `@omitEmpty` fields are set from absentDefaults. */
  private def argumentDefaults(constructor: Constructor[_]): Array[AnyRef] = {
    constructor.getParameterTypes.map { parameterType =>
      if (parameterType == java.lang.Integer.TYPE) java.lang.Integer.valueOf(0)
      else if (parameterType == java.lang.Long.TYPE) java.lang.Long.valueOf(0L)
      else if (parameterType == java.lang.Boolean.TYPE) java.lang.Boolean.FALSE
      else if (parameterType == java.lang.Double.TYPE) java.lang.Double.valueOf(0.0)
      else if (parameterType == java.lang.Float.TYPE) java.lang.Float.valueOf(0.0f)
      else if (parameterType == java.lang.Short.TYPE) java.lang.Short.valueOf(0.toShort)
      else if (parameterType == java.lang.Byte.TYPE) java.lang.Byte.valueOf(0.toByte)
      else if (parameterType == java.lang.Character.TYPE) java.lang.Character.valueOf('\u0000')
      else null
    }
  }
}
