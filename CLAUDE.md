# CLAUDE.md

Guidance for working in this repository. This is a deep, factual map of the codebase intended to support **bug-hunting and performance work**.

**Detailed internals documentation lives in [`docs/`](docs/README.md)** (architecture, exact JSON wire format, verified known issues with repro and root cause, measured performance baseline and optimisation plan, roadmap for `null` support / field naming / compact output). Read `docs/README.md` first when starting non-trivial work; this file is the short map.

## What this is

`mpjsons` is a Scala object ⇄ JSON conversion library (`io.mpjsons`, version `0.6.51`). The selling point is a minimal API: `mpjsons.serialize[T](obj)` / `mpjsons.deserialize[T](json)` with no hand-written serializers required. It uses **Scala 2.13 runtime reflection** (`scala-reflect`, `scala.reflect.runtime.universe`) to inspect types and build per-type (de)serializers, which are then cached.

- Build tool: sbt `1.11.7` (`project/build.properties`), Scala `2.13.16`.
- Current branch is `scala_2.13`; main/PR branch is `master`.
- Runtime deps: `scala-reflect` only. Test: `scalatest 3.2.15` (FlatSpec + MustMatchers). Benchmarks: `sbt-jmh` (`src/jmh`).

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
`class MPJsons(config: MPJsonsConfig)` is the public facade; the historical constructor `new MPJsons(ignoreNonExistingFields = false, ignoreNullFields = false, nullPolicy = NullPolicy.Fail)` maps onto it (`ignoreNullFields = true` means `NullPolicy.Omit`). `MPJsonsConfig` also carries the opt-in format levers `escapeSlash`, `compactOptions`, `mapsAsObjects`, `isoTime` (defaults reproduce the 0.6.x format; readers accept both forms wherever distinguishable, see `docs/json-format.md`). Format decisions are taken when (de)serializers are built, never per call. Nulls are accepted only where declared, both ways: under the default `NullPolicy.Fail` a JSON `null` reads as `None` for `Option` and as `null` only for `@nullable` reference fields, and on write only null `@nullable` fields are tolerated (omitted); every other null (fields, elements, map keys/values, tuple slots, `Some`, top level) is an error. `Omit` / `WriteNull` tolerate nulls for all reference types in both directions (`impl/util/NullValues.scala`, `StringIterator.tryConsumeNull`). Absent `Option` fields read as `None`. It owns one `SerializerFactory` and one `DeserializerFactory`. Key public methods:
- `serialize[T](obj)` / `deserialize[T](json)` — implicit `TypeTag[T]` drives type resolution. Guards against `Nothing` (unspecified type param).
- `buildStaticSerializer[T]` / `buildStaticDeserializer[T]` — return reusable `StaticSerializer`/`StaticDeserializer` bound to one type (avoids repeated factory lookup; what `PerformanceSpec` uses).
- `registerConverter`, `registerSuperclassConverter`, `registerSerializerForAnySubtype` — custom (de)serializers.
- `markTypedClass[T]` / `markTypedSuperClass[T]` — emit/parse a type tag wrapper `{"TypeName":{...}}` so collections of subtypes round-trip (see `TypedConverter`). `@typeName("x")` on a subclass of a sealed typed class replaces the simple name in the wrapper (simple name still accepted on read; see `TypeNames`).
- `postDeserializationTransform[T](f)` — post-process the deserialized object.
- `@nullable` annotation (`final class nullable extends StaticAnnotation`) — marks a bean field allowed to be null (skipped instead of throwing on serialize). `@jsonName("key")` (`jsonName.scala`) overrides the JSON key; on read the Scala field name is still accepted as a fallback unless another field owns it as its JSON name. Both are found on the field, the constructor parameter or the getter, so case class parameters need no `@field` meta-annotation.

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
- `ReflectionUtilNoCache.getAllAccessibleFields(tpe)` — walks `tpe.baseClasses`, takes each class's non-method, non-synthetic term `decls`, resolves each to a `java.lang.reflect.Field` (walking Java superclasses via `getAccessibleField`, calling `setAccessible(true)`), types it with `typeSignatureIn(tpe)` (generic superclasses resolved), and records `nullable` by scanning annotations for the substring `"nullable"`. Returns `Array[FieldWithTypeInfo]` ordered superclass fields first, declaration order within a class.
- `ReflectionUtil` — memoizes `getAllAccessibleFields` in a `var Map[Type, Array[…]]`.
- `TypesUtil` — memoized `Type`↔`Class` conversions and `typeArgs` extraction. Uses `runtimeMirror(...)` per lookup on cache miss.

