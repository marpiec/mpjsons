# CLAUDE.md

Guidance for working in this repository. This is a deep, factual map of the codebase intended to support **bug-hunting and performance work**.

**Detailed internals documentation lives in [`docs/`](docs/README.md)** (architecture, exact JSON wire format, verified known issues with repro and root cause, measured performance baseline and optimisation plan, roadmap for `null` support / field naming / compact output). Read `docs/README.md` first when starting non-trivial work; this file is the short map.

## What this is

`mpjsons` is a Scala object ⇄ JSON conversion library (`io.mpjsons`, version `0.6.51`). The selling point is a minimal API: `mpjsons.serialize[T](obj)` / `mpjsons.deserialize[T](json)` with no hand-written serializers required. It uses **Scala 2.13 runtime reflection** (`scala-reflect`, `scala.reflect.runtime.universe`) to inspect types and build per-type (de)serializers, which are then cached.

- Build tool: sbt `1.11.7` (`project/build.properties`), Scala `2.13.16`.
- Current branch is `scala_2.13`; main/PR branch is `master`.
- Runtime deps: `scala-reflect`, `slf4j-api`. Test: `scalatest 3.2.15` (FlatSpec + MustMatchers).

## Build & test commands

```bash
sbt compile
sbt test                              # run all specs
sbt "testOnly io.mpjsons.PerformanceSpec"   # single spec
sbt "testOnly io.mpjsons.TimeSpec"
```

Tests live in `src/test/scala/io/mpjsons/`, one spec per feature area (Collections, Maps, Tuples, Either, Option, Time, MalformedJson, SkipFields, InfiniteDepth, ScientificNumberNotation, etc.). `PerformanceSpec` asserts a hard wall-clock budget (`< 500ms` for 5000 serialize+deserialize round-trips of a small object) — **it is timing-sensitive and machine-dependent**; treat failures there as a signal, not necessarily a regression.

## Architecture & request flow

### Entry point: `MPJsons.scala`
`class MPJsons(ignoreNonExistingFields = false, ignoreNullFields = false)` is the public facade. It owns one `SerializerFactory` and one `DeserializerFactory`. Key public methods:
- `serialize[T](obj)` / `deserialize[T](json)` — implicit `TypeTag[T]` drives type resolution. Guards against `Nothing` (unspecified type param).
- `buildStaticSerializer[T]` / `buildStaticDeserializer[T]` — return reusable `StaticSerializer`/`StaticDeserializer` bound to one type (avoids repeated factory lookup; what `PerformanceSpec` uses).
- `registerConverter`, `registerSuperclassConverter`, `registerSerializerForAnySubtype` — custom (de)serializers.
- `markTypedClass[T]` / `markTypedSuperClass[T]` — emit/parse a type tag wrapper `{"TypeName":{...}}` so collections of subtypes round-trip (see `TypedConverter`).
- `postDeserializationTransform[T](f)` — post-process the deserialized object.
- `@nullable` annotation (`final class nullable extends StaticAnnotation`) — marks a bean field allowed to be null (skipped instead of throwing on serialize).

`MPJsons` also keeps two `var` caches: `extractTypeCache` (TypeTag→Type) and `extractTypeFromNameCache` (class name→Type).

### Factory layering (per direction)
There is parallel structure for serialize and deserialize:

```
DeserializerFactory            ← thin concrete class
  extends DeserializerFactoryMemoizer   ← caching layer (getDeserializerCache)
    extends DeserializerFactoryImpl     ← the big type-dispatch if/else chain
```
(`SerializerFactory` → `SerializerFactoryMemoizer` → `SerializerFactoryImpl` mirror this.)

- **`*FactoryImpl`**: `getSerializerNoCache` / `getDeserializerNoCache` is a long `if/else` matching `tpe.typeSymbol` against known types (primitives, java boxed, java.time, Array, Option, Either, tuples 1–6, Lists/Vectors/Seqs/Sets/Maps and their Hash/List/Sorted/Tree variants, BitSet), then custom/superclass converters, then falls back to `SingletonObjectDeserializer` (detected via a `MODULE$` field) or `BeanSerializer`/`BeanDeserializer`.
- **`*FactoryMemoizer`**: wraps the above with `getDeserializerCache: Map[(Type, Boolean), …]`. It also resolves type parameters: if `orgtpe.typeSymbol.isParameter`, it looks the concrete type up in `context.typeParams`; then builds a `newContext` pushing the type onto `typesStack` and zipping `typeParams` with `typeArgs`.

