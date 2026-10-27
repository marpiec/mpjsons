package io.mpjsons.impl.deserializer

import io.mpjsons.{JsonTypeDeserializer, MPJsonsConfig}
import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.values._
import io.mpjsons.impl.util.{NullValue, NullValues}

import java.lang.reflect.Field
import scala.reflect.runtime.universe._

/**
 * One bean field: its JSON key, the reflective field and how to read its value into an instance.
 * Primitive fields read and store the value without boxing when the built-in deserializer is in use.
 * A JSON null stores None for Option fields and null for fields declared nullable (`@nullable`, or any
 * reference field when the NullPolicy is not Fail); otherwise it fails naming the field.
 *
 * `argIndex` is the position of the field in the primary constructor's parameter list when the bean is built
 * through its constructor (see BeanDeserializer), or -1 when the field is written after construction.
 */
abstract class BeanField(val name: String, val field: Field, nullValue: NullValue, val argIndex: Int) {

  /** Reads the value at the cursor and stores it into `instance`. */
  def set(instance: AnyRef, jsonIterator: StringIterator): Unit

  /** Reads the value at the cursor as a (boxed) object, for constructor arguments. */
  def readValue(jsonIterator: StringIterator): AnyRef

  /** True when the value is consumed and discarded (`@skipSerialization`). */
  def skipped: Boolean = false

  protected final def setNull(instance: AnyRef): Unit = field.set(instance, nullValue.get())

  /** Writes values into `field`; `slot` is the index of the field in `writers` (see FieldWriters). */
  protected var writers: FieldWriters = new FieldWriters.Reflective(Array(field))
  protected var slot: Int = 0

  /**
   * The field whose key followed this one in an object read earlier, or null (see BeanFieldTable). Only a hint: it is
   * written without synchronisation by any thread that reads another order, and a stale value costs one comparison.
   */
  private[deserializer] var next: BeanField = null

  /**
   * Index of this field among the bean's fields that get a value when their key is absent (Option and `@omitEmpty`
   * fields, see BeanDeserializer), or -1. Set before the owning table is published.
   */
  private[deserializer] var absentSlot: Int = -1

  /** Makes this field write through the given writers (shared by all fields of one bean class). */
  private[deserializer] def useWriters(fieldWriters: FieldWriters, index: Int): this.type = {
    writers = fieldWriters
    slot = index
    this
  }

  protected final def nullArgument(): AnyRef = nullValue.get()
}

object BeanField {

  def apply(name: String, field: Field, fieldType: Type, ownerTypeName: String, deserializer: JsonTypeDeserializer[_],
            argIndex: Int, declaredNullable: Boolean, config: MPJsonsConfig): BeanField = {
    val javaType = field.getType
    val nullValue = NullValues.forField(fieldType, name, ownerTypeName, declaredNullable, config)
    if (javaType == java.lang.Integer.TYPE && (deserializer eq IntDeserializer)) {
      new IntField(name, field, nullValue, argIndex)
    } else if (javaType == java.lang.Long.TYPE && (deserializer eq LongDeserializer)) {
      new LongField(name, field, nullValue, argIndex)
    } else if (javaType == java.lang.Boolean.TYPE && (deserializer eq BooleanDeserializer)) {
      new BooleanField(name, field, nullValue, argIndex)
    } else if (javaType == java.lang.Double.TYPE && (deserializer eq DoubleDeserializer)) {
      new DoubleField(name, field, nullValue, argIndex)
    } else if (javaType == java.lang.Float.TYPE && (deserializer eq FloatDeserializer)) {
      new FloatField(name, field, nullValue, argIndex)
    } else if (javaType == java.lang.Short.TYPE && (deserializer eq ShortDeserializer)) {
      new ShortField(name, field, nullValue, argIndex)
    } else if (javaType == java.lang.Byte.TYPE && (deserializer eq ByteDeserializer)) {
      new ByteField(name, field, nullValue, argIndex)
    } else if (javaType == java.lang.Character.TYPE && (deserializer eq CharDeserializer)) {
      new CharField(name, field, nullValue, argIndex)
    } else {
      new ObjectField(name, field, nullValue, deserializer.asInstanceOf[JsonTypeDeserializer[AnyRef]], argIndex)
    }
  }

  /** Consumes the value without touching the instance; `readValue` returns null and must not be stored. */
  final class SkippedField(name: String, field: Field, skipValue: StringIterator => Unit) extends BeanField(name, field, null, -1) {
    override def skipped: Boolean = true
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = skipValue(jsonIterator)
    override def readValue(jsonIterator: StringIterator): AnyRef = {
      skipValue(jsonIterator)
      null
    }
  }

