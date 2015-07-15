package io.mpjsons.impl.special

import io.mpjsons.impl.deserializer.BeanDeserializer
import io.mpjsons.impl.serializer.BeanSerializer
import io.mpjsons.impl.util.{Context, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, SerializerFactory, StringIterator}
import io.mpjsons.{JsonTypeDeserializer, JsonTypeSerializer}

import scala.reflect.runtime.universe._


object TypedConverter {

  class TypedSerializer[T <: AnyRef](packageName: String, serializerFactory: SerializerFactory)
                                    (implicit tag: TypeTag[T]) extends JsonTypeSerializer[T] {

    private var serializers = Map[String, BeanSerializer]()

    override def serialize(obj: T, jsonBuilder: StringBuilder): Unit = {
      val typeName = obj.getClass.getSimpleName
      val innerElementSerializer = serializers.getOrElse(typeName, {
        val s = new BeanSerializer(serializerFactory, TypesUtil.getTypeFromClass(obj.getClass), Context(List(), Map()))
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

    private var deserializers = Map[String, BeanDeserializer[AnyRef]]()

    override def deserialize(jsonIterator: StringIterator): T = {

      jsonIterator.consumeObjectStart()

      val typeName = extractTypeName(jsonIterator)

      val innerElementDeserializer = deserializers.getOrElse(typeName, {
        val elementType = TypesUtil.getTypeFromClass(Class.forName(packageName + "." + typeName))
        val d = new BeanDeserializer[AnyRef](deserializerFactory, elementType, Context(List(), Map()))
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
      val deserializer = deserializerFactory.getDeserializer[String](typeOf[String], Context(List(), Map()))
      jsonIterator.skipWhitespaceChars()
      deserializer.deserialize(jsonIterator)
    }

  }

}




