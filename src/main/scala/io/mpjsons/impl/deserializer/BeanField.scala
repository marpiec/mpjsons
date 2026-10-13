package io.mpjsons.impl.deserializer

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.values._
import io.mpjsons.impl.util.{NullValue, NullValues}

import java.lang.reflect.Field
import scala.reflect.runtime.universe._

/**
 * One bean field: its JSON key, the reflective field and how to read its value into an instance.
 * Primitive fields read and store the value without boxing when the built-in deserializer is in use.
 * A JSON null stores the type's null value (None for Option, null for references) or fails for primitives.
 *
 * `argIndex` is the position of the field in the primary constructor's parameter list when the bean is built
 * through its constructor (see BeanDeserializer), or -1 when the field is written after construction.
 */
abstract class BeanField(val name: String, val field: Field, nullValue: NullValue, val argIndex: Int) {
  val hash: Int = name.hashCode

  /** Reads the value at the cursor and stores it into `instance`. */
  def set(instance: AnyRef, jsonIterator: StringIterator): Unit

  /** Reads the value at the cursor as a (boxed) object, for constructor arguments. */
  def readValue(jsonIterator: StringIterator): AnyRef

  protected final def setNull(instance: AnyRef): Unit = field.set(instance, nullValue.get())

  protected final def nullArgument(): AnyRef = nullValue.get()
}

object BeanField {

  def apply(name: String, field: Field, fieldType: Type, ownerTypeName: String, deserializer: JsonTypeDeserializer[_], argIndex: Int = -1): BeanField = {
    val javaType = field.getType
    val nullValue = NullValues.forField(fieldType, name, ownerTypeName)
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

  final class ObjectField(name: String, field: Field, nullValue: NullValue, deserializer: JsonTypeDeserializer[AnyRef], argIndex: Int) extends BeanField(name, field, nullValue, argIndex) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else field.set(instance, deserializer.deserialize(jsonIterator))
    }
    override def readValue(jsonIterator: StringIterator): AnyRef = {
      if (jsonIterator.tryConsumeNull()) nullArgument() else deserializer.deserialize(jsonIterator)
    }
  }

  final class IntField(name: String, field: Field, nullValue: NullValue, argIndex: Int) extends BeanField(name, field, nullValue, argIndex) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else field.setInt(instance, IntDeserializer.deserialize(jsonIterator))
    }
    override def readValue(jsonIterator: StringIterator): AnyRef = {
      if (jsonIterator.tryConsumeNull()) nullArgument() else java.lang.Integer.valueOf(IntDeserializer.deserialize(jsonIterator))
    }
  }

  final class LongField(name: String, field: Field, nullValue: NullValue, argIndex: Int) extends BeanField(name, field, nullValue, argIndex) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else field.setLong(instance, LongDeserializer.deserialize(jsonIterator))
    }
    override def readValue(jsonIterator: StringIterator): AnyRef = {
      if (jsonIterator.tryConsumeNull()) nullArgument() else java.lang.Long.valueOf(LongDeserializer.deserialize(jsonIterator))
    }
  }

  final class BooleanField(name: String, field: Field, nullValue: NullValue, argIndex: Int) extends BeanField(name, field, nullValue, argIndex) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else field.setBoolean(instance, BooleanDeserializer.deserialize(jsonIterator))
    }
    override def readValue(jsonIterator: StringIterator): AnyRef = {
      if (jsonIterator.tryConsumeNull()) nullArgument() else java.lang.Boolean.valueOf(BooleanDeserializer.deserialize(jsonIterator))
    }
  }

  final class DoubleField(name: String, field: Field, nullValue: NullValue, argIndex: Int) extends BeanField(name, field, nullValue, argIndex) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else field.setDouble(instance, DoubleDeserializer.deserialize(jsonIterator))
    }
    override def readValue(jsonIterator: StringIterator): AnyRef = {
      if (jsonIterator.tryConsumeNull()) nullArgument() else java.lang.Double.valueOf(DoubleDeserializer.deserialize(jsonIterator))
    }
  }

  final class FloatField(name: String, field: Field, nullValue: NullValue, argIndex: Int) extends BeanField(name, field, nullValue, argIndex) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else field.setFloat(instance, FloatDeserializer.deserialize(jsonIterator))
    }
    override def readValue(jsonIterator: StringIterator): AnyRef = {
      if (jsonIterator.tryConsumeNull()) nullArgument() else java.lang.Float.valueOf(FloatDeserializer.deserialize(jsonIterator))
    }
  }

  final class ShortField(name: String, field: Field, nullValue: NullValue, argIndex: Int) extends BeanField(name, field, nullValue, argIndex) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else field.setShort(instance, ShortDeserializer.deserialize(jsonIterator))
    }
    override def readValue(jsonIterator: StringIterator): AnyRef = {
      if (jsonIterator.tryConsumeNull()) nullArgument() else java.lang.Short.valueOf(ShortDeserializer.deserialize(jsonIterator))
    }
  }

  final class ByteField(name: String, field: Field, nullValue: NullValue, argIndex: Int) extends BeanField(name, field, nullValue, argIndex) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else field.setByte(instance, ByteDeserializer.deserialize(jsonIterator))
    }
    override def readValue(jsonIterator: StringIterator): AnyRef = {
      if (jsonIterator.tryConsumeNull()) nullArgument() else java.lang.Byte.valueOf(ByteDeserializer.deserialize(jsonIterator))
    }
  }

  final class CharField(name: String, field: Field, nullValue: NullValue, argIndex: Int) extends BeanField(name, field, nullValue, argIndex) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else field.setChar(instance, CharDeserializer.deserialize(jsonIterator))
    }
    override def readValue(jsonIterator: StringIterator): AnyRef = {
      if (jsonIterator.tryConsumeNull()) nullArgument() else java.lang.Character.valueOf(CharDeserializer.deserialize(jsonIterator))
    }
  }
}
