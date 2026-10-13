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

### 6. `null` inside containers produces raw NPEs — FIXED

Null elements of collections, maps, tuples, `Either` and `Some` raise `IllegalArgumentException` naming the place
under the default `NullPolicy.Fail` and are written as `null` under `Omit` / `WriteNull`; bean fields follow
`NullPolicy` (see json-format.md §2). Covered by `NullLiteralSpec`.

### 7. Non-finite doubles produce invalid JSON — FIXED

Fixed: `DoubleSerializer`/`FloatSerializer` (also used for the boxed Java types) throw `IllegalArgumentException`
for `NaN` and infinities instead of emitting text no JSON parser accepts. Covered by `FloatingPointSpec`.

### 8. `java.time.Duration` deserializer cannot be overridden — FIXED

Fixed: the `Duration` branch has the same "no converter registered" guard as the other time types. Covered by
`TimeSpec` ("override the built-in Duration format").

### 9. Dead "missing field" checks in time deserializers — FIXED

Fixed: the four time deserializers read any number of keys in any order and report the first missing key by name;
unknown keys are still rejected. Covered by `TimeSpec`.

### 10. `TypedDeserializer` class resolution is fragile — FIXED for nested and sealed types

Resolution order is now: known subclasses of a sealed marked type (wherever they are declared), then
`<binary name prefix of the marked type> + SimpleName` (same package for top-level types, same enclosing object for
nested types); an unknown name fails with a message listing the known subclasses. Covered by `TypedNestedSpec`.
Remaining limitation: subtypes of a **non-sealed** trait must be siblings of the trait, and two subtypes with the
same simple name cannot be told apart (the wire format carries only the simple name; see roadmap.md §4.1 for aliases).

### 11. Superclass-converter matching is nondeterministic — FIXED

The registry is probed in the order of `tpe.baseClasses` (the linearization, most derived first), so the most
specific registered supertype wins regardless of registration order. Covered by `SuperclassConverterSpec`.

### 12. Misleading errors on type mismatch — FIXED for numbers

Number readers now report `Expected a number but found '"'` (or `'t'`, `end of input`) when no digit is present,
and overflow as `NumberFormatException` quoting the offending digits (`NumberParsingSpec`). A `1.5` for an `Int`
field still stops after `1` and fails on the following `.5` with a separator error.

## Limitations and sharp edges

### 13. Parser is lenient, not validating — ADDRESSED by an opt-in lever

`MPJsonsConfig(strict = true)` rejects missing commas, trailing commas and content after the root value
(`StringIterator.afterValue` / `expectEnd`, `StrictParsingSpec`). Bare keys, whitespace and duplicate keys (last
wins) are accepted in both modes. Nesting depth on read is bounded only with the opt-in `MPJsonsConfig(maxDepth = n)`; cyclic object graphs on write
are reported as `IllegalArgumentException` (`RecursionLimitsSpec`). The default stays lenient (owner
decision 6 in roadmap.md §6 is about the default only).

### 14. No `null` literal support on read — FIXED (owner decisions 13/10/2026)

`null` is accepted only where declared: `None` for `Option` values, `null` for `@nullable` reference fields; under
the default `NullPolicy.Fail` everything else is rejected with a message naming the field or type, while `Omit` /
`WriteNull` accept it for any reference type. Primitives never accept it. See json-format.md §2 and `NullLiteralSpec`.

### 15. Missing JSON key leaves `Option` fields as `null` — FIXED

`Option` fields that construction left `null` are set to `None` before the key loop, so an absent key reads as
`None`; defaults set by a no-arg constructor are kept.

### 16. `java.*` value types fall into bean reflection and fail under JPMS — FIXED

Built-in support for `BigDecimal`, `BigInt`, `java.math.BigDecimal/BigInteger` (exact JSON numbers) and
`java.util.UUID`, `java.time.Instant/OffsetDateTime/ZonedDateTime` (strings); any other `java.*` type without a
registered converter now fails at build time with a message naming the type instead of
`InaccessibleObjectException`. Covered by `ExtraTypesSpec`.

