package io.mpjsons.impl.serializer

import java.lang.reflect.Field
import java.nio.charset.StandardCharsets

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

  /** Reads `field`; `slot` is the index of the field in `readers` (see FieldReaders). */
  protected var readers: FieldReaders = new FieldReaders.Reflective(Array(field))
  protected var slot: Int = 0

  /** Makes this writer read the field through the given readers (shared by all fields of one bean class). */
  private[serializer] def useReaders(fieldReaders: FieldReaders, index: Int): this.type = {
    readers = fieldReaders
    slot = index
    this
  }

  def write(obj: AnyRef, jsonBuilder: StringBuilder, notFirst: Boolean): Boolean

  /**
   * `write` for UTF-8 output: same checks in the same order, same text. The built-in writers write bytes directly;
   * this default (for other subclasses) encodes what `write` produces.
   */
  def writeUtf8(obj: AnyRef, out: Utf8Output, notFirst: Boolean): Boolean = {
    val builder = new StringBuilder
    val written = write(obj, builder, notFirst)
    out.writeText(builder.toString)
    written
  }

  protected final def appendPrefix(builder: java.lang.StringBuilder, notFirst: Boolean): Unit = {
    builder.append(if (notFirst) prefixWithComma else prefix)
  }

  private val prefixBytes: Array[Byte] = prefix.getBytes(StandardCharsets.UTF_8)
  private val prefixWithCommaBytes: Array[Byte] = prefixWithComma.getBytes(StandardCharsets.UTF_8)

  protected final def writePrefix(out: Utf8Output, notFirst: Boolean): Unit = {
    out.writeBytes(if (notFirst) prefixWithCommaBytes else prefixBytes)
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
      val value = readers.getObject(obj, slot)
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

    private val writer = Utf8Writer.of(serializer)

    override def writeUtf8(obj: AnyRef, out: Utf8Output, notFirst: Boolean): Boolean = {
      val value = readers.getObject(obj, slot)
      if (omitDefault && value == defaultValue) {
        false
      } else if (value == null) {
        if (nullMode == NullWritten) {
          writePrefix(out, notFirst)
          out.writeAscii("null")
          true
        } else if (nullMode == NullOmitted) {
          false
        } else {
          throw new IllegalArgumentException("Null value is not allowed for field " + name + " in type " + typeName)
        }
      } else {
        writePrefix(out, notFirst)
        writer.writeUtf8(value, out)
        true
      }
    }
  }

  /** Compact Option field: Some(v) is written as v, None is omitted; a null reference follows the null policy. */
  final class OptionWriter(name: String, field: Field, nullMode: Int, typeName: String,
                           elementSerializer: JsonTypeSerializer[AnyRef], omitDefault: Boolean, defaultValue: AnyRef) extends BeanFieldWriter(name, field, true) {

    override def write(obj: AnyRef, jsonBuilder: StringBuilder, notFirst: Boolean): Boolean = {
      val value = readers.getObject(obj, slot)
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

    private val elementWriter = Utf8Writer.of(elementSerializer)

    override def writeUtf8(obj: AnyRef, out: Utf8Output, notFirst: Boolean): Boolean = {
      val value = readers.getObject(obj, slot)
      if (omitDefault && value == defaultValue) {
        return false
      }
      value match {
        case Some(element) =>
          if (element == null && nullMode != NullWritten) {
            if (nullMode == NullOmitted) return false
            throw new IllegalArgumentException("Null value is not allowed in Some of field " + name + " in type " + typeName + " (NullPolicy.Fail)")
          }
          writePrefix(out, notFirst)
          if (element == null) out.writeAscii("null") else elementWriter.writeUtf8(element.asInstanceOf[AnyRef], out)
          true
        case None =>
          false
        case null =>
          if (nullMode == NullWritten) {
            writePrefix(out, notFirst)
            out.writeAscii("null")
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
      val value = readers.getInt(obj, slot)
      if (omitDefault && value == defaultValue) {
        return false
      }
      val builder = jsonBuilder.underlying
      appendPrefix(builder, notFirst)
      builder.append(value)
      true
    }

    override def writeUtf8(obj: AnyRef, out: Utf8Output, notFirst: Boolean): Boolean = {
      val value = readers.getInt(obj, slot)
      if (omitDefault && value == defaultValue) {
        return false
      }
      writePrefix(out, notFirst)
      out.writeInt(value)
      true
    }
  }

  final class LongWriter(name: String, field: Field, omitDefault: Boolean, defaultValue: Long) extends BeanFieldWriter(name, field, false) {
    override def write(obj: AnyRef, jsonBuilder: StringBuilder, notFirst: Boolean): Boolean = {
      val value = readers.getLong(obj, slot)
      if (omitDefault && value == defaultValue) {
        return false
      }
      val builder = jsonBuilder.underlying
      appendPrefix(builder, notFirst)
      builder.append(value)
      true
    }

    override def writeUtf8(obj: AnyRef, out: Utf8Output, notFirst: Boolean): Boolean = {
      val value = readers.getLong(obj, slot)
      if (omitDefault && value == defaultValue) {
        return false
      }
      writePrefix(out, notFirst)
      out.writeLong(value)
      true
    }
  }

  final class BooleanWriter(name: String, field: Field, omitDefault: Boolean, defaultValue: Boolean) extends BeanFieldWriter(name, field, false) {
    override def write(obj: AnyRef, jsonBuilder: StringBuilder, notFirst: Boolean): Boolean = {
      val value = readers.getBoolean(obj, slot)
      if (omitDefault && value == defaultValue) {
        return false
      }
      val builder = jsonBuilder.underlying
      appendPrefix(builder, notFirst)
      builder.append(if (value) "true" else "false")
      true
    }

    override def writeUtf8(obj: AnyRef, out: Utf8Output, notFirst: Boolean): Boolean = {
      val value = readers.getBoolean(obj, slot)
      if (omitDefault && value == defaultValue) {
        return false
      }
      writePrefix(out, notFirst)
      out.writeAscii(if (value) "true" else "false")
      true
    }
  }

  final class DoubleWriter(name: String, field: Field, omitDefault: Boolean, defaultValue: Double) extends BeanFieldWriter(name, field, false) {
    override def write(obj: AnyRef, jsonBuilder: StringBuilder, notFirst: Boolean): Boolean = {
      val value = readers.getDouble(obj, slot)
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

    override def writeUtf8(obj: AnyRef, out: Utf8Output, notFirst: Boolean): Boolean = {
      val value = readers.getDouble(obj, slot)
      if (omitDefault && value == defaultValue) {
        return false
      }
      if (!java.lang.Double.isFinite(value)) {
        throw new IllegalArgumentException("Non-finite floating point value " + value + " in field " + name + " cannot be represented in JSON")
      }
      writePrefix(out, notFirst)
      out.writeDouble(value)
      true
    }
  }

  final class FloatWriter(name: String, field: Field, omitDefault: Boolean, defaultValue: Float) extends BeanFieldWriter(name, field, false) {
    override def write(obj: AnyRef, jsonBuilder: StringBuilder, notFirst: Boolean): Boolean = {
      val value = readers.getFloat(obj, slot)
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

    override def writeUtf8(obj: AnyRef, out: Utf8Output, notFirst: Boolean): Boolean = {
      val value = readers.getFloat(obj, slot)
      if (omitDefault && value == defaultValue) {
        return false
      }
      if (!java.lang.Float.isFinite(value)) {
        throw new IllegalArgumentException("Non-finite floating point value " + value + " in field " + name + " cannot be represented in JSON")
      }
      writePrefix(out, notFirst)
      out.writeFloat(value)
      true
    }
  }

  final class ShortWriter(name: String, field: Field, omitDefault: Boolean, defaultValue: Short) extends BeanFieldWriter(name, field, false) {
    override def write(obj: AnyRef, jsonBuilder: StringBuilder, notFirst: Boolean): Boolean = {
      val value = readers.getShort(obj, slot)
      if (omitDefault && value == defaultValue) {
        return false
      }
      val builder = jsonBuilder.underlying
      appendPrefix(builder, notFirst)
      builder.append(value.toInt)
      true
    }

    override def writeUtf8(obj: AnyRef, out: Utf8Output, notFirst: Boolean): Boolean = {
      val value = readers.getShort(obj, slot)
      if (omitDefault && value == defaultValue) {
        return false
      }
      writePrefix(out, notFirst)
      out.writeInt(value.toInt)
      true
    }
  }

  final class ByteWriter(name: String, field: Field, omitDefault: Boolean, defaultValue: Byte) extends BeanFieldWriter(name, field, false) {
    override def write(obj: AnyRef, jsonBuilder: StringBuilder, notFirst: Boolean): Boolean = {
      val value = readers.getByte(obj, slot)
      if (omitDefault && value == defaultValue) {
        return false
      }
      val builder = jsonBuilder.underlying
      appendPrefix(builder, notFirst)
      builder.append(value.toInt)
      true
    }

    override def writeUtf8(obj: AnyRef, out: Utf8Output, notFirst: Boolean): Boolean = {
      val value = readers.getByte(obj, slot)
      if (omitDefault && value == defaultValue) {
        return false
      }
      writePrefix(out, notFirst)
      out.writeInt(value.toInt)
      true
    }
  }
}

class BeanSerializer(serializerFactory: SerializerFactory, private val tpe: Type, private val context: Context, config: MPJsonsConfig) extends JsonTypeSerializer[AnyRef] with Utf8Writer[AnyRef] {

  def this(serializerFactory: SerializerFactory, tpe: Type, context: Context) =
    this(serializerFactory, tpe, context, MPJsonsConfig())

  def this(serializerFactory: SerializerFactory, tpe: Type, context: Context, nullPolicy: NullPolicy) =
    this(serializerFactory, tpe, context, MPJsonsConfig(nullPolicy = nullPolicy))

  /** Backward compatible constructor: `errorOnNulls = false` omits null fields. */
  def this(serializerFactory: SerializerFactory, tpe: Type, context: Context, errorOnNulls: Boolean) =
    this(serializerFactory, tpe, context, MPJsonsConfig(nullPolicy = if (errorOnNulls) NullPolicy.Fail else NullPolicy.Omit))

  private val fields = ReflectionUtil.getAllAccessibleFields(tpe)
  /** Lazy val to prevent StackOverflow while construction recursive type serializer */
  private lazy val fieldsWithSerializers: Array[FieldInfo[AnyRef]] = fields.filterNot(_.skipSerialization).map { field =>
    FieldInfo(field.field, field.jsonName, serializerFactory.getSerializer(field.tpe, context)
      .asInstanceOf[JsonTypeSerializer[AnyRef]], field.nullable, field.tpe)
  }
  private lazy val writers: Array[BeanFieldWriter] = {
    val typeName = tpe.typeSymbol.fullName
    val template = if (config.omitDefaults) BeanSerializer.defaultInstance(tpe, fields, context) else null
    val readers = FieldReaders.forFields(fieldsWithSerializers.map(_.field))
    fieldsWithSerializers.zipWithIndex.map { case (info, index) =>
      BeanFieldWriter(info, config, typeName, serializerFactory, context, template).useReaders(readers, index)
    }
  }


  override def serialize(obj: AnyRef, jsonBuilder: StringBuilder): Unit = {
    val path = BeanSerializer.currentPath.get()
    path.depth += 1
    try {
      if (path.depth > BeanSerializer.CycleCheckDepth) {
        // Deep enough to be a reference cycle: track the beans on the current path by identity.
        if (!path.onPath.add(obj)) {
          throw new IllegalArgumentException("Cyclic reference detected while serializing " + tpe +
            ": an object of this type refers back to itself through its fields")
        }
        try {
          writeFields(obj, jsonBuilder)
        } finally {
          path.onPath.remove(obj)
        }
      } else {
        writeFields(obj, jsonBuilder)
      }
    } finally {
      path.depth -= 1
    }
  }

  private def writeFields(obj: AnyRef, jsonBuilder: StringBuilder): Unit = {

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

  /** `serialize` for UTF-8 output, with the same cycle detection. */
  override def writeUtf8(obj: AnyRef, out: Utf8Output): Unit = {
    val path = BeanSerializer.currentPath.get()
    path.depth += 1
    try {
      if (path.depth > BeanSerializer.CycleCheckDepth) {
        // Deep enough to be a reference cycle: track the beans on the current path by identity.
        if (!path.onPath.add(obj)) {
          throw new IllegalArgumentException("Cyclic reference detected while serializing " + tpe +
            ": an object of this type refers back to itself through its fields")
        }
        try {
          writeFieldsUtf8(obj, out)
        } finally {
          path.onPath.remove(obj)
        }
      } else {
        writeFieldsUtf8(obj, out)
      }
    } finally {
      path.depth -= 1
    }
  }

  private def writeFieldsUtf8(obj: AnyRef, out: Utf8Output): Unit = {

    val fieldWriters = writers

    out.writeByte('{')

    var isNotFirstField = false
    var index = 0
    while (index < fieldWriters.length) {
      if (fieldWriters(index).writeUtf8(obj, out, isNotFirstField)) {
        isNotFirstField = true
      }
      index += 1
    }

    out.writeByte('}')
  }

  override private[mpjsons] def writesUtf8Itself: Boolean = getClass eq classOf[BeanSerializer]

}

object BeanSerializer {

  /**
   * Beans nested deeper than this on one serialize call are tracked by identity, so a reference cycle is reported as
   * an IllegalArgumentException instead of a StackOverflowError. Shallower beans pay only a depth counter.
   */
  private[serializer] final val CycleCheckDepth = 100

  /** The beans being serialized on the current thread, from the root to the innermost one. */
  private[serializer] final class Path {
    var depth: Int = 0
    val onPath: java.util.Set[AnyRef] = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap[AnyRef, java.lang.Boolean]())
  }

  private[serializer] val currentPath: ThreadLocal[Path] = ThreadLocal.withInitial(() => new Path)

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
