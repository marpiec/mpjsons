# Known issues, limitations and risks

Status key: **BUG (verified)** = reproduced by running code on this checkout (13/10/2026, JDK 25);
**BUG (by inspection)** = clear from the source, not executed; **LIMITATION** = by design or
documented; **RISK** = depends on environment or future JDK changes.

Repro snippets assume `val m = new MPJsons`.

## Confirmed bugs

### 1. Generic beans: type-parameter bindings leak between instantiations — FIXED

Fixed: `TypeParamsResolver` substitutes all type parameters before the memoizer cache lookup; covered by
`GenericTypeParamsSpec`. (Was: `Box[Int]` then `Box[String]` on one instance failed because `List[T]` was
cached under the unsubstituted type.)

### 2. Inherited fields are silently ignored — FIXED

Fixed: `ReflectionUtilNoCache.getAllAccessibleFields` walks `tpe.baseClasses` and resolves field types with
`typeSignatureIn(tpe)` (generic superclasses work); own fields keep the historical reverse declaration order and
superclass fields follow. Covered by `InheritanceSpec`.

### 3. Deserializing a field of singleton type crashes — FIXED

Fixed: the `Array` check is guarded with `isInstanceOf[TypeRefApi]`, singletons are detected with
`typeSymbol.isModuleClass` in both factories (instead of scanning for a `MODULE$` field), and the singleton reader
skips whitespace. Round trip returns the same instance; covered by `SingletonObjectSpec`.

### 4. Trait/interface-typed targets NPE in field lookup — FIXED

Fixed: `getAccessibleField` stops at a `null` superclass; deserializers were added for `mutable.ArrayBuffer`
(also used for `mutable.Buffer/Seq/IndexedSeq/Iterable`), `mutable.HashSet`/`mutable.Set` and
`mutable.HashMap`/`mutable.Map`; a trait or abstract class without a registered converter now fails at
(de)serializer build time with a message naming the type and the registration options. `ListBuffer` still
throws by design. Covered by `MutableCollectionsSpec`.

### 5. Empty input escapes the error wrapper — FIXED

Fixed: `StringIterator` starts at the end-of-input marker for an empty string, so the failure is reported as a
`JsonInnerException` naming the target type. Covered by `MalformedJsonSpec`.

### 6. `null` inside containers produces raw NPEs — BUG (verified)

`m.serialize[Option[String]](Some(null))` and `m.serialize(List[String]("a", null))` throw
`NullPointerException` from `StringSerializer.serialize` (`obj.toString`). The bean-level null policy
(`@nullable`, `ignoreNullFields`) does not apply to elements, and no serializer can emit `null`.

### 7. Non-finite doubles produce invalid JSON — FIXED

Fixed: `DoubleSerializer`/`FloatSerializer` (also used for the boxed Java types) throw `IllegalArgumentException`
for `NaN` and infinities instead of emitting text no JSON parser accepts. Covered by `FloatingPointSpec`.

### 8. `java.time.Duration` deserializer cannot be overridden — FIXED

Fixed: the `Duration` branch has the same "no converter registered" guard as the other time types. Covered by
`TimeSpec` ("override the built-in Duration format").

### 9. Dead "missing field" checks in time deserializers — FIXED

Fixed: the four time deserializers read any number of keys in any order and report the first missing key by name;
unknown keys are still rejected. Covered by `TimeSpec`.

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
