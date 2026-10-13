package io.mpjsons.impl.serializer

import java.lang.reflect.Field

import io.mpjsons.{JsonTypeSerializer, MPJsonsConfig, NullPolicy}
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.{Context, ObjectConstructionUtil, TypesUtil}
import io.mpjsons.impl.util.reflection.ReflectionUtil

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

case class FieldInfo[T](field: Field, name: String, jsonTypeSerializer: JsonTypeSerializer[T], nullable: Boolean, tpe: Type = NoType)

/**
 * Writes one bean field. `write` appends `"name":value` (with the leading comma when `notFirst`) and returns
 * true, or appends nothing and returns false when the field is omitted (null value under Omit, a nullable
 * field under Fail, None under compactOptions, or a value equal to the type's default under omitDefaults).
 * Primitive fields are read without boxing when the built-in serializer is in use.
 *
 * `template` is the instance reading `{}` would produce (null when omitDefaults is off): writers compare the
 * field's value against it and skip equal values.
 */
abstract class BeanFieldWriter(val name: String, val field: Field, val nullable: Boolean) {

  protected val prefix: String = "\"" + name + "\":"
  protected val prefixWithComma: String = "," + prefix

  def write(obj: AnyRef, jsonBuilder: StringBuilder, notFirst: Boolean): Boolean

  protected final def appendPrefix(builder: java.lang.StringBuilder, notFirst: Boolean): Unit = {
    builder.append(if (notFirst) prefixWithComma else prefix)
  }
}

object BeanFieldWriter {

  private final val NullFails = 0
  private final val NullOmitted = 1
  private final val NullWritten = 2

  private val optionType = typeOf[Option[_]]

  def apply(info: FieldInfo[AnyRef], config: MPJsonsConfig, typeName: String,
            serializerFactory: SerializerFactory, context: Context, template: AnyRef): BeanFieldWriter = {
    val field = info.field
    val fieldType = field.getType
    val serializer = info.jsonTypeSerializer
    val hasTemplate = template != null
    if (fieldType == java.lang.Integer.TYPE && (serializer eq IntSerializer)) {
      new IntWriter(info.name, field, hasTemplate, if (hasTemplate) field.getInt(template) else 0)
    } else if (fieldType == java.lang.Long.TYPE && (serializer eq LongSerializer)) {
      new LongWriter(info.name, field, hasTemplate, if (hasTemplate) field.getLong(template) else 0L)
    } else if (fieldType == java.lang.Boolean.TYPE && (serializer eq BooleanSerializer)) {
      new BooleanWriter(info.name, field, hasTemplate, if (hasTemplate) field.getBoolean(template) else false)
    } else if (fieldType == java.lang.Double.TYPE && (serializer eq DoubleSerializer)) {
      new DoubleWriter(info.name, field, hasTemplate, if (hasTemplate) field.getDouble(template) else 0.0)
    } else if (fieldType == java.lang.Float.TYPE && (serializer eq FloatSerializer)) {
      new FloatWriter(info.name, field, hasTemplate, if (hasTemplate) field.getFloat(template) else 0.0f)
    } else if (fieldType == java.lang.Short.TYPE && (serializer eq ShortSerializer)) {
      new ShortWriter(info.name, field, hasTemplate, if (hasTemplate) field.getShort(template) else 0.toShort)
    } else if (fieldType == java.lang.Byte.TYPE && (serializer eq ByteSerializer)) {
      new ByteWriter(info.name, field, hasTemplate, if (hasTemplate) field.getByte(template) else 0.toByte)
    } else {
      val nullMode = config.nullPolicy match {
        case NullPolicy.WriteNull => NullWritten
        case NullPolicy.Omit => NullOmitted
        case NullPolicy.Fail => if (info.nullable) NullOmitted else NullFails
      }
      val defaultValue = if (hasTemplate) field.get(template) else null
      if (config.compactOptions && info.tpe != NoType && info.tpe <:< optionType) {
        val elementType = TypesUtil.getSubElementsType(info.tpe)
        new OptionWriter(info.name, field, nullMode, typeName, OptionSerializer.elementSerializer(serializerFactory, elementType, context, compact = true),
          hasTemplate, defaultValue)
      } else {
        new ObjectWriter(info.name, field, info.nullable, serializer, nullMode, typeName, hasTemplate, defaultValue)
      }
    }
  }