### `Context` (`util/Context.scala`)
`case class Context(typesStack: List[Type], typeParams: Map[Symbol, Type])`. Carries the resolution stack (for error messages, `typesStackMessage`) and the generic-parameter bindings threaded down into nested element (de)serializers. Always created at the root as `Context(List.empty, Map.empty)`.

### Reflection (`util/reflection/`)
- `ReflectionUtilNoCache.getAllAccessibleFields(tpe)` — walks `tpe.baseClasses` (own class first), takes each class's non-method, non-synthetic term `decls` in reverse declaration order, resolves each to a `java.lang.reflect.Field` (walking Java superclasses via `getAccessibleField`, calling `setAccessible(true)`), types it with `typeSignatureIn(tpe)` (generic superclasses resolved), and records `nullable` by scanning annotations for the substring `"nullable"`. Returns `Array[FieldWithTypeInfo]`. Key order: own fields (reverse declaration order), then superclass fields.
- `ReflectionUtil` — memoizes `getAllAccessibleFields` in a `var Map[Type, Array[…]]`.
- `TypesUtil` — memoized `Type`↔`Class` conversions and `typeArgs` extraction. Uses `runtimeMirror(...)` per lookup on cache miss.

### Bean (de)serialization
- `BeanSerializer`: iterates `fieldsWithSerializers` (lazy, to survive recursive types), reads each field via reflection, and writes `"name":value`. Skips a `null` field only if `ignoreNullFields`/`@nullable`; otherwise throws. A `null` literal is never written. JSON key order is **reverse declaration order** (verified; `tpe.members.sorted` would give declaration order).
- `BeanDeserializer`: caches the no-arg `Constructor` if present. `deserialize` reads `{`, builds an instance via `ObjectConstructionUtil`, then loops fields keyed by `fieldsByName: Map[String,(Field, deserializer)]`, mutating the instance via `field.set`. Unknown fields → throw, unless `ignoreNonExistingFields`, in which case `skipValue` walks past the value (`skipObject`/`skipArray`/`skipString`/`skipNumber`/`skipBooleanOrNull`).
- `ObjectConstructionUtil`: uses the no-arg constructor if available; otherwise allocates via `sun.misc.Unsafe.allocateInstance` (**no constructor runs** — field defaults from the class are not applied; commented-out `ReflectionFactory` alternative is kept deliberately). Beans are therefore mutated in place, which is why deserialization targets `var`/mutable beans and case classes with no-arg-constructible shape.

### Low-level parsing: `StringIterator`
The single mutable cursor over the input JSON string. `currentChar`, `nextIndex`, `nextChar()`, `skipWhitespaceChars()`, and `consume*` helpers (`consumeObjectStart`/`End`, `consumeArrayStart`/`End`, `consumeFieldValueSeparator`, `consumeArrayValuesSeparator`) that throw `IllegalArgumentException` on mismatch. End-of-input: the first `nextChar()` past the end sets `currentChar = '\u0000'`; a further call throws `"Unexpected end of Json"`. The constructor reads `charAt(0)`, so empty input throws an unwrapped `StringIndexOutOfBoundsException`. All deserializers share and advance this one cursor.

- `AbstractStringDeserializer.readString` handles escape sequences including `\uXXXX`.
- `AbstractIntegerDeserializer` / `AbstractFloatingPointDeserializer` accumulate the numeric substring then delegate to `.toInt/.toLong/.toDouble` etc. `isCurrentCharAFloatingPointPart` accepts `eE+-.` (scientific notation).
- `IdentifierDeserializer` reads object keys, quoted or unquoted.

### Maps
Maps serialize as a **JSON array of `[key,value]` pairs** (`MapSerializer` / `AbstractJsonMapDeserializer`), not as JSON objects — this is intentional so arbitrary key types work. `SimpleMapSerializer`/`SimpleMapDeserializer` exist for the string-keyed object form.

### Time types
`java.time.LocalDate/LocalTime/LocalDateTime/Duration` have dedicated (de)serializers under `*/time/`. `Duration` serializes as `{"seconds":…,"nanos":…}` (see `DurationDeserializer`). The most recent functional commit was `0974d5b Fixed negative duration deserialization`.

## Concurrency / state model (important for bug-hunting)

