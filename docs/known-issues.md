# Known issues, limitations and risks

Status key: **BUG (verified)** = reproduced by running code on this checkout (13/10/2026, JDK 25);
**BUG (by inspection)** = clear from the source, not executed; **LIMITATION** = by design or
documented; **RISK** = depends on environment or future JDK changes.

Repro snippets assume `val m = new MPJsons`.

## Confirmed bugs

### 1. Generic beans: type-parameter bindings leak between instantiations — BUG (verified)

```scala
case class Box[T](items: List[T], single: T)
m.deserialize[Box[Int]]("""{"items":[1,2],"single":3}""")          // Box(List(1, 2),3)
m.deserialize[Box[String]]("""{"items":["a","b"],"single":"c"}""") // JsonInnerException ← NumberFormatException: ""
m.serialize(Box(List(1), 2)); m.serialize(Box(List("x"), "y"))     // {"single":"y","items":[x]}   ← x unquoted
```

Root cause: `DeserializerFactoryMemoizer.scala:20-31` / `SerializerFactoryMemoizer.scala:19-32`. The cache key is
`(tpe, allowSuperType)`; `tpe` for the field `items: List[T]` is the unsubstituted `List[T]` (its head symbol
`List` is not a parameter, so no substitution happens), and the `ListDeserializer` built for it captured the
`Int` element deserializer from the first `Context`. The `single: T` field works because `T` itself is a
parameter and is substituted before the lookup.

Fix direction: substitute all type parameters in `tpe` before using it as a key, e.g.
`tpe.map(t => if (t.typeSymbol.isParameter) context.typeParams.getOrElse(t.typeSymbol, t) else t)` (or
`tpe.substituteTypes(params, args)`), so the key is the fully concrete `List[Int]`. Add a spec with the three
lines above. `InnerParamsSpec` passes today only because it instantiates `SomeType[T]` with a single `T`.

### 2. Inherited fields are silently ignored — BUG (verified)

```scala
class Base { var baseField: Int = 1 }
class Derived extends Base { var derivedField: Int = 2 }
m.serialize(new Derived)   // {"derivedField":2}
```

Root cause: `ReflectionUtilNoCache.scala:21` enumerates `tpe.members`, which excludes `private[this]` members of
superclasses; every Scala backing field is `private[this]`. The superclass walk in `getAccessibleField` (`:37-50`)
only helps for a member that *is* listed. README and the scaladoc claim superclass support. No test covers inheritance.

Fix direction: iterate `tpe.baseClasses` (excluding `Any`, `AnyRef`, `Object`, `Product`, `Serializable`, `Equals`)
and collect `baseType(cls).decls` term symbols that are fields (`isTerm && !isMethod && !isSynthetic`), with the
field's `typeSignature` *as seen from* `tpe` (`tpe.baseType(cls)` + `asSeenFrom`) so generic superclasses work.
Decide and document the key order for inherited fields (base first is the usual expectation).

### 3. Deserializing a field of singleton type crashes — BUG (verified)

```scala
object Single; case class WrapsSingle(s: Single.type)
m.serialize(WrapsSingle(Single))                 // {"s":{}}   OK
m.deserialize[WrapsSingle]("""{"s":{}}""")       // ClassCastException: UniqueSingleType cannot be cast to TypeRefApi
```

Root cause: `DeserializerFactoryImpl.scala:95` `tpe.asInstanceOf[TypeRef].sym == definitions.ArrayClass` without
the `isInstanceOf[TypeRefApi]` guard that `SerializerFactoryImpl.scala:94` has. Any non-`TypeRef` `Type`
(`SingleType`, `RefinedType`, `ExistentialType`) hits it.

### 4. Trait/interface-typed targets NPE in field lookup — BUG (verified)

```scala
m.deserialize[scala.collection.mutable.Map[Int,String]]("""[[1,"a"]]""")
// NullPointerException: Class.getSuperclass() is null  (ReflectionUtilNoCache.scala:44)
```