### Bean (de)serialization
- `BeanSerializer`: iterates `fieldsWithSerializers` (lazy, to survive recursive types), reads each field via reflection, and writes `"name":value`. Skips a `null` field only if `ignoreNullFields`/`@nullable`; otherwise throws. Null reference fields follow the instance `NullPolicy` (`Fail` default / `Omit` / `WriteNull`, `io.mpjsons.NullPolicy`); null elements of collections, maps, tuples, `Either`, `Some` throw under `Fail` and are written as `null` otherwise. JSON key order is declaration order, superclass fields first.
- `BeanDeserializer`: caches the no-arg `Constructor` if present. `deserialize` reads `{`, builds an instance via `ObjectConstructionUtil`, then loops fields keyed by `fieldsByName: Map[String,(Field, deserializer)]`, mutating the instance via `field.set`. Unknown fields → throw, unless `ignoreNonExistingFields`, in which case `skipValue` walks past the value (`skipObject`/`skipArray`/`skipString`/`skipNumber`/`skipBooleanOrNull`).
- `ObjectConstructionUtil`: uses the no-arg constructor if available; otherwise allocates via `sun.misc.Unsafe.allocateInstance` (**no constructor runs** — field defaults from the class are not applied; commented-out `ReflectionFactory` alternative is kept deliberately). Beans are therefore mutated in place, which is why deserialization targets `var`/mutable beans and case classes with no-arg-constructible shape.

### Low-level parsing: `StringIterator`
The single mutable cursor over the input JSON string. `currentChar`, `nextIndex`, `nextChar()`, `skipWhitespaceChars()`, and `consume*` helpers (`consumeObjectStart`/`End`, `consumeArrayStart`/`End`, `consumeFieldValueSeparator`, `consumeArrayValuesSeparator`) that throw `IllegalArgumentException` on mismatch. End-of-input: the first `nextChar()` past the end sets `currentChar = '\u0000'`; a further call throws `"Unexpected end of Json"`. Empty input starts at the end marker, so it fails inside the normal error wrapping. All deserializers share and advance this one cursor. Element loops end each member with `afterValue(closing)`, which enforces commas when the iterator was created with `strict = true` (`MPJsonsConfig.strict`); `expectEnd()` rejects trailing content in that mode.

- `AbstractStringDeserializer.readString` handles escape sequences including `\uXXXX`.
- `AbstractIntegerDeserializer` / `AbstractFloatingPointDeserializer` accumulate the numeric substring then delegate to `.toInt/.toLong/.toDouble` etc. `isCurrentCharAFloatingPointPart` accepts `eE+-.` (scientific notation).
- `IdentifierDeserializer` reads object keys, quoted or unquoted.

### Maps
Maps serialize as a **JSON array of `[key,value]` pairs** (`MapSerializer` / `AbstractJsonMapDeserializer`), not as JSON objects — this is intentional so arbitrary key types work. `SimpleMapSerializer`/`SimpleMapDeserializer` exist for the string-keyed object form.

### Time types
`java.time.LocalDate/LocalTime/LocalDateTime/Duration` have dedicated (de)serializers under `*/time/`. `Duration` serializes as `{"seconds":…,"nanos":…}` (see `DurationDeserializer`). The most recent functional commit was `0974d5b Fixed negative duration deserialization`.