Caches throughout are **`var` fields holding immutable `Map`s, mutated with `+=`** (not synchronized, not `concurrent.Map`):
- `MPJsons.extractTypeCache`, `extractTypeFromNameCache`
- `DeserializerFactoryMemoizer.getDeserializerCache`, `SerializerFactoryMemoizer.getSerializerCache`
- `DeserializerFactoryImpl.additionalDeserializers` / `additionalSuperclassDeserializers` / `postTransform`, and serializer equivalents
- `ReflectionUtil.getAllAccessibleFieldsCache`, all four caches in `TypesUtil`
- `TypedConverter.TypedSerializer.serializers` / `TypedDeserializer.deserializers`

This means a single `MPJsons` instance is **not safe for concurrent first-time use of new types** (lost cache updates, racy reads). Once all types are warmed it is effectively read-mostly. Also note Scala 2.13 `scala.reflect.runtime` was historically not thread-safe — relevant if reviewing multi-threaded usage. Treat thread-safety as an open question to confirm against the user's deployment, not an established guarantee.

## Performance-sensitive hot paths

When evaluating perf changes, these are the paths exercised per call (after warm-up the factory lookups are cached, so the real cost is the inner loops):
- `BeanSerializer.serialize` — per-field reflective `field.get` + `StringBuilder.append`. Field array is iterated every call.
- `BeanDeserializer.deserialize` — per-field `fieldsByName.get(identifier)` (HashMap lookup) + reflective `field.set`. New `StringBuilder`s allocated per identifier/string.
- `StringIterator` — char-by-char; `skipWhitespaceChars` and `isWhitespace` called frequently.
- `StringSerializer` — per-char `match`; allocates short strings for control-char hex escapes.
- Numeric deserializers allocate a `StringBuilder`/`String` then parse — a known allocation hotspot.

**History worth knowing:** commits `6442f3c`/`2bfcbb2` ("Optimization by claude") rewrote `DeserializerFactoryImpl`/`SerializerFactoryImpl` and were **both reverted** (`53ada5e`, `a174a40`). The dispatch chains are intentionally back to their original form. Those chains are **cold path** (run once per type and then cached), so rewriting them cannot change steady-state throughput; real wins are in the per-value loops listed above. Measured baseline and a prioritised plan are in `docs/performance.md`.

## Conventions

- One (de)serializer class per type/type-family, organized under `impl/serializer/…` and `impl/deserializer/…` with subpackages `immutables/`, `mutables/`, `values/`, `jsontypes/`, `utiltypes/`, `time/`, `common/`.
- Serializers extend `JsonTypeSerializer[T]` (`serialize(obj, jsonBuilder: StringBuilder): Unit`); deserializers extend `JsonTypeDeserializer[T]` (`deserialize(jsonIterator): T`). Both are tiny single-method traits.
- Element/key/value sub-serializers are resolved **once in the constructor (or a `lazy val` for recursive types)** by calling back into the factory with the threaded `context` — not per element.
- Errors are `IllegalArgumentException`/`IllegalStateException`/`RuntimeException`, wrapped at the boundary in `JsonInnerException` with a pointer-annotated message (`ErrorMessageFormatter`).
- Adding support for a new type = add a (de)serializer class **and** wire it into both `*FactoryImpl` dispatch chains (order matters — more specific symbols must precede `baseClasses.contains` fallbacks).

## Known sharp edges (candidate areas to scrutinize)

Confirmed bugs with repro and root cause are tracked in `docs/known-issues.md` (notably: generic type-parameter bindings leak between instantiations through the memoizer cache key; inherited fields are dropped; `X.type` fields crash on deserialize; `null` literal unsupported). The list below is the older set of observations from reading the code:
- `Unsafe.allocateInstance` skips constructors, so non-`var` defaults and validation in beans are bypassed on deserialize; also `sun.misc.Unsafe` availability varies by JVM.
- Unsynchronized `var Map` caches under concurrency (see above).
- `SortedSet`/`TreeSet`/`SortedMap`/`TreeMap` deliberately throw (`IllegalStateException`, missing `Ordering`). `ListBuffer` throws by design.
- Map serialized form is a pair-array, so it is **not interoperable** with standard `{"k":"v"}` JSON unless `SimpleMap*` is used.
- `nullable` detection is a case-insensitive substring match on annotation `toString` — broad by design.
- Number parsing relies on JVM `String.toX`; malformed/overflowing input surfaces as the JVM's `NumberFormatException` wrapped in `JsonInnerException`.

When you find a real bug or a measured perf win, prefer a focused change to the relevant (de)serializer or `StringIterator` and add/extend the matching spec in `src/test/scala/io/mpjsons/`.