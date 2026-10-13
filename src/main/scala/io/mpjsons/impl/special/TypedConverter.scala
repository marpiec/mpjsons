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


  /**
   * Reads {"SimpleName":{...}} and resolves SimpleName to a class, in this order:
   *  1. the known subclasses of a sealed marked type (works wherever they are declared),
   *  2. `binaryNamePrefix + SimpleName`, i.e. a sibling of the marked type (same package for top-level types,
   *     same enclosing object for nested types).
   *
   * @param binaryNamePrefix prefix of the marked type's binary class name up to and including the last '.' or '$'
   * @param knownSubclasses  simple class name -> class, for sealed marked types
   * @param markedTypeName   name of the marked type, for error messages
   */
  class TypedDeserializer[T <: AnyRef](binaryNamePrefix: String, knownSubclasses: Map[String, Class[_]], markedTypeName: String,
                                       deserializerFactory: DeserializerFactory)
    extends JsonTypeDeserializer[T] {

    /** Backward compatible constructor: subtypes are looked up in the given package. */
    def this(packageName: String, deserializerFactory: DeserializerFactory) =
      this(packageName + ".", Map.empty, packageName, deserializerFactory)

    private val deserializers = new ConcurrentHashMap[String, JsonTypeDeserializer[AnyRef]]()

    override def deserialize(jsonIterator: StringIterator): T = {

      jsonIterator.consumeObjectStart()

      val typeName = extractTypeName(jsonIterator)

      var innerElementDeserializer = deserializers.get(typeName)
      if (innerElementDeserializer == null) {
        val elementType = TypesUtil.getTypeFromClass(resolveClass(typeName))
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

    private def resolveClass(typeName: String): Class[_] = {
      knownSubclasses.get(typeName) match {
        case Some(clazz) => clazz
        case None =>
          try {
            Class.forName(binaryNamePrefix + typeName)
          } catch {
            case e: ClassNotFoundException =>
              throw new IllegalArgumentException("Unknown type name '" + typeName + "' for typed class " + markedTypeName +
                ": no such class " + binaryNamePrefix + typeName +
                (if (knownSubclasses.nonEmpty) ", known subclasses: " + knownSubclasses.keys.toList.sorted.mkString(", ") else ""), e)
          }
      }
    }

    def extractTypeName(jsonIterator: StringIterator): String = {
      val deserializer = deserializerFactory.getDeserializer[String](typeOf[String], Context(List.empty, Map.empty))
      jsonIterator.skipWhitespaceChars()
      deserializer.deserialize(jsonIterator)
    }

  }

}