  final class ObjectWriter(name: String, field: Field, nullable: Boolean, serializer: JsonTypeSerializer[AnyRef],
                           nullMode: Int, typeName: String, omitDefault: Boolean, defaultValue: AnyRef) extends BeanFieldWriter(name, field, nullable) {

    override def write(obj: AnyRef, jsonBuilder: StringBuilder, notFirst: Boolean): Boolean = {
      val value = field.get(obj)
      if (omitDefault && value == defaultValue) {
        false
      } else if (value == null) {
        if (nullMode == NullWritten) {
          val builder = jsonBuilder.underlying
          appendPrefix(builder, notFirst)
          builder.append("null")
          true
        } else if (nullMode == NullOmitted) {
          false
        } else {
          throw new IllegalArgumentException("Null value is not allowed for field " + name + " in type " + typeName)
        }
      } else {
        appendPrefix(jsonBuilder.underlying, notFirst)
        serializer.serialize(value, jsonBuilder)
        true
      }
    }
  }

  /** Compact Option field: Some(v) is written as v, None is omitted; a null reference follows the null policy. */
  final class OptionWriter(name: String, field: Field, nullMode: Int, typeName: String,
                           elementSerializer: JsonTypeSerializer[AnyRef], omitDefault: Boolean, defaultValue: AnyRef) extends BeanFieldWriter(name, field, true) {

    override def write(obj: AnyRef, jsonBuilder: StringBuilder, notFirst: Boolean): Boolean = {
      val value = field.get(obj)
      if (omitDefault && value == defaultValue) {
        return false
      }
      value match {
        case Some(element) =>
          if (element == null && nullMode != NullWritten) {
            if (nullMode == NullOmitted) return false
            throw new IllegalArgumentException("Null value is not allowed in Some of field " + name + " in type " + typeName + " (NullPolicy.Fail)")
          }
          val builder = jsonBuilder.underlying
          appendPrefix(builder, notFirst)
          if (element == null) builder.append("null") else elementSerializer.serialize(element.asInstanceOf[AnyRef], jsonBuilder)
          true
        case None =>
          false
        case null =>
          if (nullMode == NullWritten) {
            val builder = jsonBuilder.underlying
            appendPrefix(builder, notFirst)
            builder.append("null")
            true
          } else if (nullMode == NullOmitted) {
            false
          } else {
            throw new IllegalArgumentException("Null value is not allowed for field " + name + " in type " + typeName)
          }
      }
    }
  }

  final class IntWriter(name: String, field: Field, omitDefault: Boolean, defaultValue: Int) extends BeanFieldWriter(name, field, false) {
    override def write(obj: AnyRef, jsonBuilder: StringBuilder, notFirst: Boolean): Boolean = {
      val value = field.getInt(obj)
      if (omitDefault && value == defaultValue) {
        return false
      }
      val builder = jsonBuilder.underlying
      appendPrefix(builder, notFirst)
      builder.append(value)
      true
    }
  }

  final class LongWriter(name: String, field: Field, omitDefault: Boolean, defaultValue: Long) extends BeanFieldWriter(name, field, false) {
    override def write(obj: AnyRef, jsonBuilder: StringBuilder, notFirst: Boolean): Boolean = {
      val value = field.getLong(obj)
      if (omitDefault && value == defaultValue) {
        return false
      }
      val builder = jsonBuilder.underlying
      appendPrefix(builder, notFirst)
      builder.append(value)
      true
    }
  }

  final class BooleanWriter(name: String, field: Field, omitDefault: Boolean, defaultValue: Boolean) extends BeanFieldWriter(name, field, false) {
    override def write(obj: AnyRef, jsonBuilder: StringBuilder, notFirst: Boolean): Boolean = {
      val value = field.getBoolean(obj)
      if (omitDefault && value == defaultValue) {
        return false
      }
      val builder = jsonBuilder.underlying
      appendPrefix(builder, notFirst)
      builder.append(if (value) "true" else "false")
      true
    }
  }

  final class DoubleWriter(name: String, field: Field, omitDefault: Boolean, defaultValue: Double) extends BeanFieldWriter(name, field, false) {
    override def write(obj: AnyRef, jsonBuilder: StringBuilder, notFirst: Boolean): Boolean = {
      val value = field.getDouble(obj)
      if (omitDefault && value == defaultValue) {
        return false
      }
      if (!java.lang.Double.isFinite(value)) {
        throw new IllegalArgumentException("Non-finite floating point value " + value + " in field " + name + " cannot be represented in JSON")
      }
      val builder = jsonBuilder.underlying
      appendPrefix(builder, notFirst)
      builder.append(value)
      true
    }
  }