### 17. Field order is reverse declaration order — FIXED (owner decision 13/10/2026)

Keys are written in declaration order, superclass fields first. Output strings of multi-field types changed
accordingly (readers never depended on order).

### 18. Thread-safety — FIXED for caches, registries remain registration-time only

All (de)serializer, reflection and type caches are `ConcurrentHashMap`s with get-then-`putIfAbsent` (never
`computeIfAbsent`, because building a (de)serializer re-enters the factory). `ConcurrencySpec` exercises cold
first-time use of a dozen types from 8 threads. The converter registries (`registerConverter`,
`markTypedClass`, `postDeserializationTransform`) are still plain `var Map`s: register everything before sharing
an instance across threads.

### 19. `TypeTag`-keyed cache does not cache — FIXED

`MPJsons.extractType` is now just `tag.tpe`; the memoizer looks the cache up before building any `Context`, so a
warm root lookup is one hash probe with no allocation. The remaining TypeTag-API overhead (~100 ns, ~100 B) is the
`TypeTag` materialisation at the call site, which only `buildStatic*` avoids.

### 20. JDK evolution risks — RISK

- `sun.misc.Unsafe.allocateInstance` (`ObjectConstructionUtil.scala:71`) is still available on JDK 25 and is not one
  of the memory-access methods deprecated by JEP 471, but `sun.misc.Unsafe` as a whole is on a removal path.
  Alternatives: `sun.reflect.ReflectionFactory.newConstructorForSerialization` (kept in a comment at `:78-86`,
  itself `jdk.unsupported`), or generating constructors via `MethodHandles` for case classes (call the primary
  constructor with parsed arguments instead of mutating an allocated blank).
- Writing `final` fields through `Field.set` (what deserializing a case class does by default) is the subject of
  JEP 500 ("Prepare to Make Final Mean Final", proposed for JDK 26): first a warning, later disallowed unless
  `--enable-final-field-mutation`. **Mitigation available**: `MPJsonsConfig(caseClassesViaConstructor = true)`
  builds case classes through their primary constructor (no final-field writes; constructor bodies run). It is
  opt-in because it boxes every constructor argument; making it the default is an owner decision once the JDK
  warning becomes relevant.
- `Field.setAccessible(true)` on application classes keeps working (unnamed module), but not on `java.*` (§16).

### 21. Miscellaneous (by inspection)

- `Iterable[T]` deserializes to a mutable `ArrayBuffer` exposed as `Iterable`; `Stream` is deprecated in 2.13 (`LazyList` is supported, read eagerly).
- `registerConverter` keys on `tpe.toString`; two different types with the same printed name (e.g. same simple name after `import`-dependent printing? no, `toString` is fully qualified for `TypeRef`) are unlikely but type aliases print as their alias.
- `TypesUtil.getClassFromType` uses the library's class loader; `getTypeFromClass` uses the class's loader; `deserialize(json, typeName)` uses `Class.forName(typeName)` with the caller-independent loader. Multi-classloader deployments (app servers, plugins) can fail to resolve application classes.
- `slf4j-api` is a declared runtime dependency but is never used.
- `checkFutureChar`, `hasNextChar`, `nextNonWhitespaceChar` in `StringIterator` and the `Java*Deserializer` objects in `values/` are unused but kept because they are public (the dead `NoSuchMethodException` catch in `ObjectConstructionUtil.createInstance` and the duplicate `Map` branch in the deserializer dispatch were removed).
- `IdentifierDeserializer` for bare keys stops at `:` or whitespace only; a bare key containing `}` or `,` would swallow them.
- Tuple7 and larger serialize as beans (`{"_7":..}`) rather than arrays like Tuple1..6.
- (removed) the duplicate `Map` check in `DeserializerFactoryImpl`.
