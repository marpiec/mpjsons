package io.mpjsons.impl.special

import io.mpjsons.impl.util.Messages

import io.mpjsons.impl.util.{Context, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, SerializerFactory, StringIterator}
import io.mpjsons.impl.serializer.{StringSerializer, Utf8Output, Utf8Writer}
import io.mpjsons.impl.deserializer.values.StringDeserializer
import io.mpjsons.{JsonTypeDeserializer, JsonTypeSerializer}

import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import scala.reflect.runtime.universe._


object TypedConverter {

  /** Type names matched in place by one TypedDeserializer (see knownNames); further names use the map only. */
  private final val MaxKnownNames = 32

  private final class KnownName(val name: String, val deserializer: JsonTypeDeserializer[AnyRef])

  /**
   * Writes {"Name":{...}} where Name is the `@typeName` alias of the runtime class, or its simple name.
   *
   * @param readTable names the reader of this typed class can resolve (see TypeNames.readTable); an alias that is not
   *                  in it would produce JSON that cannot be read back, so writing such a class fails. Null skips
   *                  that check (backward compatible constructor).
   * @param siblings  further aliases the reader resolves, found on the class path (see TypeNames.Siblings); may be null
   */
  class TypedSerializer[T <: AnyRef](serializerFactory: SerializerFactory, readTable: Map[String, Class[_]], markedTypeName: String,
                                     siblings: TypeNames.Siblings)
    extends JsonTypeSerializer[T] with Utf8Writer[T] {

    def this(serializerFactory: SerializerFactory, readTable: Map[String, Class[_]], markedTypeName: String) =
      this(serializerFactory, readTable, markedTypeName, null)

    def this(serializerFactory: SerializerFactory) = this(serializerFactory, null, "")

    private final class Entry(val prefix: String, val serializer: JsonTypeSerializer[AnyRef]) {
      val prefixBytes: Array[Byte] = prefix.getBytes(StandardCharsets.UTF_8)
      val writer: Utf8Writer[AnyRef] = Utf8Writer.of(serializer)
    }

    private val entries = new ConcurrentHashMap[Class[_], Entry]()

    override def serialize(obj: T, jsonBuilder: StringBuilder): Unit = {
      val entry = entryFor(obj.getClass)

      jsonBuilder.append(entry.prefix)
      entry.serializer.serialize(obj, jsonBuilder)
      jsonBuilder.append('}')
    }

    override def writeUtf8(obj: T, out: Utf8Output): Unit = {
      val entry = entryFor(obj.getClass)

      out.writeBytes(entry.prefixBytes)
      entry.writer.writeUtf8(obj, out)
      out.writeByte('}')
    }

    override private[mpjsons] def writesUtf8Itself: Boolean = getClass eq classOf[TypedSerializer[_]]

    private def entryFor(clazz: Class[_]): Entry = {
      var entry = entries.get(clazz)
      if (entry == null) {
        entry = createEntry(clazz)
        entries.putIfAbsent(clazz, entry)
      }
      entry
    }

    private def createEntry(clazz: Class[_]): Entry = {
      val alias = TypeNames.aliasOf(clazz)
      if (alias.isDefined && readTable != null && !readTable.get(alias.get).contains(clazz) &&
        (siblings == null || !siblings.table.get(alias.get).contains(clazz))) {
        throw new IllegalArgumentException("@typeName(\"" + alias.get + "\") on " + clazz.getName + " cannot be resolved when reading " +
          markedTypeName + ": aliases are read for the known subclasses of a sealed typed class and for the subclasses found " +
          "in its package on the class path, and " + clazz.getSimpleName + " is neither")
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
   *     same enclosing object for nested types),
   *  3. the `@typeName` alias of such a sibling, found on the class path (see TypeNames.Siblings; listed only when
   *     the first two do not resolve the name).
   *
   * The JSON names the class, so it is treated as untrusted: a sibling name containing '.', '/', '[' or ';' is rejected,
   * the class is loaded without running its static initializer, and anything that is not a subtype of the marked class
   * is rejected before an instance is created.
   *
   * @param binaryNamePrefix prefix of the marked type's binary class name up to and including the last '.' or '$'
   * @param knownSubclasses  name -> class for sealed marked types, see TypeNames.readTable
   * @param markedTypeName   name of the marked type, for error messages
   * @param markedClass      the marked class; every resolved class must be a subtype of it
   * @param siblings         aliases found on the class path, or null
   */
  class TypedDeserializer[T <: AnyRef](binaryNamePrefix: String, knownSubclasses: Map[String, Class[_]], markedTypeName: String,
                                       markedClass: Class[_], deserializerFactory: DeserializerFactory, siblings: TypeNames.Siblings)
    extends JsonTypeDeserializer[T] {

    def this(binaryNamePrefix: String, knownSubclasses: Map[String, Class[_]], markedTypeName: String, markedClass: Class[_],
             deserializerFactory: DeserializerFactory) =
      this(binaryNamePrefix, knownSubclasses, markedTypeName, markedClass, deserializerFactory, null)

    /** Backward compatible constructor: subtypes are looked up in the given package. */
    def this(packageName: String, deserializerFactory: DeserializerFactory) =
      this(packageName + ".", Map.empty, packageName, classOf[AnyRef], deserializerFactory)

    private val deserializers = new ConcurrentHashMap[String, JsonTypeDeserializer[AnyRef]]()
    private val stringType = typeOf[String]
    private val rootContext = Context(List.empty, Map.empty)
    private val classLoader: ClassLoader = Option(markedClass.getClassLoader).getOrElse(getClass.getClassLoader)

    /**
     * Type names already read, with their deserializers, matched in place on the raw input (no String per value, no
     * hashing); copied on write. Only names without '"' and '\' are kept: for them a raw match is exactly what the
     * built-in String reader returns, so it is used only while that reader is in effect.
     */
    @volatile private var knownNames: Array[KnownName] = Array.empty

    override def deserialize(jsonIterator: StringIterator): T = {

      jsonIterator.consumeObjectStart()

      val stringDeserializer = deserializerFactory.getDeserializer[String](stringType, rootContext)
      jsonIterator.skipWhitespaceChars()
      var innerElementDeserializer = if (stringDeserializer eq StringDeserializer) matchKnownName(jsonIterator) else null
      if (innerElementDeserializer == null) {
        val typeName = stringDeserializer.deserialize(jsonIterator)

        innerElementDeserializer = deserializers.get(typeName)
        if (innerElementDeserializer == null) {
          val elementType = TypesUtil.getTypeFromClass(resolveClass(typeName))
          innerElementDeserializer = deserializerFactory.getDeserializer(elementType, Context(List.empty, Map.empty), allowSuperType = false).asInstanceOf[JsonTypeDeserializer[AnyRef]]
          deserializers.putIfAbsent(typeName, innerElementDeserializer)
        }
        remember(typeName, innerElementDeserializer)
      }

      jsonIterator.skipWhitespaceChars()
      jsonIterator.nextChar()

      jsonIterator.skipWhitespaceChars()
      val value = innerElementDeserializer.deserialize(jsonIterator).asInstanceOf[T]

      jsonIterator.skipWhitespaceChars()
      jsonIterator.nextChar()

      value
    }

    /** The deserializer of the known name quoted at the cursor, with the cursor after its closing quote; else null. */
    private def matchKnownName(jsonIterator: StringIterator): JsonTypeDeserializer[AnyRef] = {
      if (jsonIterator.currentChar == '"') {
        val names = knownNames
        val input = jsonIterator.stringValue
        val start = jsonIterator.nextIndex
        var index = 0
        while (index < names.length) {
          val known = names(index)
          val end = start + known.name.length
          if (end < jsonIterator.stringLength && input.charAt(end) == '"' && input.regionMatches(start, known.name, 0, known.name.length)) {
            jsonIterator.seek(end + 1)
            return known.deserializer
          }
          index += 1
        }
      }
      null
    }

    private def remember(typeName: String, deserializer: JsonTypeDeserializer[AnyRef]): Unit = {
      if (knownNames.length < TypedConverter.MaxKnownNames && typeName.indexOf('"') < 0 && typeName.indexOf('\\') < 0) {
        synchronized {
          val names = knownNames
          if (names.length < TypedConverter.MaxKnownNames && !names.exists(_.name == typeName)) {
            knownNames = names :+ new KnownName(typeName, deserializer)
          }
        }
      }
    }

    private def resolveClass(typeName: String): Class[_] = {
      val resolved = knownSubclasses.get(typeName) match {
        case Some(clazz) => clazz
        case None => findSibling(typeName)
      }
      if (!markedClass.isAssignableFrom(resolved)) {
        throw new IllegalArgumentException("Type name '" + Messages.abbreviate(typeName) + "' for typed class " + markedTypeName +
          " resolves to " + resolved.getName + ", which is not a subtype of it")
      }
      resolved
    }

    private def findSibling(typeName: String): Class[_] = {
      val simpleName = typeName.nonEmpty && !typeName.exists(c => c == '.' || c == '/' || c == '[' || c == ';')
      var notFound: Throwable = null
      if (simpleName) {
        try {
          return Class.forName(binaryNamePrefix + typeName, false, classLoader) // no static initializer runs here
        } catch {
          case e @ (_: ClassNotFoundException | _: LinkageError) => notFound = e
        }
      }
      siblingAlias(typeName).getOrElse {
        if (!simpleName) {
          throw new IllegalArgumentException("Invalid type name '" + Messages.abbreviate(typeName) + "' for typed class " + markedTypeName +
            ": expected a simple class name or a @typeName alias")
        }
        throw new IllegalArgumentException("Unknown type name '" + Messages.abbreviate(typeName) + "' for typed class " + markedTypeName +
          ": no such class " + binaryNamePrefix + Messages.abbreviate(typeName) +
          (if (knownSubclasses.nonEmpty) ", known subclasses: " + knownSubclasses.keys.toList.sorted.mkString(", ") else ""), notFound)
      }
    }

    /** The sibling whose `@typeName` alias is `typeName`, found on the class path (the first call lists it). */
    private def siblingAlias(typeName: String): Option[Class[_]] = if (siblings == null) None else siblings.table.get(typeName)

    /**
     * The same reader, accepting only subclasses of `requiredClass` (a subtype of the marked class). Used when a
     * converter registered for a supertype reads a more specific declared type (markTypedSuperClass).
     */
    def restrictedTo(requiredClass: Class[_], requiredTypeName: String): TypedDeserializer[T] =
      new TypedDeserializer[T](binaryNamePrefix, knownSubclasses, requiredTypeName, requiredClass, deserializerFactory, siblings)

    def extractTypeName(jsonIterator: StringIterator): String = {
      val deserializer = deserializerFactory.getDeserializer[String](stringType, rootContext)
      jsonIterator.skipWhitespaceChars()
      deserializer.deserialize(jsonIterator)
    }

  }

}
