# Architecture: how mpjsons works

`mpjsons` converts Scala objects to and from JSON using **Scala 2.13 runtime reflection**
(`scala.reflect.runtime.universe`) to discover types, plus **Java reflection** (`java.lang.reflect.Field`)
to read and write fields. For every `Type` it builds one (de)serializer object, caches it, and from
then on each call is a tree walk over those cached objects. There is no intermediate JSON tree
(no AST, no token stream): serializers append directly to a `StringBuilder`, deserializers pull
characters directly from a `StringIterator`.

Dependencies: `scala-reflect` (essential) and `slf4j-api` (**verified unused** in `src/main`, can be dropped).

## 1. Public API surface (`src/main/scala/io/mpjsons/MPJsons.scala`)

| Member | Notes |
|--------|-------|
| `class MPJsons(ignoreNonExistingFields = false, ignoreNullFields = false)` | One instance owns one `SerializerFactory` and one `DeserializerFactory` and all caches. |
| `serialize[T](obj)(implicit TypeTag[T]): String` | Resolves `Type` from the tag, looks up the serializer, writes into a fresh `scala.collection.mutable.StringBuilder`, returns `toString`. |
| `serialize(obj, tpe: Type)` / `serialize(obj, typeName: String)` | Same, with an explicit `Type` or a class name resolved via `Class.forName`. |
| `deserialize[T](json)(implicit TypeTag[T]): T`, `deserialize(json, tpe)`, `deserialize(jsonIterator, tpe)`, `deserialize(json, typeName)` | Wrap any `RuntimeException` into `JsonInnerException` with a pointer-annotated message (`ErrorMessageFormatter`, `MPJsons.scala:11-28`). |
| `buildStaticSerializer[T]` / `buildStaticDeserializer[T]` | Return `StaticSerializer` / `StaticDeserializer` bound to one `JsonTypeSerializer`/`Deserializer`; skip the per-call factory lookup. `StaticSerializer.serializeToStringBuilder` avoids the final `toString` copy. |
| `registerConverter[T](sf => ser, df => deser)` | Exact-type custom converter. Keyed by `tpe.toString` (`SerializerFactoryImpl.scala:25`, `DeserializerFactoryImpl.scala:34`). |
| `registerSuperclassConverter[T]` | For `T` and subtypes. Keyed by `Symbol`, matched via `tpe.baseClasses.contains`. |
| `registerSerializerForAnySubtype[T]` | Serialize-only; keyed by the type's string with `[...]` stripped (regex `\[.*\]`), consulted last (`SerializerFactoryImpl.scala:190`). |
| `markTypedClass[T]` / `markTypedSuperClass[T]` | Register `TypedConverter` for `T` (wrapper `{"SimpleName":{...}}`). |
| `postDeserializationTransform[T](f: T => T)` | Wraps the deserializer of exactly `T` in `PostTransformDeserializer`. |
| `@nullable` (`final class nullable extends StaticAnnotation`) | Field may be `null` on serialize (omitted). Must be applied with `@(nullable @field)` on case-class params (**verified**: without `@field` the annotation lands on the constructor parameter and is invisible to the field scan). |

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
3. `java.time.LocalTime/LocalDate/LocalDateTime` unless a converter is registered for that `tpe.toString`; `java.time.Duration` **without** that override guard (`:91`); `java.lang.Character`.
4. `Array` via `tpe.asInstanceOf[TypeRef].sym == definitions.ArrayClass` (**unguarded cast**, crashes for `SingleType` such as `Foo.type`, known-issues §3).
5. `Option`, `Map`/`immutable.Map`, `Either`.
6. `Tuple1..Tuple6`.
7. Seqs: `List`, `Vector`, `Iterable`, `Seq`, `Stream`, `immutable.Queue` (each matched for both the `scala.collection` and `immutable` symbol where both exist).
8. Sets: `Set`, `immutable.HashSet`, `immutable.ListSet`; `SortedSet`/`TreeSet` → `IllegalStateException`; `BitSet`.
9. Maps: `immutable.HashMap`; `SortedMap`/`TreeMap` → throw; `immutable.ListMap`.
10. Mutable collections: `mutable.ArrayBuffer/Buffer/Seq/IndexedSeq/Iterable` → `ArrayBufferDeserializer`; `mutable.Set/HashSet` → `MutableHashSetDeserializer`; `mutable.Map/HashMap` → `MutableHashMapDeserializer`.
11. If `allowSuperType`: registered superclass deserializers (exact symbol, then `baseClasses.contains` over the registry map in map-iteration order).
12. `ListBuffer` → `IllegalArgumentException`.
13. Exact-type custom deserializer by `tpe.toString`.
14. `Nothing` / `Any` / `AnyRef` → `IllegalArgumentException`.
15. Singleton object (`typeSymbol.isModuleClass`) → `SingletonObjectDeserializer`.
16. Abstract type (trait / abstract class, `typeSymbol.isAbstract`) → `IllegalArgumentException` naming the type and the registration options.
17. Otherwise `BeanDeserializer`.

