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
 */
abstract class BeanField(val name: String, val field: Field, nullValue: NullValue) {
  val hash: Int = name.hashCode

  /** Reads the value at the cursor and stores it into `instance`. */
  def set(instance: AnyRef, jsonIterator: StringIterator): Unit

  protected final def setNull(instance: AnyRef): Unit = field.set(instance, nullValue.get())
}

object BeanField {

  def apply(name: String, field: Field, fieldType: Type, ownerTypeName: String, deserializer: JsonTypeDeserializer[_]): BeanField = {
    val javaType = field.getType
    val nullValue = NullValues.forField(fieldType, name, ownerTypeName)
    if (javaType == java.lang.Integer.TYPE && (deserializer eq IntDeserializer)) {
      new IntField(name, field, nullValue)
    } else if (javaType == java.lang.Long.TYPE && (deserializer eq LongDeserializer)) {
      new LongField(name, field, nullValue)
    } else if (javaType == java.lang.Boolean.TYPE && (deserializer eq BooleanDeserializer)) {
      new BooleanField(name, field, nullValue)
    } else if (javaType == java.lang.Double.TYPE && (deserializer eq DoubleDeserializer)) {
      new DoubleField(name, field, nullValue)
    } else if (javaType == java.lang.Float.TYPE && (deserializer eq FloatDeserializer)) {
      new FloatField(name, field, nullValue)
    } else if (javaType == java.lang.Short.TYPE && (deserializer eq ShortDeserializer)) {
      new ShortField(name, field, nullValue)
    } else if (javaType == java.lang.Byte.TYPE && (deserializer eq ByteDeserializer)) {
      new ByteField(name, field, nullValue)
    } else if (javaType == java.lang.Character.TYPE && (deserializer eq CharDeserializer)) {
      new CharField(name, field, nullValue)
    } else {
      new ObjectField(name, field, nullValue, deserializer.asInstanceOf[JsonTypeDeserializer[AnyRef]])
    }
  }

  final class ObjectField(name: String, field: Field, nullValue: NullValue, deserializer: JsonTypeDeserializer[AnyRef]) extends BeanField(name, field, nullValue) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else field.set(instance, deserializer.deserialize(jsonIterator))
    }
  }

  final class IntField(name: String, field: Field, nullValue: NullValue) extends BeanField(name, field, nullValue) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else field.setInt(instance, IntDeserializer.deserialize(jsonIterator))
    }
  }

  final class LongField(name: String, field: Field, nullValue: NullValue) extends BeanField(name, field, nullValue) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else field.setLong(instance, LongDeserializer.deserialize(jsonIterator))
    }
  }

  final class BooleanField(name: String, field: Field, nullValue: NullValue) extends BeanField(name, field, nullValue) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else field.setBoolean(instance, BooleanDeserializer.deserialize(jsonIterator))
    }
  }

  final class DoubleField(name: String, field: Field, nullValue: NullValue) extends BeanField(name, field, nullValue) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else field.setDouble(instance, DoubleDeserializer.deserialize(jsonIterator))
    }
  }

  final class FloatField(name: String, field: Field, nullValue: NullValue) extends BeanField(name, field, nullValue) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else field.setFloat(instance, FloatDeserializer.deserialize(jsonIterator))
    }
  }

  final class ShortField(name: String, field: Field, nullValue: NullValue) extends BeanField(name, field, nullValue) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else field.setShort(instance, ShortDeserializer.deserialize(jsonIterator))
    }
  }

  final class ByteField(name: String, field: Field, nullValue: NullValue) extends BeanField(name, field, nullValue) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else field.setByte(instance, ByteDeserializer.deserialize(jsonIterator))
    }
  }

  final class CharField(name: String, field: Field, nullValue: NullValue) extends BeanField(name, field, nullValue) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = {
      if (jsonIterator.tryConsumeNull()) setNull(instance) else field.setChar(instance, CharDeserializer.deserialize(jsonIterator))
    }
  }
}
