# Architecture: how mpjsons works

`mpjsons` converts Scala objects to and from JSON using **Scala 2.13 runtime reflection**
(`scala.reflect.runtime.universe`) to discover types, plus **Java reflection** (`java.lang.reflect.Field`)
to read and write fields. For every `Type` it builds one (de)serializer object, caches it, and from
then on each call is a tree walk over those cached objects. There is no intermediate JSON tree
(no AST, no token stream): serializers append directly to a `StringBuilder`, deserializers pull
characters directly from a `StringIterator`.

Runtime dependency: `scala-reflect` only (the unused `slf4j-api` dependency was removed).

## 1. Public API surface (`src/main/scala/io/mpjsons/MPJsons.scala`)

| Member | Notes |
|--------|-------|
| `class MPJsons(config: MPJsonsConfig)`; `new MPJsons(ignoreNonExistingFields = false, ignoreNullFields = false, nullPolicy = NullPolicy.Fail)` | One instance owns one `SerializerFactory` and one `DeserializerFactory` (both receive the config) and all caches. `MPJsonsConfig` holds `ignoreNonExistingFields`, `nullPolicy`, and the opt-in format levers `escapeSlash`, `compactOptions`, `mapsAsObjects`, `isoTime` (json-format.md). |
| `serialize[T](obj)(implicit TypeTag[T]): String` | Resolves `Type` from the tag, looks up the serializer, writes into a fresh `scala.collection.mutable.StringBuilder`, returns `toString`. |
| `serialize(obj, tpe: Type)` / `serialize(obj, typeName: String)` | Same, with an explicit `Type` or a class name resolved via `Class.forName`. |
| `deserialize[T](json)(implicit TypeTag[T]): T`, `deserialize(json, tpe)`, `deserialize(jsonIterator, tpe)`, `deserialize(json, typeName)` | Wrap any `RuntimeException` into `JsonInnerException` whose message shows the failing position, the input length and a short window of the input with a caret (`ErrorMessageFormatter`); the whole input is never copied into the message. |
| `buildStaticSerializer[T]` / `buildStaticDeserializer[T]` | Return `StaticSerializer` / `StaticDeserializer` bound to one `JsonTypeSerializer`/`Deserializer`; skip the per-call factory lookup. `StaticSerializer.serializeToStringBuilder` avoids the final `toString` copy. |
| `registerConverter[T](sf => ser, df => deser)` | Exact-type custom converter. Keyed by `tpe.toString` (`SerializerFactoryImpl.scala:25`, `DeserializerFactoryImpl.scala:34`). |
| `registerSuperclassConverter[T]` | For `T` and subtypes. Keyed by `Symbol`, matched via `tpe.baseClasses.contains`. |
| `registerSerializerForAnySubtype[T]` | Serialize-only; keyed by the type's string with `[...]` stripped (regex `\[.*\]`), consulted last (`SerializerFactoryImpl.scala:190`). |
| `markTypedClass[T]` / `markTypedSuperClass[T]` | Register `TypedConverter` for `T` (wrapper `{"SimpleName":{...}}`). |
| `postDeserializationTransform[T](f: T => T)` | Wraps the deserializer of exactly `T` in `PostTransformDeserializer`. |
| `@nullable` (`final class nullable extends StaticAnnotation`) | Field may be `null` on serialize (omitted under `NullPolicy.Fail`). Found on the field, the constructor parameter of the same name or the getter, so no `@field` meta-annotation is needed. |
| `@jsonName("key")` (`jsonName.scala`) | Overrides the JSON key of a field; same lookup places as `@nullable`; duplicates within a type fail at build time. |

The traits user code implements for custom converters (`JsonTypeSerializer[T]`, `JsonTypeDeserializer[T]`) and
the classes those implementations touch (`StringIterator`, `SerializerFactory`, `DeserializerFactory`, `Context`)
are all **public API**. The test suite itself contains such converters (`CustomConverterSupportSpec`,
`SimpleMapSpec`) that call `consumeObjectStart`, `skipWhitespaceChars`, `nextChar`, `currentChar`,
`getDeserializer(tpe, Context(...))`. Internal refactors must keep those members working.