## Concurrency / state model (important for bug-hunting)

Caches are `ConcurrentHashMap`s filled with get-then-`putIfAbsent` (never `computeIfAbsent`: building a (de)serializer re-enters the factory for element types):
- `MPJsons.extractTypeFromNameCache` (`extractType` is plain `tag.tpe`)
- the two maps in `DeserializerFactoryMemoizer` / `SerializerFactoryMemoizer` (keyed by the fully substituted `Type`)
- `ReflectionUtil.getAllAccessibleFieldsCache`, the three caches in `TypesUtil`
- `TypedConverter.TypedSerializer.serializers` / `TypedDeserializer.deserializers`

Still plain `var Map`s, written only at registration time: `DeserializerFactoryImpl.additionalDeserializers` / `additionalSuperclassDeserializers` / `postTransform` and the serializer equivalents. Register converters before sharing an instance across threads. Each registration clears the memoizer caches (`registrationsChanged()`), so late registrations take effect; static (de)serializers built earlier do not change. `ConcurrencySpec` covers cold first-time use from 8 threads. Scala runtime reflection is thread-safe (lock-based) since 2.11.

## Performance-sensitive hot paths

When evaluating perf changes, these are the paths exercised per call (after warm-up the factory lookups are cached, so the real cost is the inner loops). All of them are now allocation-conscious; measure with the JMH module (`sbt "Jmh/run -f1 -wi 3 -w 1 -i 5 -r 1 -prof gc .*MpjsonsBenchmark.*"`) before and after any change:
- `BeanSerializer.serialize` — one thread-local depth counter per bean for cycle detection (identity tracking only beyond 100 nested beans), then a `while` loop over per-field `BeanFieldWriter`s: typed `Field.getInt/getLong/...` + direct primitive append, precomputed `"name":` prefixes; reference fields delegate to the field's serializer.
- `BeanDeserializer.deserialize` — `BeanFieldTable.find` matches keys in place on the raw input (no `String` per key), then typed `BeanField.set` (`Field.setInt` etc.).
- `StringIterator` — readers scan the raw `stringValue` with a local index and `seek` back; `skipWhitespaceChars` is still called at every separator.
- `StringSerializer` / `readString` — bulk runs, escape table, single `substring` when no escapes.
- Numbers and booleans are parsed directly from the input chars; collections are built with their builders; the output `StringBuilder` is pre-sized from the previous output length per type.

**History worth knowing:** commits `6442f3c`/`2bfcbb2` ("Optimization by claude") rewrote `DeserializerFactoryImpl`/`SerializerFactoryImpl` and were **both reverted** (`53ada5e`, `a174a40`). The dispatch chains are intentionally back to their original form. Those chains are **cold path** (run once per type and then cached), so rewriting them cannot change steady-state throughput; real wins are in the per-value loops listed above. Measured baseline and a prioritised plan are in `docs/performance.md`.

## Conventions

- One (de)serializer class per type/type-family, organized under `impl/serializer/…` and `impl/deserializer/…` with subpackages `immutables/`, `mutables/`, `values/`, `jsontypes/`, `utiltypes/`, `time/`, `common/`.
- Serializers extend `JsonTypeSerializer[T]` (`serialize(obj, jsonBuilder: StringBuilder): Unit`); deserializers extend `JsonTypeDeserializer[T]` (`deserialize(jsonIterator): T`). Both are tiny single-method traits.
- Element/key/value sub-serializers are resolved **once in the constructor (or a `lazy val` for recursive types)** by calling back into the factory with the threaded `context` — not per element.
- Errors are `IllegalArgumentException`/`IllegalStateException`/`RuntimeException`, wrapped at the boundary in `JsonInnerException` with a pointer-annotated message (`ErrorMessageFormatter`). Never put the whole input into an exception message: the formatter shows a short window around the failing position, and quoted tokens go through `Messages.abbreviate`.
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