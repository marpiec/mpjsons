# Upgrading from 0.6.51

Everything below applies with the **default configuration** (`new MPJsons`): no format lever is on by
default, and the wire format of unchanged types is byte-identical except where this page says otherwise.
Items marked *fix* turn previously wrong or crashing behaviour into working behaviour; they are listed
because output or accepted input changes.

## Output (serialize)

| Change | Before | Now | Who is affected |
|---|---|---|---|
| **Key order** | reverse declaration order (`{"c":..,"b":..,"a":..}`) | declaration order, superclass fields first | anything comparing JSON as text: snapshot tests, hashes, diffs of stored documents. JSON readers are unaffected. |
| **Inherited fields** (*fix*) | fields declared in a superclass were silently dropped | written (and read) like own fields, placed before the subclass fields | types with class inheritance: their output gains keys |
| **Generic beans** (*fix*) | a second instantiation of `Box[T]` on the same instance reused the first one's element (de)serializers, e.g. `Box[String]` after `Box[Int]` wrote unquoted strings | correct per instantiation | anyone who worked around it with separate `MPJsons` instances can drop the workaround |
| **`NaN` / `Infinity`** | written as `NaN` / `Infinity` (not JSON; could not be read back) | `IllegalArgumentException` | code that serialized non-finite doubles |
| **null elements** | `NullPointerException` for `Some(null)`, `List("a", null)`, null map values, null tuple slots | `IllegalArgumentException` naming the place under the default `NullPolicy.Fail`; written as `null` under `Omit` / `WriteNull` | code relying on the exception type |
| **`serialize(null)`** | `NullPointerException` | `IllegalArgumentException` under `Fail`; writes `null` under `Omit` / `WriteNull` | |
| **Cyclic object graphs** | `StackOverflowError` | `IllegalArgumentException` naming the type ("Cyclic reference detected") | code catching `StackOverflowError` |
| **`SimpleMapSerializer` null values** | silently skipped regardless of configuration | follow `NullPolicy` like a bean field: `IllegalArgumentException` naming the key under the default `Fail`, skipped under `Omit` (and `ignoreNullFields = true`), `"k":null` under `WriteNull`; a null key throws instead of `NullPointerException` | users of the opt-in `SimpleMap*` converters with null values and the default policy |
| **`@nullable` on case class parameters** | ignored unless written as `@(nullable @field)` | effective directly on the parameter (any annotation whose name contains "nullable", e.g. `javax.annotation.Nullable`, counts) | null fields that used to throw are now omitted |
| **`java.*` types without a converter** | bean reflection: `InaccessibleObjectException` on JDK 9+ (worked on JDK 8 for some types) | `IllegalArgumentException` at build time naming the type and `registerConverter`; `BigDecimal`, `BigInt`, `java.math.*`, `UUID`, `Instant`, `OffsetDateTime`, `ZonedDateTime` now have built-in formats | JDK 8 users serializing JDK types through reflection must register converters |
| **Classes extending JDK classes** | JDK superclass fields ignored | unchanged (JDK superclasses are skipped explicitly) | |

## Input (deserialize)

