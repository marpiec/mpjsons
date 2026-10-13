package io.mpjsons.impl.special

import io.mpjsons.impl.util.Messages

import io.mpjsons.impl.util.{Context, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, SerializerFactory, StringIterator}
import io.mpjsons.impl.serializer.StringSerializer
import io.mpjsons.{JsonTypeDeserializer, JsonTypeSerializer}

import java.util.concurrent.ConcurrentHashMap
import scala.reflect.runtime.universe._


object TypedConverter {

  /**
   * Writes {"Name":{...}} where Name is the `@typeName` alias of the runtime class, or its simple name.
   *
   * @param readTable names the reader of this typed class can resolve (see TypeNames.readTable); an alias that is not
   *                  in it would produce JSON that cannot be read back, so writing such a class fails. Null skips
   *                  that check (backward compatible constructor).
   */
  class TypedSerializer[T <: AnyRef](serializerFactory: SerializerFactory, readTable: Map[String, Class[_]], markedTypeName: String)
    extends JsonTypeSerializer[T] {

    def this(serializerFactory: SerializerFactory) = this(serializerFactory, null, "")

    private final class Entry(val prefix: String, val serializer: JsonTypeSerializer[AnyRef])

    private val entries = new ConcurrentHashMap[Class[_], Entry]()

    override def serialize(obj: T, jsonBuilder: StringBuilder): Unit = {
      val clazz = obj.getClass
      var entry = entries.get(clazz)
      if (entry == null) {
        entry = createEntry(clazz)
        entries.putIfAbsent(clazz, entry)
      }

      jsonBuilder.append(entry.prefix)
      entry.serializer.serialize(obj, jsonBuilder)
      jsonBuilder.append('}')
    }

    private def createEntry(clazz: Class[_]): Entry = {
      val alias = TypeNames.aliasOf(clazz)
      if (alias.isDefined && readTable != null && !readTable.get(alias.get).contains(clazz)) {
        throw new IllegalArgumentException("@typeName(\"" + alias.get + "\") on " + clazz.getName + " cannot be resolved when reading " +
          markedTypeName + ": aliases need a sealed typed class whose known subclasses include " + clazz.getSimpleName)
      }
      val name = new StringBuilder("{")
      StringSerializer.serialize(alias.getOrElse(clazz.getSimpleName), name)
      name.append(':')
      val serializer = serializerFactory.getSerializer(TypesUtil.getTypeFromClass(clazz), Context(List.empty, Map.empty), allowSuperType = false).asInstanceOf[JsonTypeSerializer[AnyRef]]
      new Entry(name.toString, serializer)
    }
  }


  /**
   * Reads {"SimpleName":{...}} and resolves SimpleName to a class, in this order:
   *  1. the names of the known subclasses of a sealed marked type: `@typeName` aliases and simple names (works
   *     wherever they are declared),
   *  2. `binaryNamePrefix + SimpleName`, i.e. a sibling of the marked type (same package for top-level types,
   *     same enclosing object for nested types).
   *
   * @param binaryNamePrefix prefix of the marked type's binary class name up to and including the last '.' or '$'
   * @param knownSubclasses  name -> class for sealed marked types, see TypeNames.readTable
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
              throw new IllegalArgumentException("Unknown type name '" + Messages.abbreviate(typeName) + "' for typed class " + markedTypeName +
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