## 2. End-to-end flow

### serialize

```
MPJsons.serialize[T](obj)(tag)
  ├─ guard: tag == typeTag[Nothing] → IllegalArgumentException
  ├─ extractType(tag): Map[TypeTag[_], Type] lookup   (ineffective, see performance.md §3)
  └─ serialize(obj, tpe)
       ├─ val json = new StringBuilder()
       ├─ serializerFactory.getSerializer(tpe, Context(Nil, Map.empty))      ← SerializerFactoryMemoizer
       │     ├─ if tpe.typeSymbol.isParameter → tpe = context.typeParams(sym)
       │     ├─ newContext = Context(tpe :: stack, typeParams ++ (tpe's own params zip tpe.typeArgs))
       │     └─ getSerializerCache.getOrElse((tpe, allowSuperType), build via getSerializerNoCache(...))
       ├─ serializer.serialize(obj, json)                                      ← tree of JsonTypeSerializer
       └─ json.toString()
```

### deserialize

```
MPJsons.deserialize[T](json)(tag)
  ├─ guard Nothing; extractType(tag)
  └─ deserialize(json, tpe)
       ├─ val it = new StringIterator(json)        ← throws StringIndexOutOfBounds on "" (outside the try!)
       └─ try deserializerFactory.getDeserializer(tpe, Context(...)).deserialize(it)
          catch RuntimeException → JsonInnerException(formatted message with ^ pointer)
```

Sub-(de)serializers for element/field types are resolved **once, in the constructor** of the parent
(de)serializer (or in a `lazy val` for beans, to survive recursive types), by calling back into the
factory with the threaded `Context`. At steady state no factory code runs inside the per-value loops,
except the root lookup described above.

## 3. Factory layering

```
DeserializerFactory (impl/DeserializerFactory.scala, empty concrete class)
  extends DeserializerFactoryMemoizer     (factoryimpl/DeserializerFactoryMemoizer.scala)  — cache + type-param resolution
    extends DeserializerFactoryImpl       (factoryimpl/DeserializerFactoryImpl.scala)      — dispatch chain + converter registries
```
`SerializerFactory` → `SerializerFactoryMemoizer` → `SerializerFactoryImpl` mirror this.

### 3.1 Memoizer (`DeserializerFactoryMemoizer.scala:18-34`, `SerializerFactoryMemoizer.scala:17-34`)

```scala
val tpe = TypeParamsResolver.resolve(orgtpe, context)      // List[T] -> List[Int] using context.typeParams
val map = if (allowSuperType) cache else cacheWithoutSuperTypes   // two ConcurrentHashMap[Type, ...]
val cached = map.get(tpe); if (cached != null) return cached
val newContext = Context(tpe :: context.typesStack, context.typeParams ++ (tpe's own params zip tpe.typeArgs))
val created = build(tpe, newContext, allowSuperType); map.putIfAbsent(tpe, created)   // first stored wins
```

Facts that matter:

1. The key is the **fully substituted** type, so different instantiations of a generic bean never share
   element (de)serializers (`GenericTypeParamsSpec`). An unbound parameter is an error.
2. A cache hit allocates nothing; `Context` is only built on a miss.
3. `putIfAbsent` rather than `computeIfAbsent`: building a (de)serializer re-enters the factory for its
   element types, which `computeIfAbsent` forbids. Under contention two threads may build the same
   (de)serializer; the first stored wins and both are equivalent.
4. `allowSuperType = false` is used only by `TypedConverter` to avoid re-entering the superclass converter
   for the concrete subtype (that was the "Cache fixed" commit `148f457`).

### 3.2 Dispatch chains (cold path, run once per `(Type, Boolean)`)

`DeserializerFactoryImpl.getPureDeserializerNoCache` (`:49-203`), in order:

