package io.mpjsons.impl.special

import io.mpjsons.impl.util.{Context, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, SerializerFactory, StringIterator}
import io.mpjsons.{JsonTypeDeserializer, JsonTypeSerializer}

import scala.reflect.runtime.universe._


object TypedConverter {

  class TypedSerializer[T <: AnyRef](serializerFactory: SerializerFactory) extends JsonTypeSerializer[T] {

    private var serializers: Map[String, JsonTypeSerializer[AnyRef]] = Map.empty

    override def serialize(obj: T, jsonBuilder: StringBuilder): Unit = {
      val typeName = obj.getClass.getSimpleName
      val innerElementSerializer = serializers.getOrElse(typeName, {
        val s = serializerFactory.getSerializer(TypesUtil.getTypeFromClass(obj.getClass), Context(List.empty, Map.empty), allowSuperType = false).asInstanceOf[JsonTypeSerializer[AnyRef]]
        serializers += typeName -> s
        s
      })

      val simpleName: String = obj.getClass.getSimpleName
      jsonBuilder.append("{\"" + simpleName + "\":")

      innerElementSerializer.serialize(obj, jsonBuilder)
      jsonBuilder.append('}')
    }
  }


  class TypedDeserializer[T <: AnyRef](packageName: String, deserializerFactory: DeserializerFactory)
    extends JsonTypeDeserializer[T] {

    private var deserializers: Map[String, JsonTypeDeserializer[AnyRef]] = Map.empty

    override def deserialize(jsonIterator: StringIterator): T = {

      jsonIterator.consumeObjectStart()

      val typeName = extractTypeName(jsonIterator)

      val innerElementDeserializer = deserializers.getOrElse(typeName, {
        val elementType = TypesUtil.getTypeFromClass(Class.forName(packageName + "." + typeName))
        val d = deserializerFactory.getDeserializer(elementType, Context(List.empty, Map.empty), allowSuperType = false).asInstanceOf[JsonTypeDeserializer[AnyRef]]
        deserializers += typeName -> d
        d
      })

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




