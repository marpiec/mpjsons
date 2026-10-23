package io.mpjsons.impl.serializer

import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.serializer.common.IteratorSerializer
import io.mpjsons.impl.util.{Context, TypesUtil}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class ArraySerializer[E](serializerFactory: SerializerFactory, tpe: Type, context: Context)
  extends IteratorSerializer[E, Array[E]](serializerFactory, tpe, context) with Utf8Writer[Array[E]] {

  /**
   * Arrays of primitives written by the built-in element serializers are written without boxing every element;
   * the output (and the error for non-finite numbers) is the same as the element serializer's.
   */
  private val primitiveKind: Int = {
    // the same serializers are used for java.lang boxed elements, so the element class must be primitive as well
    def elementClass: Class[_] = TypesUtil.getClassFromType[Any](TypesUtil.getSubElementsType(tpe))
    val serializer: AnyRef = subTypeSerializer
    if ((serializer eq IntSerializer) && elementClass == java.lang.Integer.TYPE) ArraySerializer.IntKind
    else if ((serializer eq LongSerializer) && elementClass == java.lang.Long.TYPE) ArraySerializer.LongKind
    else if ((serializer eq DoubleSerializer) && elementClass == java.lang.Double.TYPE) ArraySerializer.DoubleKind
    else if ((serializer eq FloatSerializer) && elementClass == java.lang.Float.TYPE) ArraySerializer.FloatKind
    else if ((serializer eq ShortSerializer) && elementClass == java.lang.Short.TYPE) ArraySerializer.ShortKind
    else if ((serializer eq ByteSerializer) && elementClass == java.lang.Byte.TYPE) ArraySerializer.ByteKind
    else if ((serializer eq BooleanSerializer) && elementClass == java.lang.Boolean.TYPE) ArraySerializer.BooleanKind
    else ArraySerializer.OtherKind
  }

  override def serialize(obj: Array[E], jsonBuilder: StringBuilder): Unit = {
    primitiveKind match {
      case ArraySerializer.OtherKind => serializeIterator(obj.iterator, jsonBuilder)
      case ArraySerializer.IntKind => writeInts(obj.asInstanceOf[Array[Int]], jsonBuilder.underlying)
      case ArraySerializer.LongKind => writeLongs(obj.asInstanceOf[Array[Long]], jsonBuilder.underlying)
      case ArraySerializer.DoubleKind => writeDoubles(obj.asInstanceOf[Array[Double]], jsonBuilder.underlying)
      case ArraySerializer.FloatKind => writeFloats(obj.asInstanceOf[Array[Float]], jsonBuilder.underlying)
      case ArraySerializer.ShortKind => writeShorts(obj.asInstanceOf[Array[Short]], jsonBuilder.underlying)
      case ArraySerializer.ByteKind => writeBytes(obj.asInstanceOf[Array[Byte]], jsonBuilder.underlying)
      case ArraySerializer.BooleanKind => writeBooleans(obj.asInstanceOf[Array[Boolean]], jsonBuilder.underlying)
    }
  }

  private def writeInts(array: Array[Int], builder: java.lang.StringBuilder): Unit = {
    builder.append('[')
    var i = 0
    while (i < array.length) {
      if (i > 0) builder.append(',')
      builder.append(array(i))
      i += 1
    }
    builder.append(']')
  }

  private def writeLongs(array: Array[Long], builder: java.lang.StringBuilder): Unit = {
    builder.append('[')
    var i = 0
    while (i < array.length) {
      if (i > 0) builder.append(',')
      builder.append(array(i))
      i += 1
    }
    builder.append(']')
  }

  private def writeDoubles(array: Array[Double], builder: java.lang.StringBuilder): Unit = {
    builder.append('[')
    var i = 0
    while (i < array.length) {
      if (i > 0) builder.append(',')
      val value = array(i)
      if (!java.lang.Double.isFinite(value)) {
        throw new IllegalArgumentException("Non-finite floating point value " + value + " cannot be represented in JSON")
      }
      builder.append(value)
      i += 1
    }
    builder.append(']')
  }

  private def writeFloats(array: Array[Float], builder: java.lang.StringBuilder): Unit = {
    builder.append('[')
    var i = 0
    while (i < array.length) {
      if (i > 0) builder.append(',')
      val value = array(i)
      if (!java.lang.Float.isFinite(value)) {
        throw new IllegalArgumentException("Non-finite floating point value " + value + " cannot be represented in JSON")
      }
      builder.append(value)
      i += 1
    }
    builder.append(']')
  }

  private def writeShorts(array: Array[Short], builder: java.lang.StringBuilder): Unit = {
    builder.append('[')
    var i = 0
    while (i < array.length) {
      if (i > 0) builder.append(',')
      builder.append(array(i).toInt)
      i += 1
    }
    builder.append(']')
  }

  private def writeBytes(array: Array[Byte], builder: java.lang.StringBuilder): Unit = {
    builder.append('[')
    var i = 0
    while (i < array.length) {
      if (i > 0) builder.append(',')
      builder.append(array(i).toInt)
      i += 1
    }
    builder.append(']')
  }

  private def writeBooleans(array: Array[Boolean], builder: java.lang.StringBuilder): Unit = {
    builder.append('[')
    var i = 0
    while (i < array.length) {
      if (i > 0) builder.append(',')
      builder.append(if (array(i)) "true" else "false")
      i += 1
    }
    builder.append(']')
  }

  override def writeUtf8(obj: Array[E], out: Utf8Output): Unit = {
    primitiveKind match {
      case ArraySerializer.OtherKind => writeIterator(obj.iterator, out)
      case ArraySerializer.IntKind =>
        val array = obj.asInstanceOf[Array[Int]]
        out.writeByte('[')
        var i = 0
        while (i < array.length) {
          if (i > 0) out.writeByte(',')
          out.writeInt(array(i))
          i += 1
        }
        out.writeByte(']')
      case ArraySerializer.LongKind =>
        val array = obj.asInstanceOf[Array[Long]]
        out.writeByte('[')
        var i = 0
        while (i < array.length) {
          if (i > 0) out.writeByte(',')
          out.writeLong(array(i))
          i += 1
        }
        out.writeByte(']')
      case ArraySerializer.DoubleKind =>
        val array = obj.asInstanceOf[Array[Double]]
        out.writeByte('[')
        var i = 0
        while (i < array.length) {
          if (i > 0) out.writeByte(',')
          val value = array(i)
          if (!java.lang.Double.isFinite(value)) {
            throw new IllegalArgumentException("Non-finite floating point value " + value + " cannot be represented in JSON")
          }
          out.writeDouble(value)
          i += 1
        }
        out.writeByte(']')
      case ArraySerializer.FloatKind =>
        val array = obj.asInstanceOf[Array[Float]]
        out.writeByte('[')
        var i = 0
        while (i < array.length) {
          if (i > 0) out.writeByte(',')
          val value = array(i)
          if (!java.lang.Float.isFinite(value)) {
            throw new IllegalArgumentException("Non-finite floating point value " + value + " cannot be represented in JSON")
          }
          out.writeFloat(value)
          i += 1
        }
        out.writeByte(']')
      case ArraySerializer.ShortKind =>
        val array = obj.asInstanceOf[Array[Short]]
        out.writeByte('[')
        var i = 0
        while (i < array.length) {
          if (i > 0) out.writeByte(',')
          out.writeInt(array(i).toInt)
          i += 1
        }
        out.writeByte(']')
      case ArraySerializer.ByteKind =>
        val array = obj.asInstanceOf[Array[Byte]]
        out.writeByte('[')
        var i = 0
        while (i < array.length) {
          if (i > 0) out.writeByte(',')
          out.writeInt(array(i).toInt)
          i += 1
        }
        out.writeByte(']')
      case ArraySerializer.BooleanKind =>
        val array = obj.asInstanceOf[Array[Boolean]]
        out.writeByte('[')
        var i = 0
        while (i < array.length) {
          if (i > 0) out.writeByte(',')
          out.writeAscii(if (array(i)) "true" else "false")
          i += 1
        }
        out.writeByte(']')
    }
  }

  override private[mpjsons] def writesUtf8Itself: Boolean = getClass eq classOf[ArraySerializer[_]]
}

private object ArraySerializer {
  final val OtherKind = 0
  final val IntKind = 1
  final val LongKind = 2
  final val DoubleKind = 3
  final val FloatKind = 4
  final val ShortKind = 5
  final val ByteKind = 6
  final val BooleanKind = 7
}