  final class FloatWriter(name: String, field: Field, omitDefault: Boolean, defaultValue: Float) extends BeanFieldWriter(name, field, false) {
    override def write(obj: AnyRef, jsonBuilder: StringBuilder, notFirst: Boolean): Boolean = {
      val value = field.getFloat(obj)
      if (omitDefault && value == defaultValue) {
        return false
      }
      if (!java.lang.Float.isFinite(value)) {
        throw new IllegalArgumentException("Non-finite floating point value " + value + " in field " + name + " cannot be represented in JSON")
      }
      val builder = jsonBuilder.underlying
      appendPrefix(builder, notFirst)
      builder.append(value)
      true
    }
  }

  final class ShortWriter(name: String, field: Field, omitDefault: Boolean, defaultValue: Short) extends BeanFieldWriter(name, field, false) {
    override def write(obj: AnyRef, jsonBuilder: StringBuilder, notFirst: Boolean): Boolean = {
      val value = field.getShort(obj)
      if (omitDefault && value == defaultValue) {
        return false
      }
      val builder = jsonBuilder.underlying
      appendPrefix(builder, notFirst)
      builder.append(value.toInt)
      true
    }
  }

  final class ByteWriter(name: String, field: Field, omitDefault: Boolean, defaultValue: Byte) extends BeanFieldWriter(name, field, false) {
    override def write(obj: AnyRef, jsonBuilder: StringBuilder, notFirst: Boolean): Boolean = {
      val value = field.getByte(obj)
      if (omitDefault && value == defaultValue) {
        return false
      }
      val builder = jsonBuilder.underlying
      appendPrefix(builder, notFirst)
      builder.append(value.toInt)
      true
    }
  }
}

class BeanSerializer(serializerFactory: SerializerFactory, private val tpe: Type, private val context: Context, config: MPJsonsConfig) extends JsonTypeSerializer[AnyRef] {

  def this(serializerFactory: SerializerFactory, tpe: Type, context: Context) =
    this(serializerFactory, tpe, context, MPJsonsConfig())

  def this(serializerFactory: SerializerFactory, tpe: Type, context: Context, nullPolicy: NullPolicy) =
    this(serializerFactory, tpe, context, MPJsonsConfig(nullPolicy = nullPolicy))

  /** Backward compatible constructor: `errorOnNulls = false` omits null fields. */
  def this(serializerFactory: SerializerFactory, tpe: Type, context: Context, errorOnNulls: Boolean) =
    this(serializerFactory, tpe, context, MPJsonsConfig(nullPolicy = if (errorOnNulls) NullPolicy.Fail else NullPolicy.Omit))

  private val fields = ReflectionUtil.getAllAccessibleFields(tpe)
  /** Lazy val to prevent StackOverflow while construction recursive type serializer */
  private lazy val fieldsWithSerializers: Array[FieldInfo[AnyRef]] = fields.map { field =>
    FieldInfo(field.field, field.jsonName, serializerFactory.getSerializer(field.tpe, context)
      .asInstanceOf[JsonTypeSerializer[AnyRef]], field.nullable, field.tpe)
  }
  private lazy val writers: Array[BeanFieldWriter] = {
    val typeName = tpe.typeSymbol.fullName
    val template = if (config.omitDefaults) BeanSerializer.defaultInstance(tpe, fields, context) else null
    fieldsWithSerializers.map(info => BeanFieldWriter(info, config, typeName, serializerFactory, context, template))
  }


  override def serialize(obj: AnyRef, jsonBuilder: StringBuilder): Unit = {

    val fieldWriters = writers

    jsonBuilder.append('{')

    var isNotFirstField = false
    var index = 0
    while (index < fieldWriters.length) {
      if (fieldWriters(index).write(obj, jsonBuilder, isNotFirstField)) {
        isNotFirstField = true
      }
      index += 1
    }

    jsonBuilder.append('}')
  }

}

object BeanSerializer {

  private val optionType = typeOf[Option[_]]

  /**
   * The instance the deserializer produces for `{}`: built the same way (no-arg constructor or allocation without
   * a constructor) with null Option fields set to None. Null when the type cannot be instantiated.
   */
  private[serializer] def defaultInstance(tpe: Type, fields: Array[io.mpjsons.impl.util.reflection.FieldWithTypeInfo], context: Context): AnyRef = {
    try {
      val clazz = TypesUtil.getClassFromType[AnyRef](tpe)
      val constructor = try clazz.getConstructor() catch { case _: NoSuchMethodException => null }
      val instance = ObjectConstructionUtil.createInstance[AnyRef](clazz, constructor, context)
      for (field <- fields if field.tpe <:< optionType && field.field.get(instance) == null) {
        field.field.set(instance, None)
      }
      instance
    } catch {
      case _: Exception => null
    }
  }
}