`getAccessibleField` assumes `clazz.getSuperclass != null`. Separately: there are **no deserializers for any
mutable collection** (`ArrayBuffer`, `mutable.Map`, `mutable.Set`, ...), although all of them serialize fine
(`SerializerFactoryImpl.scala:100-125`). Either add them (`mutable.ArrayBuffer`, `mutable.HashMap`,
`mutable.HashSet`, `ListBuffer` are the obvious set) or fail with a clear "unsupported type" message.

### 5. Empty input escapes the error wrapper — BUG (verified)

`m.deserialize[Stat]("")` throws raw `StringIndexOutOfBoundsException` from `StringIterator.scala:11`
(`stringValue.charAt(0)` in the constructor, which runs before the `try` in `MPJsons.deserialize`).

### 6. `null` inside containers produces raw NPEs — BUG (verified)

`m.serialize[Option[String]](Some(null))` and `m.serialize(List[String]("a", null))` throw
`NullPointerException` from `StringSerializer.serialize` (`obj.toString`). The bean-level null policy
(`@nullable`, `ignoreNullFields`) does not apply to elements, and no serializer can emit `null`.

### 7. Non-finite doubles produce invalid JSON — BUG (verified)

`m.serialize(Double.NaN)` → `NaN`, `m.serialize(Double.PositiveInfinity)` → `Infinity`. Decide: throw, or write `null`/a string.

### 8. `java.time.Duration` deserializer cannot be overridden — BUG (by inspection)

`DeserializerFactoryImpl.scala:91` lacks the `&& !additionalDeserializers.contains(tpe.toString)` guard that
`LocalTime/LocalDate/LocalDateTime` (`:85-89`) and all four serializers (`SerializerFactoryImpl.scala:73-80`) have.
(The reverted commit `2bfcbb2` had fixed exactly this line.)

### 9. Dead "missing field" checks in time deserializers — BUG (by inspection)

`LocalDateDeserializer.scala:40-44` and `LocalTimeDeserializer.scala:41-47` test `< -1` against sentinels initialised
to `-1`, so they never fire; a missing key surfaces as `DateTimeException` from `LocalDate.of`. Also every time
deserializer requires *exactly* N keys (loop `0 until N`), so an extra key is reported as "Incorrect identifier" and a
missing one as a separator error.

### 10. `TypedDeserializer` class resolution is fragile — LIMITATION / BUG (by inspection)

`TypedConverter.scala:45` resolves `packageName + "." + simpleName`. Fails for subtypes in another package, for
classes nested in objects/classes (binary name uses `$`), and two subtypes with equal simple names in different
packages collide in the per-name cache. Consider writing the fully-qualified or a registered alias name
(roadmap.md §4.4).

### 11. Superclass-converter matching is nondeterministic — BUG (by inspection)

`DeserializerFactoryImpl.scala:175-179` / `SerializerFactoryImpl.scala:165-169` iterate a `Map[Symbol, _]`; when a
type extends two registered supertypes, which converter wins depends on map iteration order.

### 12. Misleading errors on type mismatch — LIMITATION (verified)

`"1"`, `true`, or `1.5` for an `Int` field → `NumberFormatException: For input string: ""` or
`Unexpected end of Json`, because the numeric reader consumes zero chars and the bean loop then
misinterprets the rest. A JSON-aware "expected number but found string" message costs nothing on the hot path
(check the first char once).

## Limitations and sharp edges

### 13. Parser is lenient, not validating — LIMITATION (verified)

Missing commas, trailing commas, trailing garbage after the root value, duplicate keys (last wins) and bare keys are
all accepted silently; there is no recursion depth limit. Acceptable for trusted producers (this library's own
output), risky for external input. Any rewrite of the hot loops should decide explicitly which of these to keep
(bare keys and whitespace tolerance are documented features and tested; the rest are accidents).

### 14. No `null` literal support on read — LIMITATION (verified)

See json-format.md §2 and roadmap.md §3. Today a `null` value for any known field throws; `null` is tolerated only
while skipping unknown fields in `ignoreNonExistingFields` mode.

### 15. Missing JSON key leaves `Option` fields as `null` — LIMITATION (verified)

`case class WithOpt(a: Option[String], ...)` from `{"b":"x"}` gives `a == null`. Users work around it with
`postDeserializationTransform`. Pre-initialising `Option` fields to `None` after instance creation is cheap
(field list is known at build time).