Then `getDeserializerNoCache` wraps the result in `PostTransformDeserializer` if a transform is registered for exactly `tpe`.

`SerializerFactoryImpl.getSerializerNoCache` (`:40-199`):

1. Scala and Java numeric/boolean primitives → `SimpleToStringSerializer` (`append(obj: Any)`).
2. `java.time` four types (all four guarded by "no converter registered").
3. `String`, `mutable.StringBuilder`, `Char`, `java.lang.Character` → `StringSerializer`.
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
for (baseClass <- tpe.baseClasses)                       // linearization: the class itself first, then supertypes
  for (m <- tpe.baseType(baseClass).decls.toList.reverse  // reverse declaration order (historical `members` order)
         if m.isTerm && !m.isMethod && !m.isSynthetic && !m.isModule; if seenNames.add(name))
    FieldWithTypeInfo(getAccessibleField(clazz, name),    // Java Field (walks superclasses) or null → skipped
                      m.typeSignatureIn(tpe),             // field type as seen from tpe (generic bases resolved)
                      m.annotations.exists(_.toString.toLowerCase.contains("nullable")))
```

Verified properties of this scan:

- Own fields come first in **reverse declaration order** (`Ordered(zeta, alpha, mid, beta)` serializes as `{"beta":..,"mid":..,"alpha":..,"zeta":..}`), then each superclass's fields in the same order. Switching to declaration order is `decls` without the `.reverse` (roadmap §2.3, owner decision).
- A field name declared in both a class and its superclass is taken once, from the most-derived class.
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

## 7. Bean (de)serialization

`BeanSerializer` (`impl/serializer/BeanSerializer.scala`):
- `fields` resolved eagerly, `fieldsWithSerializers` is a `lazy val` (recursion guard).
- Per call: `append('{')`, then for each field `field.get(obj)`; `null` → throw `IllegalArgumentException` unless `!errorOnNulls || nullable`, in which case the field is **omitted** (the library never writes a `null` literal); else append `"name":` and delegate. Commas are tracked with a boolean.

`BeanDeserializer` (`impl/deserializer/BeanDeserializer.scala`):
- `fieldsByName: Map[String, (Field, JsonTypeDeserializer[AnyRef])]`, lazy.
- Loop: `consumeObjectStart`, create instance, `while (currentChar != '}')`: read identifier (`IdentifierDeserializer`, quoted or bare), `consumeFieldValueSeparator`, look up field; known → deserialize + `field.set`; unknown → `skipValue` if `ignoreNonExistingFields` (handles `null` literal here only) else throw. A comma is consumed **if present** (so missing and trailing commas are both accepted). Finally `nextCharOrNullIfLast` steps past `}`.
- Fields absent from the JSON keep whatever construction left (null/0/false, or the initialiser value when a no-arg constructor exists). `Option` fields absent → `null`, not `None` (**verified**).

## 8. Low-level parsing: `StringIterator` (`impl/StringIterator.scala`)

Single mutable cursor shared by all deserializers:
- Constructor: `currentChar = stringValue.charAt(0)`, `nextIndex = 1` (empty input throws immediately).
- `nextChar()`: if `nextIndex < length` read it; if `nextIndex == length` set `currentChar = '\u0000'` (end marker, not `' '`); beyond that throw `IllegalArgumentException("Unexpected end of Json")`.
- `skipWhitespaceChars()` loops on `Char.isWhitespace`.
- `consume*` helpers: skip whitespace, check one expected char, advance; error messages embed the whole input and the remainder.
- `nextCharOrNullIfLast` returns `Any` (Unit or null); used to step over a closing `}` when it may be the last char.
- `isCurrentCharADigitPart` accepts digits and `-`; `isCurrentCharAFloatingPointPart` additionally `+ . e E`.
- `checkFutureChar`, `hasNextChar`, `nextNonWhitespaceChar` are **unused** (verified by grep).
- `debugGetConsumedString`/`debugGetRemainingString` substring copies are only used for error formatting.

Value readers (`impl/deserializer/jsontypes/`): `AbstractStringDeserializer.readString` (escape handling incl. `\uXXXX`, appends char by char into a new `StringBuilder`), `AbstractIntegerDeserializer.readNumberString` and `AbstractFloatingPointDeserializer.readNumberString` (collect chars into a `StringBuilder`, then `toInt`/`toLong`/`toDouble`), `BooleanDeserializer` (collects lowercase letters, matches `"true"`/`"false"`).

## 9. Special converters (`impl/special/TypedConverter.scala`)

- `TypedSerializer`: `obj.getClass.getSimpleName` as key; per-class serializer cache (`var Map`); writes `{"SimpleName":` + inner + `}`; inner serializer resolved with `allowSuperType = false`.
- `TypedDeserializer(packageName, ...)`: reads the key as a JSON string, `Class.forName(packageName + "." + typeName)`, caches per name. Consequences: subtypes must live in the **same package** as the marked type, must be top-level (nested classes have `$` in their binary name), and two subtypes with the same simple name in different packages collide.

`SimpleMapSerializer[T]` / `SimpleMapDeserializer[T]` (object form `{"k":v}`) are **not** wired into the
dispatch; the user instantiates them with a `TypeTag[T]` and registers them as a converter. Null map values are skipped on serialize.

## 10. Caches and mutability (thread-safety)

Caches are `java.util.concurrent.ConcurrentHashMap`s filled with get-then-`putIfAbsent`:
`MPJsons.extractTypeFromNameCache`; the two maps in each `*FactoryMemoizer`; `ReflectionUtil.getAllAccessibleFieldsCache`;
the three maps in `TypesUtil` (global, shared across `MPJsons` instances); `TypedSerializer.serializers`,
`TypedDeserializer.deserializers`. Under contention a (de)serializer may be built twice; the first stored wins.
`ConcurrencySpec` runs cold first-time use of a dozen types from 8 threads.

Still plain `var Map`s, written only by the registration methods: `*FactoryImpl.additional*` registries and
`postTransform`. Register converters and typed classes before sharing an instance across threads. Scala runtime
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
| `impl/factoryimpl/*FactoryMemoizer.scala` | Caching and type-parameter resolution |
| `impl/util/Context.scala`, `TypesUtil.scala`, `ObjectConstructionUtil.scala` | Context threading, Type/Class helpers, instance allocation |
| `impl/util/reflection/ReflectionUtil*.scala` | Field discovery (`FieldWithTypeInfo`) |
| `impl/serializer/*.scala` | `BeanSerializer`, `StringSerializer`, `SimpleToStringSerializer`, `IterableSerializer`/`ArraySerializer` (via `common/IteratorSerializer`), `MapSerializer`, `SimpleMapSerializer`, `OptionSerializer`, `EitherSerializer`, `BitSetSerializer`, `Tuple1..6Serializer`, `SingletonObjectSerializer`, `time/*` |
| `impl/deserializer/*.scala` | `BeanDeserializer`, `IdentifierDeserializer`, `PostTransformDeserializer`, `SimpleMapDeserializer`, `SingletonObjectDeserializer`; `jsontypes/` abstract readers; `values/` primitives; `immutables/` collections and `Option`; `mutables/ArrayDeserializer`; `utiltypes/` `Either`, tuples; `time/` |
| `impl/special/TypedConverter.scala` | Polymorphic wrapper converters |

Tests: `src/test/scala/io/mpjsons/*Spec.scala`, 56 tests in 26 suites, all passing on JDK 25 (**verified**, 12 s wall clock including compilation). `PerformanceSpec` asserts 5000 round-trips in under 500 ms, which the current code passes with roughly a 20x margin, so it does not detect regressions of the kind that matter.
