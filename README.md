# mpjsons

**mpjsons** is a fast, minimal-API object ⇄ JSON conversion library for Scala.

Its whole design goal is the simplest possible API: serialize any object to JSON and
deserialize JSON back into a typed object with **no hand-written serializers**. Type
information is discovered through Scala 2.13 runtime reflection, and the generated
per-type (de)serializers are cached, so after warm-up conversion runs through tight,
allocation-conscious loops.

```scala
import io.mpjsons.MPJsons

val mpjsons = new MPJsons

case class User(firstName: String, lastName: String)

val json = mpjsons.serialize(User("John", "Doe"))
// {"lastName":"Doe","firstName":"John"}

val user = mpjsons.deserialize[User](json)
// User(John,Doe)
```

For most use cases, `serialize` and `deserialize` are all you will ever need.

---

## Features

- **Zero boilerplate** — serialize/deserialize case classes and mutable beans directly from their fields.
- **Generics** — `List[User]`, `Map[String, List[User]]`, and other nested type parameters are resolved automatically.
- **Collections** — `List`, `Vector`, `Seq`, `Set`, `Queue`, `Stream`, `Array`, and `Map`, both mutable and immutable, including `Hash*`/`List*` variants.
- **Scala types** — `Option`, `Either`, and `Tuple1`–`Tuple6`.
- **`java.time`** — `LocalDate`, `LocalTime`, `LocalDateTime`, `Duration`.
- **Custom converters** — plug in your own (de)serializers for any type.
- **Polymorphic JSON** — embed type tags so collections of subtypes round-trip (`markTypedClass`).
- **Helpful errors** — deserialization failures are wrapped with a pointer to the exact spot in the JSON.
- **Hot-path helpers** — reusable, single-type `StaticSerializer`/`StaticDeserializer`.

## Requirements