1. Scala primitives by `typeSymbol` equality: Long, Int, Boolean, String, Double, Float, Short, Byte, Char.
2. Java boxed: `java.lang.Long/Integer/Boolean/Double/Float/Short/Byte` (mapped to the *same* Scala deserializer objects; the `Java*Deserializer` objects in `values/` exist but are unused).
3. `java.time.LocalTime/LocalDate/LocalDateTime/Duration` unless a converter is registered for that `tpe.toString`; `java.lang.Character`; then (same converter guard) `BigDecimal`, `java.math.BigDecimal`, `BigInt`, `java.math.BigInteger`, `java.util.UUID`, `java.time.Instant/OffsetDateTime/ZonedDateTime`.
4. `Array` via `tpe.asInstanceOf[TypeRef].sym == definitions.ArrayClass` (**unguarded cast**, crashes for `SingleType` such as `Foo.type`, known-issues §3).
5. `Option`, `Map`/`immutable.Map`, `Either`.
6. `Tuple1..Tuple6`.
7. Seqs: `List`, `Vector`, `Iterable`, `Seq`, `Stream`, `LazyList`, `immutable.Queue` (each matched for both the `scala.collection` and `immutable` symbol where both exist).
8. Sets: `Set`, `immutable.HashSet`, `immutable.ListSet`; `SortedSet`/`TreeSet` → `IllegalStateException`; `BitSet`.
9. Maps: `immutable.HashMap`; `SortedMap`/`TreeMap` → throw; `immutable.ListMap`.
10. Mutable collections: `mutable.ArrayBuffer/Buffer/Seq/IndexedSeq/Iterable` → `ArrayBufferDeserializer`; `mutable.Set/HashSet` → `MutableHashSetDeserializer`; `mutable.Map/HashMap` → `MutableHashMapDeserializer`.
11. If `allowSuperType`: registered superclass deserializers (exact symbol, then `baseClasses.contains` over the registry map in map-iteration order).
12. `ListBuffer` → `IllegalArgumentException`.
13. Exact-type custom deserializer by `tpe.toString`.
14. `Nothing` / `Any` / `AnyRef` → `IllegalArgumentException`.
15. Singleton object (`typeSymbol.isModuleClass`) → `SingletonObjectDeserializer`.
16. Any other `java.*` type → `IllegalArgumentException` ("use registerConverter"); abstract type (trait / abstract class, `typeSymbol.isAbstract`) → `IllegalArgumentException` naming the type and the registration options.
17. Otherwise `BeanDeserializer`.

Then `getDeserializerNoCache` wraps the result in `PostTransformDeserializer` if a transform is registered for exactly `tpe`.

`SerializerFactoryImpl.getSerializerNoCache` (`:40-199`):

1. Scala and Java numeric/boolean primitives → `SimpleToStringSerializer` (`append(obj: Any)`).
2. `java.time` four types (all four guarded by "no converter registered"; ISO string variants when `isoTime`); then `BigDecimal`/`BigInt` (Scala and `java.math`) → number serializers and `UUID`/`Instant`/`OffsetDateTime`/`ZonedDateTime` → `ToStringAsJsonStringSerializer`, same guard.
3. `String`, `mutable.StringBuilder`, `Char`, `java.lang.Character` → `StringSerializer` (or its no-slash-escape variant).
3b. Final fallback: a `java.*` type without a converter fails with "use registerConverter" instead of reaching `BeanSerializer`.
4. `Array` (guarded with `isInstanceOf[TypeRefApi]`).
5. **Only if** `typeSymbol.fullName.startsWith("scala.")`: any mutable `Seq/Set/Iterable` → `IterableSerializer`, mutable `Map` → `MapSerializer`; any `collection.Seq`, `BitSet` (exact), `collection.Set`, `collection.Map`, `Iterable` likewise; then `Either`, `Option`, `Tuple1..6` by `baseClasses.contains`. Serialization therefore accepts every standard collection, while deserialization supports the explicit list in the chain above.
6. Superclass serializers (if `allowSuperType`), exact custom serializer by `tpe.toString`, `Nothing/Any/AnyRef` errors, singleton (`typeSymbol.isModuleClass`) → `SingletonObjectSerializer`.
7. `registerSerializerForAnySubtype` registry by stripped type string, else `BeanSerializer(..., errorOnNulls = !ignoreNullFields)`.

