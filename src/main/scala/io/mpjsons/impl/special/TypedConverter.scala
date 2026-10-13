package io.mpjsons.impl.special

import io.mpjsons.impl.util.{Context, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, SerializerFactory, StringIterator}
import io.mpjsons.{JsonTypeDeserializer, JsonTypeSerializer}

import java.util.concurrent.ConcurrentHashMap
import scala.reflect.runtime.universe._


object TypedConverter {

  class TypedSerializer[T <: AnyRef](serializerFactory: SerializerFactory) extends JsonTypeSerializer[T] {

    private val serializers = new ConcurrentHashMap[Class[_], JsonTypeSerializer[AnyRef]]()

    override def serialize(obj: T, jsonBuilder: StringBuilder): Unit = {
      val clazz = obj.getClass
      var innerElementSerializer = serializers.get(clazz)
      if (innerElementSerializer == null) {
        innerElementSerializer = serializerFactory.getSerializer(TypesUtil.getTypeFromClass(clazz), Context(List.empty, Map.empty), allowSuperType = false).asInstanceOf[JsonTypeSerializer[AnyRef]]
        serializers.putIfAbsent(clazz, innerElementSerializer)
      }

      jsonBuilder.append("{\"").append(clazz.getSimpleName).append("\":")
      innerElementSerializer.serialize(obj, jsonBuilder)
      jsonBuilder.append('}')
    }
  }


  class TypedDeserializer[T <: AnyRef](packageName: String, deserializerFactory: DeserializerFactory)
    extends JsonTypeDeserializer[T] {

    private val deserializers = new ConcurrentHashMap[String, JsonTypeDeserializer[AnyRef]]()

    override def deserialize(jsonIterator: StringIterator): T = {

      jsonIterator.consumeObjectStart()

      val typeName = extractTypeName(jsonIterator)

      var innerElementDeserializer = deserializers.get(typeName)
      if (innerElementDeserializer == null) {
        val elementType = TypesUtil.getTypeFromClass(Class.forName(packageName + "." + typeName))
        innerElementDeserializer = deserializerFactory.getDeserializer(elementType, Context(List.empty, Map.empty), allowSuperType = false).asInstanceOf[JsonTypeDeserializer[AnyRef]]
        deserializers.putIfAbsent(typeName, innerElementDeserializer)
      }

      jsonIterator.skipWhitespaceChars()
      jsonIterator.nextChar()

      jsonIterator.skipWhitespaceChars()
      val value = innerElementDeserializer.deserialize(jsonIterator).asInstanceOf[T]

      jsonIterator.skipWhitespaceChars()
      jsonIterator.nextChar()

      value
    }

    def extractTypeName(jsonIterator: StringIterator): String = {
      val deserializer = deserializerFactory.getDeserializer[String](typeOf[String], Context(List.empty, Map.empty))
      jsonIterator.skipWhitespaceChars()
      deserializer.deserialize(jsonIterator)
    }

  }

}