| Change | Before | Now | Who is affected |
|---|---|---|---|
| **`null` literal** | error for every known field | accepted only where declared: `None` for `Option` values, `null` for `@nullable` reference fields; everything else is still rejected (with a clearer message) under the default `NullPolicy.Fail`; `Omit` / `WriteNull` accept `null` for any reference type | code that relied on `null` being rejected for `@nullable` fields or `Option` values |
| **Absent `Option` field** | left as `null` | `None` (defaults set by a no-arg constructor are kept) | code testing `optionField == null`; `postDeserializationTransform` workarounds keep working |
| **Keys of inherited fields** (*fix*) | "No field" error (or skipped in lenient mode) | populated | |
| **`Foo.type` fields, mutable collections, nested/sealed typed subclasses, `LazyList`** (*fix*) | crash / unsupported | supported | |
| **Lenient readers** | | `Option` accepts an inline value, maps accept the `{"k":v}` object form, `java.time` types accept ISO-8601 strings, beans accept `{ }` with inner whitespace, time objects accept keys in any order | only more input is accepted |
| **Error types and messages** | `StringIndexOutOfBoundsException` on empty input; `NumberFormatException: For input string: ""` on a non-number; `InstantiationException` / `NullPointerException` for traits and abstract targets at first use; `ClassNotFoundException` for an unknown typed subclass | always `JsonInnerException` from `deserialize`, with `IllegalArgumentException` causes such as `Expected a number but found '"'`, `Cannot deserialize abstract type ...`, `Unknown type name ... known subclasses: ...`; integer overflow still `NumberFormatException` | code matching on the old exception types or messages |
| **Error message content** | the message repeated the whole input up to three times, plus the full text of long tokens | a window of about 80 characters around the failing position, its position and the input length; long tokens (keys, numbers, strings) are cut after 60 characters with their length | log parsers or tests matching the old `Json: ...` line or the full input |
| **Superclass converters** (*fix*) | when several registered supertypes matched, the winner depended on `Map` iteration order | the most specific supertype wins | |
| **Duration converter** (*fix*) | `registerConverter[Duration]` was ignored on read | honoured | |
| **Late registrations** (*fix*) | `registerConverter`, `registerSuperclassConverter`, `registerSerializerForAnySubtype`, `markTypedClass` and `postDeserializationTransform` were silently ignored for types already (de)serialized by the instance | they drop the cached (de)serializers and take effect immediately; static (de)serializers built earlier keep their behaviour | code that registered converters lazily |

## API

Source-compatible for Scala callers unless noted; downstream *binaries* compiled against 0.6.51 should be
recompiled.

| Member | Change |
|---|---|
| `MPJsons` constructor | primary is now `new MPJsons(config: MPJsonsConfig)`; `new MPJsons(ignoreNonExistingFields = false, ignoreNullFields = false, nullPolicy = NullPolicy.Fail)` is an auxiliary constructor with defaults. The 2-argument constructor no longer exists as a binary signature; Java callers must pass three arguments or a config. New: `mpjsons.config`. |
| `NullPolicy`, `MPJsonsConfig`, `jsonName`, `typeName` | new public types in `io.mpjsons` |
| `SerializerFactory` / `DeserializerFactory` | primary constructors take `MPJsonsConfig`; the `Boolean` constructors remain |
| `BeanSerializer`, `BeanDeserializer`, `OptionSerializer`, `OptionDeserializer`, `MapSerializer`, `TypedSerializer`, `TypedDeserializer` | gained new primary constructors (config, or the typed-name table); the previous overloads remain |
| `StringIterator` | 1-argument constructor remains (lenient); `nextCharOrNullIfLast` now returns `Unit` and always advances; new `seek`, `tryConsumeNull`, `afterValue`, `isAtEnd`, `expectEnd` |
| `AbstractStringDeserializer.readString` | returns `String` (was `StringBuilder`) |
| `AbstractIntegerDeserializer` / `AbstractFloatingPointDeserializer` | the protected `readNumberString` is replaced by `readLong` / `readInt` / `readShort` / `readByte` and `readDouble` / `readFloat`; the companion's `readNumberString` is replaced by `scanNumber` / `skipNumber` / `readDouble` / `readFloat`. **Custom deserializers extending these classes need updating.** |
| `FieldInfo`, `FieldWithTypeInfo` | gained a trailing field (`tpe` with a default; `jsonName`, with an auxiliary constructor for the old arity). Pattern matches with the old arity need updating. |
| `SimpleToStringSerializer` | kept but no longer used by the factories (primitives have dedicated serializers) |
| Dependency | `slf4j-api` is no longer a dependency of mpjsons; add it yourself if you relied on it transitively |

## Not changed

Default wire format of primitives, strings, collections, maps (pair arrays), `Option` (`{}` / `{"value":v}`),
`Either`, tuples, `java.time` objects, typed wrappers; lenient parsing (bare keys, missing and trailing commas,
trailing content); `ignoreNonExistingFields`; `postDeserializationTransform`; `markTypedClass`; custom converters.
All new behaviour beyond the fixes above is behind `MPJsonsConfig` levers that default to off.