- Scala **2.13.16**
- A JVM (deserialization can fall back to `sun.misc.Unsafe` for constructor-less classes — see [Limitations](#limitations--notes))

## Installation

mpjsons is **not published to Maven Central** or any public repository. Build it from
source and consume it locally.

Clone and compile:

```bash
git clone <repo-url>
cd mpjsons
sbt compile
sbt test        # optional: run the full spec suite
```

To use it from another project, enable local publishing. The `publishLocal` task is
disabled in `build.sbt` (`publishLocal := {}`); remove that line, then:

```bash
sbt publishLocal
```

and add the dependency to your project:

```scala
libraryDependencies += "io.mpjsons" %% "mpjsons" % "0.6.51"
```

(Current version: `0.6.51`, Scala `2.13.16`. Runtime dependencies: `scala-reflect`, `slf4j-api`.)

---

## Usage manual

Create one `MPJsons` instance and reuse it:

```scala
import io.mpjsons.MPJsons

val mpjsons = new MPJsons
```

The constructor takes two optional flags (both default to `false`):

```scala
new MPJsons(ignoreNonExistingFields = false, ignoreNullFields = false)
```

- `ignoreNonExistingFields` — on deserialize, silently skip JSON fields that don't exist on the target type instead of throwing.
- `ignoreNullFields` — on serialize, omit `null` fields instead of throwing.

Both are covered in detail below.

### Case classes and beans

Case classes and mutable (`var`-based) beans both work. Deserialization mutates fields
in place, so target types should be case classes or beans with `var` fields.

```scala
case class User(a: String)
mpjsons.serialize(User("Hello"))               // {"a":"Hello"}
mpjsons.deserialize[User]("""{"a":"Hello"}""") // User(Hello)

class InnerObject {
  var intValue: Int = _
  var stringValue: String = _
}
mpjsons.deserialize[InnerObject]("""{"intValue":10,"stringValue":"Hello"}""")
```

The parser is lenient about input: it tolerates whitespace and accepts field names with
or without quotes.

```scala
mpjsons.deserialize[InnerObject](""" {  intValue : 10 , stringValue : "Hello" } """)
```

> **Note:** JSON key order in the output follows reflection member order, not declaration order.

### Collections

Lists, vectors, sets, queues, streams, and arrays serialize as JSON arrays:

```scala
mpjsons.serialize(List("a", "b", "c"))   // ["a","b","c"]
mpjsons.serialize(Vector(1, 2, 3))       // [1,2,3]
mpjsons.serialize(Set[Long](1, 11, 111)) // [1,11,111]
```

You can serialize a top-level collection directly (no wrapper object needed):

```scala
val json = mpjsons.serialize(List("Zxy", "Xyz", "ZyX"))
val back = mpjsons.deserialize[List[String]](json)
```

Generics compose freely:

```scala
mpjsons.serialize(Map("group" -> List(User("Ann"), User("Bob"))))
mpjsons.deserialize[Map[String, List[User]]](json)
```

### Maps

By design, a regular `Map` serializes as a **JSON array of `[key, value]` pairs**, not as
a JSON object. This lets arbitrary (non-string) key types round-trip:

```scala
mpjsons.serialize(Map("a" -> "Ala", "k" -> "Kot"))
// [["a","Ala"],["k","Kot"]]

mpjsons.serialize(Map(1 -> 1224, 5 -> 5324))
// [[1,1224],[5,5324]]
```

> If you need the standard string-keyed object form (`{"a":"Ala","k":"Kot"}`), it is **not**
> selected automatically. Wire it up with `SimpleMapSerializer` / `SimpleMapDeserializer`
> via a [custom converter](#custom-converters) for your specific `Map[String, T]` type.

### Option, Either, Tuples

```scala
// Option — present values are inlined, None is absent
Some("test")   // serialized inline
None

// Either
Left(false)    // round-trips as Left(false)
Right(2)       // round-trips as Right(2)

// Tuples serialize as JSON arrays
mpjsons.serialize(Tuple1("Hello"))                 // ["Hello"]
mpjsons.serialize(("Hello", 2))                    // ["Hello",2]
mpjsons.serialize(("Hello", 2, "World", 5, "Hey")) // ["Hello",2,"World",5,"Hey"]
```

### java.time

Time types serialize as **structured objects**, not ISO-8601 strings:

```scala
mpjsons.serialize(LocalTime.of(12, 30, 17, 125))
// {"hour":12,"minute":30,"second":17,"nano":125}

mpjsons.serialize(LocalDate.of(2023, 7, 23))
// {"year":2023,"month":7,"day":23}

mpjsons.serialize(LocalDateTime.of(2023, 8, 23, 14, 30, 17, 125))
// {"date":{"year":2023,"month":8,"day":23},"time":{"hour":14,"minute":30,"second":17,"nano":125}}

mpjsons.serialize(java.time.Duration.ofSeconds(123, 456))
// {"seconds":123,"nanos":456}

mpjsons.deserialize[LocalDate]("""{"year":2023,"month":7,"day":23}""")
```

### Recursive and deeply nested types

Self-referential types are supported (element serializers for recursive types are resolved lazily):

```scala
case class RecursiveType(subtypes: List[RecursiveType])

val obj = RecursiveType(List(RecursiveType(List(RecursiveType(Nil))), RecursiveType(Nil)))
mpjsons.deserialize[RecursiveType](mpjsons.serialize(obj)) == obj  // true
```

### Numbers

Standard integer and floating-point formats are parsed, including scientific notation:

```scala
case class WithNumber(a: Int, b: Double, c: Double)
mpjsons.deserialize[WithNumber]("""{"a":30,"b":1.2e4,"c":2.1e2}""")
// WithNumber(30, 12000.0, 210.0)
```

### Ignoring unknown fields

By default, unknown JSON fields cause a `JsonInnerException`. Pass
`ignoreNonExistingFields = true` to skip them:

```scala
case class TestObject(a: Boolean, b: Int)
val json = """{"a": true, "c": "Hello", "d": [1,2,3,4], "b": 3}"""

new MPJsons(ignoreNonExistingFields = true).deserialize[TestObject](json)
// TestObject(true, 3)  — c and d are ignored

new MPJsons().deserialize[TestObject](json)
// throws JsonInnerException
```

### Nullable fields and null handling

By default, serializing a `null` field throws. Mark a field with `@nullable` to allow it
to be omitted from the output when it's null. On a case-class parameter, apply it as a
field meta-annotation:

```scala
import io.mpjsons.nullable
import scala.annotation.meta.field

case class SimpleObjectWithNulls(@(nullable @field) a: String, b: String)

mpjsons.serialize(SimpleObjectWithNulls(null, "ok"))   // OK — "a" is omitted
mpjsons.serialize(SimpleObjectWithNulls("fail", null)) // throws — "b" is not nullable
```

When every field of an object is `@nullable` and null, the object serializes to `{}` and
round-trips back to the all-null instance:

```scala
case class PrimitiveHolder(
  @(nullable @field) a: java.lang.Long,
  @(nullable @field) b: java.lang.Integer
)

mpjsons.serialize(PrimitiveHolder(null, null))     // {}
mpjsons.deserialize[PrimitiveHolder]("{}")          // PrimitiveHolder(null, null)
```

### Post-deserialization transform

Register a function to run on each instance of a type after deserialization — handy for
cleaning up `null`s left by missing JSON properties:

```scala
case class Sample(a: String, b: String)

mpjsons.postDeserializationTransform[Sample] { s =>
  if (s.b == null) s.copy(b = "") else s
}

mpjsons.deserialize[Sample]("""{a: "Hello"}""")  // Sample("Hello", "")
```

### Custom converters

To support a type mpjsons doesn't handle natively, implement `JsonTypeSerializer[T]`
(`serialize(obj, jsonBuilder): Unit`) and/or `JsonTypeDeserializer[T]`
(`deserialize(jsonIterator): T`), then register them. Registration takes factory
functions so your converter can resolve sub-(de)serializers from the factory.

```scala
mpjsons.registerConverter[SomeType](
  sf => new MyTypeSerializer(sf),
  df => new MyTypeDeserializer(df))
```

Related registration methods:

- `registerConverter[T](serializer, deserializer)` — for exactly type `T`.
- `registerSuperclassConverter[T](serializer, deserializer)` — for `T` and all its subtypes.
- `registerSerializerForAnySubtype[T](serializer)` — serialize-only, handles `T` and any subtype (including generic containers like `Wrapper[_]`).

```scala
case class Wrapper[T](value: T)

class WrapperSerializer extends JsonTypeSerializer[Wrapper[_]] {
  override def serialize(obj: Wrapper[_], jb: StringBuilder): Unit = {
    jb.append("{\"someValue\":")
    jb.append(obj.value.toString)
    jb.append("}")
  }
}

mpjsons.registerSerializerForAnySubtype[Wrapper[_]](sf => new WrapperSerializer)
mpjsons.serialize(Wrapper(123))  // {"someValue":123}
```

### Polymorphic / typed JSON

When a collection holds subtypes of a common trait, plain JSON loses the concrete type.
`markTypedClass[T]` embeds each value's class name as a wrapper so it round-trips:

```scala
sealed trait CustomDescription
case class TextDescription(text: String) extends CustomDescription
case class NumericDescription(number: Int) extends CustomDescription

mpjsons.markTypedClass[CustomDescription]

val json = mpjsons.serialize(
  List[CustomDescription](TextDescription("Hello"), NumericDescription(14)))
// [{"TextDescription":{"text":"Hello"}},{"NumericDescription":{"number":14}}]

mpjsons.deserialize[List[CustomDescription]](json)
// List(TextDescription("Hello"), NumericDescription(14))
```

The type tag is added only when serializing **through the supertype**. Serializing the
concrete type directly produces plain JSON:

```scala
trait CommonType
case class A(a: String) extends CommonType

mpjsons.markTypedClass[CommonType]

mpjsons.serialize(A("Hello"))                 // {"a":"Hello"}
mpjsons.serialize(A("Hello"): CommonType)     // {"A":{"a":"Hello"}}
```

> `markTypedSuperClass[T]` behaves the same but applies to every subclass of `T`. It is
> kept for backward compatibility and generally should not be needed.

### Performance: static (de)serializers

`serialize`/`deserialize` look the (de)serializer up per call (cached after the first
use). On hot paths you can bind a reusable, type-specialized instance once:

```scala
val ser = mpjsons.buildStaticSerializer[User]
val des = mpjsons.buildStaticDeserializer[User]

val json = ser.serialize(user)
val back = des.deserialize(json)
```

`StaticSerializer` also exposes `serializeToStringBuilder(obj): StringBuilder`, which
avoids the char-array copy of `.toString()` when a `StringBuilder` can be consumed directly.

### Error handling

Deserialization failures (malformed JSON, unknown fields in strict mode, type mismatches)
are wrapped in `io.mpjsons.impl.JsonInnerException`. The message points at the exact
location in the input where parsing failed:

```scala
import io.mpjsons.impl.JsonInnerException

try {
  mpjsons.deserialize[User]("""{"firstName":"Bob" """) // truncated
} catch {
  case e: JsonInnerException => println(e.getMessage)
}
```

---

## Limitations & notes

- **`Map` form is non-standard.** Regular maps serialize as an array of `[key, value]` pairs (see [Maps](#maps)); use `SimpleMap*` converters for the `{"k":"v"}` object form.
- **Sorted collections throw.** `SortedSet`, `TreeSet`, `SortedMap`, `TreeMap`, and `ListBuffer` are intentionally unsupported and throw.
- **Singleton `object`s are not supported.**
- **Constructor-less deserialization uses `sun.misc.Unsafe`.** When a type has no no-arg constructor, instances are allocated without running any constructor — so constructor-side defaults and validation are bypassed, and availability depends on the JVM.
- **Not thread-safe for cold types.** Internal caches are unsynchronized `var Map`s. A single `MPJsons` instance is not safe for concurrent *first-time* use of new types; once all types are warmed up it is effectively read-only. Warm up types up front if you serialize concurrently.

## Building & testing

```bash
sbt compile
sbt test                                       # all specs
sbt "testOnly io.mpjsons.BeanSerializationSpec" # a single spec
```

Specs live under `src/test/scala/io/mpjsons/`, one per feature area (collections, maps,
tuples, Option/Either, time, custom converters, malformed JSON, and more) and are the
authoritative reference for supported behavior.

## License

Released under the [MIT License](http://opensource.org/licenses/MIT).