  final class ObjectField(name: String, field: Field, nullValue: NullValue, deserializer: JsonTypeDeserializer[AnyRef], argIndex: Int) extends BeanField(name, field, nullValue, argIndex) {
    private val primitive = field.getType.isPrimitive
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else if (primitive) field.set(instance, deserializer.deserialize(jsonIterator)) else writers.setObject(instance, slot, deserializer.deserialize(jsonIterator))
    }
    override def readValue(jsonIterator: StringIterator): AnyRef = {
      if (jsonIterator.tryConsumeNull()) nullArgument() else deserializer.deserialize(jsonIterator)
    }
  }

  final class IntField(name: String, field: Field, nullValue: NullValue, argIndex: Int) extends BeanField(name, field, nullValue, argIndex) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else writers.setInt(instance, slot, IntDeserializer.deserialize(jsonIterator))
    }
    override def readValue(jsonIterator: StringIterator): AnyRef = {
      if (jsonIterator.tryConsumeNull()) nullArgument() else java.lang.Integer.valueOf(IntDeserializer.deserialize(jsonIterator))
    }
  }

  final class LongField(name: String, field: Field, nullValue: NullValue, argIndex: Int) extends BeanField(name, field, nullValue, argIndex) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else writers.setLong(instance, slot, LongDeserializer.deserialize(jsonIterator))
    }
    override def readValue(jsonIterator: StringIterator): AnyRef = {
      if (jsonIterator.tryConsumeNull()) nullArgument() else java.lang.Long.valueOf(LongDeserializer.deserialize(jsonIterator))
    }
  }

  final class BooleanField(name: String, field: Field, nullValue: NullValue, argIndex: Int) extends BeanField(name, field, nullValue, argIndex) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else writers.setBoolean(instance, slot, BooleanDeserializer.deserialize(jsonIterator))
    }
    override def readValue(jsonIterator: StringIterator): AnyRef = {
      if (jsonIterator.tryConsumeNull()) nullArgument() else java.lang.Boolean.valueOf(BooleanDeserializer.deserialize(jsonIterator))
    }
  }

  final class DoubleField(name: String, field: Field, nullValue: NullValue, argIndex: Int) extends BeanField(name, field, nullValue, argIndex) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else writers.setDouble(instance, slot, DoubleDeserializer.deserialize(jsonIterator))
    }
    override def readValue(jsonIterator: StringIterator): AnyRef = {
      if (jsonIterator.tryConsumeNull()) nullArgument() else java.lang.Double.valueOf(DoubleDeserializer.deserialize(jsonIterator))
    }
  }

  final class FloatField(name: String, field: Field, nullValue: NullValue, argIndex: Int) extends BeanField(name, field, nullValue, argIndex) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else writers.setFloat(instance, slot, FloatDeserializer.deserialize(jsonIterator))
    }
    override def readValue(jsonIterator: StringIterator): AnyRef = {
      if (jsonIterator.tryConsumeNull()) nullArgument() else java.lang.Float.valueOf(FloatDeserializer.deserialize(jsonIterator))
    }
  }

  final class ShortField(name: String, field: Field, nullValue: NullValue, argIndex: Int) extends BeanField(name, field, nullValue, argIndex) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else writers.setShort(instance, slot, ShortDeserializer.deserialize(jsonIterator))
    }
    override def readValue(jsonIterator: StringIterator): AnyRef = {
      if (jsonIterator.tryConsumeNull()) nullArgument() else java.lang.Short.valueOf(ShortDeserializer.deserialize(jsonIterator))
    }
  }

  final class ByteField(name: String, field: Field, nullValue: NullValue, argIndex: Int) extends BeanField(name, field, nullValue, argIndex) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else writers.setByte(instance, slot, ByteDeserializer.deserialize(jsonIterator))
    }
    override def readValue(jsonIterator: StringIterator): AnyRef = {
      if (jsonIterator.tryConsumeNull()) nullArgument() else java.lang.Byte.valueOf(ByteDeserializer.deserialize(jsonIterator))
    }
  }

  final class CharField(name: String, field: Field, nullValue: NullValue, argIndex: Int) extends BeanField(name, field, nullValue, argIndex) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else writers.setChar(instance, slot, CharDeserializer.deserialize(jsonIterator))
    }
    override def readValue(jsonIterator: StringIterator): AnyRef = {
      if (jsonIterator.tryConsumeNull()) nullArgument() else java.lang.Character.valueOf(CharDeserializer.deserialize(jsonIterator))
    }
  }
}