Adding a type means wiring both chains; order matters because later `baseClasses.contains` checks are broad.

## 4. `Context` (`impl/util/Context.scala`)

`case class Context(typesStack: List[Type], typeParams: Map[Symbol, Type])`. `typesStack` exists only
for error messages (`typesStackMessage`). `typeParams` maps a type-parameter `Symbol` (e.g. the `T` of
`Box[T]`) to the concrete `Type` seen at the use site, and is extended at every memoizer call. Root calls
always start with `Context(List.empty, Map.empty)`.

## 5. Reflection layer (`impl/util/reflection/`, `impl/util/TypesUtil.scala`)

`ReflectionUtilNoCache.getAllAccessibleFields(tpe)`:

```scala
for ((baseClass, classIndex) <- tpe.baseClasses.zipWithIndex)   // linearization: the class itself first, then supertypes
  for ((m, declIndex) <- tpe.baseType(baseClass).decls.toList.zipWithIndex   // declaration order
         if m.isTerm && !m.isMethod && !m.isSynthetic && !m.isModule; if seenNames.add(name))
    collected += ((classCount - classIndex, declIndex,
      FieldWithTypeInfo(getAccessibleField(clazz, name),    // Java Field (walks superclasses) or null → skipped
                        m.typeSignatureIn(tpe),             // field type as seen from tpe (generic bases resolved)
                        m.annotations.exists(_.toString.toLowerCase.contains("nullable")))))
collected.sortBy(e => (e._1, e._2))   // least derived class first, declaration order within a class
```

Properties of this scan:

- Key order is **declaration order**, superclass fields first (`Ordered(zeta, alpha, mid, beta)` serializes as `{"zeta":..,"alpha":..,"mid":..,"beta":..}`). Up to 0.6.51 the order was reversed (an artefact of `Type.members`).
- A field name declared in both a class and its superclass is taken once, from the most-derived class.
- Annotations are gathered from the field symbol, the declaring class's primary-constructor parameter of the same name and the getter (`tpe.member(TermName(name))`), because an annotation on a case class parameter stays on the parameter unless a `@field` meta-annotation is used. `@jsonName` is read from the annotation tree's literal argument; `nullable` is a substring match on the annotation's `toString`.
- Local field names carry a trailing space in Scala reflection, hence `.trim`.
- Non-field members that survive the filter (nested classes, type members, modules) resolve to `null` via `NoSuchFieldException` and are filtered out; cold-path cost only.
- `getAccessibleField` walks `clazz.getSuperclass` until it equals `Object`; for an interface/trait `getSuperclass` is `null` → NPE (seen when the target type is `mutable.Map`).
- `setAccessible(true)` is called on every field; under JPMS this throws `InaccessibleObjectException` for `java.*` classes (e.g. `BigDecimal`, `UUID` fall into `BeanSerializer` and fail).
- `nullable` detection is a substring match on the annotation's `toString`, so any annotation whose name contains "nullable" (any package, any case) qualifies.

`ReflectionUtil` memoizes `getAllAccessibleFields` per `Type` in a `var Map`. `TypesUtil` memoizes
`typeArgs` extraction and `Type`↔`Class` conversions; `getClassFromType` uses the **library's** class loader
(`runtimeMirror(getClass.getClassLoader)`), `getTypeFromClass` uses the class's own loader.

## 6. Object construction (`impl/util/ObjectConstructionUtil.scala`)

`BeanDeserializer` caches `clazz.getConstructor()` (public no-arg) or `null`. `createInstance`:

