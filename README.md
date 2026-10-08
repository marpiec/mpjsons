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
// {"firstName":"John","lastName":"Doe"}

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
- **`java.time` and friends** — `LocalDate`, `LocalTime`, `LocalDateTime`, `Duration` (objects, or ISO-8601 strings with `isoTime`), `Instant`, `OffsetDateTime`, `ZonedDateTime`, `java.util.UUID` (strings), `BigDecimal` and `BigInt` (exact numbers).
- **Custom converters** — plug in your own (de)serializers for any type.
- **Polymorphic JSON** — embed type tags so collections of subtypes round-trip (`markTypedClass`).
- **Helpful errors** — deserialization failures are wrapped with a pointer to the exact spot in the JSON.
- **Hot-path helpers** — reusable, single-type `StaticSerializer`/`StaticDeserializer`.

## Requirements

- Scala **2.13.18**
- A JVM (deserialization can fall back to `sun.misc.Unsafe` for constructor-less classes — see [Limitations](#limitations--notes))

## Installation

mpjsons is **not published to Maven Central** or any public repository. Build it from
source and consume it locally.

Clone and build (requires Maven and a JDK):

```bash
git clone <repo-url>
cd mpjsons
mvn test        # optional: run the full spec suite
mvn install     # build and install into your local Maven repository (~/.m2)
```

and add the dependency to your project's `pom.xml`:

```xml
<dependency>
    <groupId>io.mpjsons</groupId>
    <artifactId>mpjsons_2.13</artifactId>
    <version>0.6.51</version>
</dependency>
```

To publish to the Nexus repositories configured in `distributionManagement`, bump the
version in `pom.xml` and run `mvn deploy`.

(Current version: `0.7.0`, Scala `2.13.18`. Runtime dependency: `scala-reflect`.)

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

> **Note:** JSON keys follow declaration order; fields inherited from a superclass come first.

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

A cyclic object graph, where an object refers back to itself through its fields, cannot be
written as JSON: serializing one fails with an `IllegalArgumentException` naming the type. For
untrusted input, set `MPJsonsConfig(maxDepth = ...)` to bound how deeply nested a document may be.

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

### Field names in JSON

By default the JSON key is the Scala field name. `@jsonName` overrides it, which is the
simplest way to make the output smaller or to match an external schema. It works on case
class parameters, `val`s and `var`s directly, without a meta-annotation, and also on
inherited fields.

```scala
import io.mpjsons.jsonName

case class User(@jsonName("n") firstName: String, @jsonName("l") lastName: String, age: Int)

mpjsons.serialize(User("John", "Doe", 42))        // {"n":"John","l":"Doe","age":42}
mpjsons.deserialize[User]("""{"n":"John","l":"Doe","age":42}""")
```

On read, the Scala field name is still accepted as a fallback, so JSON written before a field
was renamed keeps loading; if another field owns that name as its JSON name, the JSON name wins.
Two fields of one type must not share a JSON name: that is reported when the (de)serializer is
built.

### Leaving fields out of the output

`@skipSerialization` leaves a field out of the JSON that `serialize` writes. Reading is not
affected.

```scala
import io.mpjsons.skipSerialization

case class Session(user: String, @skipSerialization token: String)

mpjsons.serialize(Session("ann", "secret"))                        // {"user":"ann"}
mpjsons.deserialize[Session]("""{"user":"ann","token":"secret"}""") // Session("ann", "secret")
mpjsons.deserialize[Session]("""{"user":"ann"}""")                  // Session("ann", null)
```

- The key is still read, under its `@jsonName` if it has one. When the key is absent, the field
  gets zero, false, null or None, like any other absent key. A serialize-then-deserialize round
  trip therefore loses the value.
- No serializer is built for the field, so its type does not need to be serializable, and the
  null policy does not apply to it. A deserializer is still built, so the type must be readable.
- Like `@jsonName`, it works on case class parameters, `val`s and `var`s without a
  meta-annotation, and on inherited fields.

### Nullable fields and null handling

By default, serializing a `null` field throws. Mark a field with `@nullable` to allow it
to be omitted from the output when it's null. It works directly on case-class parameters
(the `@(nullable @field)` form shown below is still accepted):

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

#### The `null` literal

Nulls are accepted only where they are declared. A field declares that it may be null with
`@nullable`; an `Option` declares absence by its type. With the default `NullPolicy.Fail` this
holds in both directions:

- **Write**: a null `@nullable` field is omitted; any other null (a field, a collection element,
  a map key or value, a tuple slot, an `Either` or `Some` payload, the top-level value) throws.
- **Read**: `null` gives `null` for a `@nullable` reference field and `None` for an `Option`;
  anywhere else it is rejected with a message naming the field or type. A missing `Option` field
  also reads as `None`. Primitives (`Int`, `Boolean`, ...) never accept `null`.

```scala
import io.mpjsons.nullable

case class Profile(@nullable name: String, nickname: Option[String], city: String, age: Int)

mpjsons.deserialize[Profile]("""{"name":null,"nickname":null,"city":"Oslo","age":3}""") // Profile(null, None, "Oslo", 3)
mpjsons.deserialize[Profile]("""{"name":"Ann","city":"Oslo","age":3}""")              // nickname = None
mpjsons.deserialize[Profile]("""{"name":"Ann","city":null,"age":3}""")                // throws: city is not @nullable
mpjsons.serialize(Profile(null, None, "Oslo", 3))                                      // {"nickname":{},"city":"Oslo","age":3}
mpjsons.serialize(List("a", null))                                                    // throws
```

To tolerate nulls everywhere, choose another policy explicitly; it then applies to reading as well:

```scala
import io.mpjsons.NullPolicy

new MPJsons(nullPolicy = NullPolicy.Omit).serialize(Profile("Ann", None, null, 3))      // {"name":"Ann","nickname":{},"age":3}
new MPJsons(nullPolicy = NullPolicy.WriteNull).serialize(Profile("Ann", None, null, 3)) // {"name":"Ann","nickname":{},"city":null,"age":3}
new MPJsons(nullPolicy = NullPolicy.WriteNull).serialize(List("a", null))               // ["a",null]
```

`ignoreNullFields = true` is the same as `NullPolicy.Omit`.

### Compact output

`MPJsonsConfig` holds opt-in format levers. The defaults reproduce the historical format, and
the reader accepts both forms wherever they can be told apart, so an instance with a lever on can
still read data written with it off.

```scala
import io.mpjsons.{MPJsons, MPJsonsConfig}

val compact = new MPJsons(MPJsonsConfig(escapeSlash = false, compactOptions = true))

case class Profile(name: String, nickname: Option[String], homepage: Option[String])

compact.serialize(Profile("Ann", Some("annie"), None))
// {"name":"Ann","nickname":"annie"}      (historical: {"name":"Ann","nickname":{"value":"annie"},"homepage":{}})
```

| Lever | Default | Effect when changed |
|---|---|---|
| `escapeSlash = false` | `true` | `/` is written unescaped instead of `\/`. Reading is unaffected. |
| `compactOptions = true` | `false` | `Some(v)` is written as `v`; a `None` field is omitted and a `None` element is `null`. An `Option` directly inside another `Option` keeps the historical form so `Some(None)` survives. On read, the historical `{}` / `{"value":v}` is still recognised unless the element type is itself written as a JSON object (a bean, an `Option`, `Either`, a time type), in which case `{` is the inline element. |
| `mapsAsObjects = true` | `false` | Maps whose key type is `String`, a number, `Boolean` or `Char` are written as `{"key":value}` instead of `[["key",value]]`; other key types keep the pair form. A null value is treated like a null bean field (dropped under `NullPolicy.Omit`, `"key":null` under `WriteNull`, error under `Fail`). Both forms are always read. |
| `isoTime = true` | `false` | `LocalDate`, `LocalTime`, `LocalDateTime` and `Duration` are written as ISO-8601 strings (`"2023-07-23"`, `"PT1M"`) instead of objects. Both forms are always read. |
| `strict = true` | `false` | Reading rejects missing commas, trailing commas and content after the root value instead of tolerating them. Bare keys and whitespace are still accepted. |
| `omitDefaults = true` | `false` | Bean fields whose value equals what reading `{}` would produce (the no-arg constructor initialisers, or zero / false / null / None) are left out. Lossless for round trips through this library; such nulls are omitted regardless of `nullPolicy`. |
| `caseClassesViaConstructor = true` | `false` | Case classes are built by calling their primary constructor with the parsed values (constructor bodies such as `require` run; no final field is written reflectively, which future JDKs restrict). Absent keys still give zero / false / null / None. Costs some boxing per value. |
| `maxDepth = n` | `0` (unlimited) | Reading fails with a clear error once values (objects, collections, maps, Options, tuples) nest deeper than `n`, including unknown fields being skipped. Use it for untrusted input, which could otherwise end in a `StackOverflowError`. |

Other levers are listed in `docs/roadmap.md`.

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

Registrations take effect immediately, also for types that were already serialized or
deserialized: every registration drops the cached (de)serializers. Static serializers and
deserializers built earlier keep the behaviour they were built with, so build them after
registering. Register converters before sharing an instance between threads.

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

#### Type name aliases

By default the wrapper key is the subclass's simple name. `@typeName` sets a different one:
shorter output, and a renamed class can keep its old name on the wire. The simple class name is
still accepted when reading, so data written before the alias was added keeps loading.

```scala
import io.mpjsons.typeName

sealed trait Shape
@typeName("c") case class Circle(r: Double) extends Shape
@typeName("Square") case class Box(side: Int) extends Shape   // class renamed from Square

mpjsons.markTypedClass[Shape]
mpjsons.serialize(List[Shape](Circle(1.5), Box(2)))   // [{"c":{"r":1.5}},{"Square":{"side":2}}]
```

Aliases are resolved through the known subclasses of a **sealed** typed class. Writing an aliased
subclass of a non-sealed typed class fails with an explanation, because the reader could not
find it. Two subclasses that would write the same name are rejected by `markTypedClass`.

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

## Upgrading from 0.6.51 to 0.7.0

The default wire format is unchanged except for key order (declaration order, superclass fields first)
and a handful of fixes that change output or accepted input (inherited fields, generic beans, `null`,
absent `Option` fields reading as `None`, non-finite doubles). The full list, including API and
dependency changes, is in [docs/upgrading.md](docs/upgrading.md).

## Limitations & notes

- **Traits and abstract classes need `markTypedClass` or a converter.** Serializing or deserializing a value through such a type otherwise fails when first used, because the concrete class would be lost.
- **`Map` form is non-standard.** Regular maps serialize as an array of `[key, value]` pairs (see [Maps](#maps)); use `SimpleMap*` converters for the `{"k":"v"}` object form.
- **Sorted collections throw.** `SortedSet`, `TreeSet`, `SortedMap`, `TreeMap`, and `ListBuffer` are intentionally unsupported and throw.
- **Singleton `object`s** serialize to `{}` and deserialize to the singleton instance when the declared type is `Foo.type`; polymorphic use (an `object` behind a trait) still needs `markTypedClass`.
- **Constructor-less deserialization uses `sun.misc.Unsafe`.** When a type has no no-arg constructor, instances are allocated without running any constructor — so constructor-side defaults and validation are bypassed, and availability depends on the JVM.
- **Not thread-safe for cold types.** Internal caches are unsynchronized `var Map`s. A single `MPJsons` instance is not safe for concurrent *first-time* use of new types; once all types are warmed up it is effectively read-only. Warm up types up front if you serialize concurrently.

## Building & testing

```bash
mvn test                                          # all specs
mvn test -Dsuites=io.mpjsons.BeanSerializationSpec # a single spec
```

Specs live under `src/test/scala/io/mpjsons/`, one per feature area (collections, maps,
tuples, Option/Either, time, custom converters, malformed JSON, and more) and are the
authoritative reference for supported behavior.

## License

Released under the [MIT License](http://opensource.org/licenses/MIT).