### 16. `java.*` value types fall into bean reflection and fail under JPMS — LIMITATION (verified)

`m.serialize(BigDecimal("1.5"))` and `m.serialize(UUID.randomUUID())` throw `InaccessibleObjectException`
(`module java.base does not "opens java.math"`). Any JDK type without an explicit (de)serializer is unusable on
JDK 9+. Options: dedicated converters for the common ones (`BigDecimal`, `BigInt`, `UUID`, `Instant`, `ZonedDateTime`,
`java.util.Date`), and a fail-fast check in the bean fallback for `typeSymbol.fullName.startsWith("java.")`.

### 17. Field order is reverse declaration order — LIMITATION (verified)

Output key order follows `tpe.members` (reverse). `members.sorted` or `decls` give declaration order
(**verified**). Changing it alters output strings (only `JavaPrimitivesSpec` asserts a multi-field order), which
matters for anyone diffing or hashing serialized JSON; see roadmap.md §2.3.

### 18. Thread-safety — RISK

All caches are unsynchronized `var Map`s (architecture.md §10). Concurrent first-time use of new types can
build a (de)serializer twice and can race inside Scala runtime reflection. Warm up types before sharing an
instance across threads, or move caches to `ConcurrentHashMap.computeIfAbsent` (and make the registries
immutable after a `freeze()`).

### 19. `TypeTag`-keyed cache does not cache — LIMITATION (verified by timing)

`MPJsons.extractTypeCache` is keyed by `TypeTag`, whose `equals`/`hashCode` evaluate `tag.tpe`, which is the
expensive step it tries to skip; and a fresh `TypeTag` object is materialised at every call site invocation
(~0.15 µs). See performance.md §3.

### 20. JDK evolution risks — RISK

- `sun.misc.Unsafe.allocateInstance` (`ObjectConstructionUtil.scala:71`) is still available on JDK 25 and is not one
  of the memory-access methods deprecated by JEP 471, but `sun.misc.Unsafe` as a whole is on a removal path.
  Alternatives: `sun.reflect.ReflectionFactory.newConstructorForSerialization` (kept in a comment at `:78-86`,
  itself `jdk.unsupported`), or generating constructors via `MethodHandles` for case classes (call the primary
  constructor with parsed arguments instead of mutating an allocated blank).
- Writing `final` fields through `Field.set` (what deserializing a case class does) is the subject of JEP 500
  ("Prepare to Make Final Mean Final", proposed for JDK 26): first a warning, later disallowed unless
  `--enable-final-field-mutation`. **Verify the current status before planning**; if it lands, constructor-based
  construction of case classes becomes mandatory, which also fixes the "defaults are bypassed" problem.
- `Field.setAccessible(true)` on application classes keeps working (unnamed module), but not on `java.*` (§16).

### 21. Miscellaneous (by inspection)

- `Iterable[T]` deserializes to a mutable `ArrayBuffer` exposed as `Iterable`; `Stream` is deprecated in 2.13 and `LazyList` is unsupported.
- `registerConverter` keys on `tpe.toString`; two different types with the same printed name (e.g. same simple name after `import`-dependent printing? no, `toString` is fully qualified for `TypeRef`) are unlikely but type aliases print as their alias.
- `TypesUtil.getClassFromType` uses the library's class loader; `getTypeFromClass` uses the class's loader; `deserialize(json, typeName)` uses `Class.forName(typeName)` with the caller-independent loader. Multi-classloader deployments (app servers, plugins) can fail to resolve application classes.
- `slf4j-api` is a declared runtime dependency but is never used.
- `checkFutureChar`, `hasNextChar`, `nextNonWhitespaceChar` in `StringIterator`, the `Java*Deserializer` objects in `values/`, and the `NoSuchMethodException` catch in `ObjectConstructionUtil.createInstance` are dead code.
- `IdentifierDeserializer` for bare keys stops at `:` or whitespace only; a bare key containing `}` or `,` would swallow them.
- Tuple7 and larger serialize as beans (`{"_7":..}`) rather than arrays like Tuple1..6.
- `DeserializerFactoryImpl.scala:99` and `:155` check `Map` twice (harmless).