- constructor present → `constructor.newInstance()` (field initialisers run, so defaults apply; **verified**: `class WithDefaults { var a = 42 }` deserialized from `{}` has `a == 42`).
- otherwise → `sun.misc.Unsafe.allocateInstance(clazz)`: no constructor runs, all fields are zero/null (**verified**: `class NoDefaultCtor(val x: Int) { var y = 100 }` from `{}` gives `y == 0`). Case classes always take this path (their primary constructor has parameters).
- `catch NoSuchMethodException` inside `createInstance` is dead code (the lookup already happened); `InstantiationException` → `JsonInnerException("maybe it is abstract?")`.
- The `Unsafe` instance is obtained once at object init via `theUnsafe`; failure throws `IllegalStateException` and makes the whole deserializer side unusable.

Field population uses `java.lang.reflect.Field.set(instance, value)` after `setAccessible(true)`; this
writes `final` fields of case classes too (works on JDK 25; see known-issues §20 for the JDK roadmap risk).
Lazy vals survive because their bitmap field is zero after `allocateInstance` (**verified**).

With `MPJsonsConfig(caseClassesViaConstructor = true)`, `BeanDeserializer` builds concrete case classes
differently: values are read into an argument array (boxed, `BeanField.readValue`) indexed by primary-constructor
parameter position (`BeanField.argIndex`), absent parameters get zero / false / null / None, and the Java
constructor matching the parameter types is invoked, so the constructor body (`require`, derived vals) runs and no
final field is written reflectively. Fields that are not constructor parameters are set afterwards. Plain classes
keep the allocation path. `ConstructorConstructionSpec`.

## 7. Bean (de)serialization

`BeanSerializer` (`impl/serializer/BeanSerializer.scala`):
- `fields` resolved eagerly; `fieldsWithSerializers` and `writers` are `lazy val`s (recursion guard).
- One `BeanFieldWriter` per field, chosen at build time: `IntWriter`/`LongWriter`/`BooleanWriter`/`DoubleWriter`/... use `Field.getInt` etc. and append the primitive directly (only when the built-in serializer for that type is in effect, so custom converters still win); `ObjectWriter` does `field.get` and delegates. Each writer holds the precomputed `"name":` and `,"name":` prefixes.
- Per call: `append('{')`, a `while` loop over the writers, `append('}')`. Each call increments a per-thread nesting counter (`BeanSerializer.currentPath`); beyond 100 nested beans the beans on the current path are also tracked by identity, so a reference cycle throws `IllegalArgumentException` instead of overflowing the stack. A `null` reference field follows `config.nullPolicy` (`Fail` throws unless `@nullable`, `Omit` skips, `WriteNull` writes `null`). With `config.omitDefaults` each writer also holds the value of a template instance (built like the deserializer builds for `{}`, Options set to `None`) and skips fields equal to it.

`BeanDeserializer` (`impl/deserializer/BeanDeserializer.scala`):
- `fieldTable: BeanFieldTable` (lazy), an open-addressing table of `BeanField`s keyed by JSON name; a field renamed with `@jsonName` gets a second entry under its Scala name as a read fallback, unless another field owns that name as its JSON name. `BeanField.set(instance, iterator)` is typed the same way as the writers (`IntField` → `Field.setInt(IntDeserializer.deserialize(it))`, ... , `ObjectField` → `field.set(deserializer.deserialize(it))`).
- `BeanFieldTable.find` reads the key straight from the raw input, hashing while scanning (String.hashCode formula) and comparing candidates in place with `regionMatches`, so a known key allocates nothing. Quoted keys with escapes and unknown keys are materialised as a `String` (unknown keys: cursor rewound, then `IdentifierDeserializer` reads it for the error message or `skipValue`).
- Loop: `consumeObjectStart`, create instance, skip whitespace, `while (currentChar != '}')`: find field, `consumeFieldValueSeparator`, set or skip/throw; a comma is consumed **if present** (missing and trailing commas are both accepted). Finally `nextCharOrNullIfLast` steps past `}`.
- Fields absent from the JSON keep whatever construction left (null/0/false, or the initialiser value when a no-arg constructor exists). `Option` fields absent → `null`, not `None` (**verified**).

## 8. Low-level parsing: `StringIterator` (`impl/StringIterator.scala`)

Single mutable cursor shared by all deserializers:
- Constructor: `currentChar = stringValue.charAt(0)`, `nextIndex = 1` (empty input throws immediately).
- `nextChar()`: if `nextIndex < length` read it; if `nextIndex == length` set `currentChar = '\u0000'` (end marker, not `' '`); beyond that throw `IllegalArgumentException("Unexpected end of Json")`.
- `skipWhitespaceChars()` loops on `Char.isWhitespace`.
- `consume*` helpers: skip whitespace, check one expected char, advance; error messages embed the whole input and the remainder.
- `seek(index)` positions the cursor on an absolute index; readers that scan ahead on the raw string (`stringValue`, `stringLength`, `nextIndex - 1` = index of `currentChar`) use it to hand the cursor back.
- `depth` / `enterNested(maxDepth)` / `exitNested()` count nesting per call when `MPJsonsConfig.maxDepth` is set: the deserializer memoizer wraps every non-leaf deserializer in `DepthLimitedDeserializer` (`DepthLimit.scala`), and `BeanDeserializer` counts skipped objects and arrays. Without the setting nothing is wrapped.
- `afterValue(closing)` is called by every element loop after a member/element: it skips whitespace and an optional comma; with `strict = true` (second constructor argument, set from `MPJsonsConfig.strict`) a missing comma or a comma before `closing` is an error, and `expectEnd()` rejects content after the root value. `isAtEnd` is `nextIndex > stringLength`; `nextCharOrNullIfLast` now always advances.
- `nextCharOrNullIfLast` returns `Any` (Unit or null); used to step over a closing `}` when it may be the last char.
- `isCurrentCharADigitPart` accepts digits and `-`; `isCurrentCharAFloatingPointPart` additionally `+ . e E`.
- `checkFutureChar`, `hasNextChar`, `nextNonWhitespaceChar` are **unused** (verified by grep).
- `debugGetConsumedString`/`debugGetRemainingString` substring copies are only used for error formatting.

Value readers (`impl/deserializer/jsontypes/`) all work on the raw input and `seek` past what they consumed:
- `AbstractStringDeserializer.readString`: scans for the closing quote; no escape → one `substring`; otherwise bulk-copies runs into a `java.lang.StringBuilder` and decodes the eight short escapes and `\uXXXX` (`appendCodePoint`).
- `AbstractIntegerDeserializer.readLong`: accumulates digits (negatively, so `Long.MinValue` fits) with overflow detection; `readLongInRange` adds the Int/Short/Byte range check. No digit → `Expected a number but found 'x'`.
- `AbstractFloatingPointDeserializer.scanNumber/readDouble/readFloat`: scans the literal extent; a plain integer of up to 18 digits is accumulated directly, anything else goes through `parseDouble`/`parseFloat` on one substring. `skipNumber` scans without materialising.
- `BooleanDeserializer`: `startsWith("true"/"false")` on the raw input.

## 9. Special converters (`impl/special/TypedConverter.scala`)

- `TypedSerializer`: `obj.getClass.getSimpleName` as key; per-class serializer cache (`var Map`); writes `{"SimpleName":` + inner + `}`; inner serializer resolved with `allowSuperType = false`.
- `TypedDeserializer(binaryNamePrefix, knownSubclasses, markedTypeName, ...)`: reads the key as a JSON string and resolves it first among the known subclasses of a sealed marked type (collected transitively with `knownDirectSubclasses` at `markTypedClass` time), then as `binaryNamePrefix + typeName` where the prefix is the marked type's binary class name up to the last `.` or `$` (siblings in the same package or enclosing object). Resolved deserializers are cached per name. Two subtypes with the same simple name cannot be distinguished.

`SimpleMapSerializer[T]` / `SimpleMapDeserializer[T]` (object form `{"k":v}`) are **not** wired into the
dispatch; the user instantiates them with a `TypeTag[T]` and registers them as a converter. Both follow the
factory's `NullPolicy` (`serializerFactory.config` / `deserializerFactory.config`): a null value is treated like a
null bean field on write (`Fail` throws, `Omit` skips the entry, `WriteNull` writes `null`) and like any map value
on read.

## 10. Caches and mutability (thread-safety)

Caches are `java.util.concurrent.ConcurrentHashMap`s filled with get-then-`putIfAbsent`:
`MPJsons.extractTypeFromNameCache`; the two maps in each `*FactoryMemoizer`; `ReflectionUtil.getAllAccessibleFieldsCache`;
the three maps in `TypesUtil` (global, shared across `MPJsons` instances); `TypedSerializer.serializers`,
`TypedDeserializer.deserializers`. Under contention a (de)serializer may be built twice; the first stored wins.
`ConcurrencySpec` runs cold first-time use of a dozen types from 8 threads.

Still plain `var Map`s, written only by the registration methods: `*FactoryImpl.additional*` registries and
`postTransform`. Every registration calls `registrationsChanged()`, which the memoizers override to clear both caches, so a
converter registered after a type was first used still takes effect (nested types are rebuilt too); `StaticSerializer` /
`StaticDeserializer` instances built earlier keep their behaviour. Register converters and typed classes before sharing an instance across threads. Scala runtime
reflection is documented thread-safe since 2.11 but is lock-based, so cold-start work from many threads
serialises on that lock.

## 11. File map

| Path (under `src/main/scala/io/mpjsons/`) | Responsibility |
|---|---|
| `MPJsons.scala` | Facade, `StaticSerializer`, `StaticDeserializer`, `nullable`, error formatting |
| `JsonTypeSerializer.scala`, `JsonTypeDeserializer.scala` | The two single-method traits every converter implements |
| `impl/StringIterator.scala` | Cursor over the input string |
| `impl/JsonInnerException.scala` | Boundary exception |
| `impl/SerializerFactory.scala`, `impl/DeserializerFactory.scala` | Concrete factory classes (empty) |
| `impl/factoryimpl/*FactoryImpl.scala` | Dispatch chains and converter registries |
| `impl/factoryimpl/*FactoryMemoizer.scala`, `TypeParamsResolver.scala` | Caching and type-parameter substitution |
| `impl/util/Context.scala`, `TypesUtil.scala`, `ObjectConstructionUtil.scala` | Context threading, Type/Class helpers, instance allocation |
| `impl/util/reflection/ReflectionUtil*.scala` | Field discovery (`FieldWithTypeInfo`) across the class hierarchy |
| `impl/serializer/*.scala` | `BeanSerializer` (+ `BeanFieldWriter`s), `StringSerializer`, `PrimitiveSerializers` (`Int/Long/Short/Byte/BooleanSerializer`), `FloatingPointSerializers` (`Double/FloatSerializer`), `SimpleToStringSerializer` (legacy), `IterableSerializer`/`ArraySerializer` (via `common/IteratorSerializer`), `MapSerializer`, `SimpleMapSerializer`, `OptionSerializer`, `EitherSerializer`, `BitSetSerializer`, `Tuple1..6Serializer`, `SingletonObjectSerializer`, `time/*` |
| `impl/deserializer/*.scala` | `BeanDeserializer`, `BeanField` (typed setters), `BeanFieldTable` (in-place key matching), `IdentifierDeserializer`, `PostTransformDeserializer`, `SimpleMapDeserializer`, `SingletonObjectDeserializer`; `jsontypes/` raw-input readers; `values/` primitives; `immutables/` collections and `Option`; `mutables/` `Array`, `ArrayBuffer`, mutable `HashSet`/`HashMap`; `utiltypes/` `Either`, tuples; `time/` |
| `impl/special/TypedConverter.scala` | Polymorphic wrapper converters |
| `src/jmh/scala/io/mpjsons/bench/MpjsonsBenchmark.scala` | JMH benchmarks (`sbt "Jmh/run ..."`, see performance.md) |

Tests: `src/test/scala/io/mpjsons/*Spec.scala`, 97 tests, all passing on JDK 25. `PerformanceSpec` asserts 5000 round-trips in under 500 ms, which the code passes with a very large margin, so it does not detect regressions; use the JMH benchmarks.
