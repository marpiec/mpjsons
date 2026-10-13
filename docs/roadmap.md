# Roadmap and design notes for the planned work

Planned themes (from the owner, 13/10/2026):

1. raw performance of serialization and deserialization;
2. smaller generated JSON, including annotating how fields are named in JSON;
3. support for the `null` literal.

This file turns the analysis in the other documents into an ordered plan with design options and the
decisions that only the owner can take. Nothing here is implemented.

## 1. Bird's-eye assessment

The architecture (reflection once per type, cached (de)serializer trees, direct `StringBuilder`/cursor I/O)
is the right shape for a fast library and should be kept. The weaknesses are concentrated in four places:

- **Correctness of type handling**: generic cache key (known-issues §1), inherited fields (§2), non-`TypeRef`
  types (§3), interface targets (§4). Two of these silently produce wrong data, so they come before any feature.
- **Hot loops** allocate a `StringBuilder` and a `String` per value and per key, box every primitive twice, and
  step the cursor through a mutable object one char at a time (performance.md §3).
- **Wire format** is self-describing but verbose (`Option` as object, `Map` as pair-array, `java.time` as
  objects, escaped `/`) and has no representation of `null` at all (json-format.md §9).
- **Robustness**: lenient parsing with no end-of-input check, raw exceptions leaking (`StringIndexOutOfBounds`, NPE),
  unsynchronised caches.

Recommended order: fix correctness (§2) → build the benchmark harness and do the allocation-free hot loops
(performance.md §4 rows 0-7) → `null` support (§3) → naming and compact format (§4), because the format work
should land on top of the new parser loops, not be written twice.

## 2. Prerequisite fixes (small, independent PRs, each with a spec)

### 2.1 Generic cache key
Substitute type parameters in `tpe` before using it as the memoizer key (`*FactoryMemoizer.scala`). Spec: `Box[Int]`
then `Box[String]` on one instance, serialize and deserialize. Also key `postTransform` and converter registries on the
same substituted type.

### 2.2 Inherited fields
Collect fields across `baseClasses` with types as seen from the concrete type. Spec: `class Derived extends Base`
round trip; generic superclass `class Base[T] { var t: T }`.

### 2.3 Declaration-order fields
Use `tpe.members.sorted` (or per-class `decls` in `baseClasses` order, base first). Update `JavaPrimitivesSpec`'s
expected string. **Decision for the owner**: this changes every multi-field output string. It is harmless for JSON
consumers but changes hashes/diffs of stored documents. If persisted JSON is ever compared textually, ship it
together with the compact-format version bump (§5).

### 2.4 Robustness
- `StringIterator` on empty input → `JsonInnerException` (move construction inside the `try`, or accept `""` as "no value").
- Guard the `Array` check in `DeserializerFactoryImpl` with `isInstanceOf[TypeRefApi]`.
- `getAccessibleField`: stop when `getSuperclass == null`.
- Fail fast with a clear message for `java.*` types without converters and for mutable collections on deserialize
  (or add `ArrayBuffer`, `ListBuffer`, `mutable.HashMap`, `mutable.HashSet`, `mutable.Map`, `mutable.Set` deserializers).
- Duration override guard; dead time-field checks.
- Thread-safety: `ConcurrentHashMap.computeIfAbsent` for all caches; registries frozen after first use or made concurrent.

## 3. `null` literal support — design

### 3.1 Semantics to decide (owner decisions marked ◆)

| Situation | Proposed behaviour | Alternative |
|---|---|---|
| Read `"f":null` into `AnyRef` field (String, bean, collection, boxed Java number) | set `null` | ◆ or treat as "absent" (keep constructed value) |
| Read `"f":null` into `Option[T]` field | `None` | |
| Read `"f":null` into primitive field (`Int`, `Boolean`, ...) | error with a clear message (`null is not allowed for primitive field f`) | ◆ or default (0/false) when `lenientNulls = true` |
| Read `null` as an element of a collection / map value / tuple slot / `Either` payload | `null` if element type is `AnyRef`, `None` if `Option`, error if primitive | |
| Read `null` as the root value | return `null` (for `AnyRef`/`Option` roots `None`) | |
| Absent `Option` field | `None` (new) instead of `null` (today) | ◆ this is a behaviour change users may rely on via `postDeserializationTransform` |
| Write `null` field, default | throw (unchanged) | |
| Write `null` field with `@nullable` / `ignoreNullFields` | omit key (unchanged) | |
| Write `null` field, new policy `writeNulls` | emit `"f":null` | ◆ instance-level flag `MPJsons(nullPolicy = Fail | Omit | WriteNull)` replacing the boolean |
| Write `None` | `{}` today → under compact format omit the key or write `null` (§4.2) | |
| Write `null` collection element / `Some(null)` | `null` literal (replaces today's NPE) | ◆ or throw with a proper message |

Recommended defaults: read accepts `null` everywhere it is representable (`AnyRef` → `null`, `Option` → `None`,
primitive → error); write keeps `Fail` as default and adds `WriteNull` as an explicit policy. This is backwards
compatible for every input that parses today (nothing that parses today contains `null` in a known field) and for
every output.

### 3.2 Implementation sketch

Reading: the cheapest place is a single check at **value entry points**, not inside every deserializer:

```scala
// in BeanDeserializer / array / map / tuple / Either / Option loops, after skipWhitespaceChars():
if (it.currentChar == 'n') { it.expectLiteral("null"); value = field.nullValue /* null | None | throw */ }
else value = deserializer.deserialize(it)
```

- Precompute per field/element a `NullHandling` enum at build time from the `Type`: `Reference` (`tpe <:< AnyRef` and
  not a Java boxed type? boxed types are `AnyRef` too, so they accept null), `OptionType`, `Primitive`.
- Alternatively wrap deserializers in a `NullableDeserializer(inner, nullValue)` chosen by the factory; this keeps the
  loops untouched but adds a virtual call per value. The entry-point check is cheaper and also fixes the "misleading
  error" problem by validating the first char.
- `expectLiteral` must verify all four chars (`nul` followed by anything else is an error) and not treat `nullable`-like
  bare identifiers as null when reading **keys** (keys are read by `IdentifierDeserializer`, unaffected).
- `Option` absent → `None`: after instance creation, assign `None` to all `Option` fields (precomputed list) before the
  loop; cost is one `Field.set` per Option field per object.
- Top-level: `MPJsons.deserialize` checks for the `null` literal before dispatch.

Writing: add `NullSerializer` behaviour at the same entry points (bean fields, collection elements) controlled by the
instance policy: `Fail` (throw with field path), `Omit` (beans only; for collections `Omit` is not meaningful, so fall
back to `WriteNull` or `Fail` ◆), `WriteNull` (append `null`). Keep `@nullable` meaning "may be omitted" and consider
`@nullable(write = true)` to force `null` emission per field.

### 3.3 Tests to add
`NullLiteralSpec`: each row of the table in §3.1, plus `ignoreNonExistingFields` + null, nested `Option[Option[String]]`
with `null` at both levels, `List[String]` with `null`, `Map[String, String]` with `null` value, root `null`, and the
serialize policies.

## 4. Field naming and JSON size — design

### 4.1 `@jsonName` (or `@JsonField(name = "...")`)

- Annotation: `final class jsonName(name: String) extends StaticAnnotation`. Read at build time in the field scan.
- **Lookup must not require `@field` meta**: on a case class `@jsonName("n") a: String` puts the annotation on the constructor
  parameter (verified for `@nullable`). Resolve annotations from (a) the backing field symbol, (b) the primary constructor
  parameter with the same name (`typeSymbol.asClass.primaryConstructor.asMethod.paramLists.head`, verified available),
  (c) the getter. Apply the same resolution to `@nullable` to remove that footgun.
- Validate at build time: names unique within the class, non-empty, no need to escape (or escape when writing).
- `BeanSerializer`: precompute `prefixFirst = "\"n\":"` and `prefixNext = ",\"n\":"` per field.
- `BeanDeserializer`: key table keyed by JSON name (plus, optionally ◆, the Scala name as an alias for reading old data).
- Interaction with `TypedConverter`: type keys use `getSimpleName`; add `@typeName("T")` or a registration-time alias so the
  wrapper key can be short too (also fixes known-issues §10 if the alias is registered explicitly: `markTypedClass[Base](Map("T" -> classOf[TextDescription]))`).

### 4.2 Compact representations (each independently switchable, all default **off** until a major version)

| Lever | Compact form | Read compatibility |
|---|---|---|
| `Option` | `Some(v)` → `v`; `None` → key omitted (beans) or `null` (elements) | deserializer accepts `{}`/`{"value":v}` **and** bare value/`null` (ambiguous only if `T` is itself a bean whose only field is `value`; detect and reject at build time ◆) |
| `Map[String, V]` and `Map[K, V]` with `K` ∈ {String, Int, Long, Boolean, Char, UUID} | JSON object `{"k":v}` with key `toString`/parse | accept both `[[k,v]]` and `{}` on read (first char decides) |
| `java.time` | ISO-8601 strings; `Duration` as ISO `PT1M30S` or seconds+nanos string | accept object form too |
| String escaping | stop escaping `/` | no read impact |
| Default-valued fields | omit when value equals a per-class default: for case classes use `apply$default$N` or an instance built by the no-arg constructor; for primitives `0/false` with `@omitDefault` opt-in | missing → default must then be applied on read (reuse the §3.2 "pre-initialise" step: construct via no-arg constructor or copy defaults from a cached template instance) |
| `Double` integral values | `1.0` → `1` | lossless for `Double`; keep `1.0E10` style for large values; ◆ |
| Type tags | short aliases (§4.1) | |
| Numbers as `Long`/`BigInt` | n/a | |

Together with `@jsonName`, `Option` inlining and `Map` objects typically shrink bean JSON by 30-50%; the exact figure
depends on the share of Options and maps in the model.

### 4.3 Configuration shape

Replace the two booleans with a config case class to avoid a parameter explosion:

```scala
final case class MPJsonsConfig(
  ignoreNonExistingFields: Boolean = false,
  nullPolicy: NullPolicy = NullPolicy.Fail,          // Fail | Omit | WriteNull
  compactOption: Boolean = false,
  mapsAsObjects: Boolean = false,
  isoTime: Boolean = false,
  omitDefaults: Boolean = false,
  escapeSlash: Boolean = true,
  strict: Boolean = false)                           // reject missing/trailing commas, trailing garbage
class MPJsons(config: MPJsonsConfig = MPJsonsConfig()) { def this(ignoreNonExistingFields: Boolean, ignoreNullFields: Boolean) = ... }
```

Keep the old constructor for source compatibility. Because format options change the (de)serializer *tree*, they must be
part of what the factory builds (pass the config down, not read at call time) so the hot path has no flag checks.

### 4.4 Compatibility strategy for persisted JSON

Commit history (`a2edb13`, PostgreSQL `jsonb`) shows the output is stored. Therefore:
- readers must accept **both** old and new forms for every lever in §4.2 (the table lists how);
- writers switch only via config; flipping defaults is a major version (`0.7.0`), documented in `README.md`;
- add a `FormatCompatibilitySpec` that keeps fixture strings of the 0.6.x format and asserts they still deserialize.

## 5. Suggested milestone plan

| Milestone | Contents | Status (13/10/2026) |
|---|---|---|
| M1 Correctness | §2.1-2.4 | **Done** (field order switched to declaration order on 13/10/2026); the converter registries remain registration-time `var Map`s. Known-issues §1-§5, §7-§12 (numbers), §17-§19 fixed with specs. |
| M2 Bench + hot loops | performance.md rows 0-7 | **Done**: JMH module, memoizer/caches, strings, numbers, key matching, typed field access, collection builders, primitive serializers, output pre-sizing. Output byte-identical. |
| M3 Null | §3 | **Done** (13/10/2026) with decisions 1-3 as recommended: read `null` → null / `None` / error for primitives everywhere, absent `Option` → `None`, write policy `NullPolicy.Fail | Omit | WriteNull` (default `Fail`), null elements always written as `null`. `NullLiteralSpec`. |
| M4 Naming + compact | §4.1, §4.2, §4.3, §4.4 | §4.1 `@jsonName` **done** (13/10/2026, `JsonNameSpec`; `@nullable` no longer needs `@field`). §4.3 `MPJsonsConfig` **done**. §4.2 levers: `escapeSlash`, `compactOptions`, `mapsAsObjects` and `isoTime` **done** (opt-in, `CompactFormatSpec`); `strict` parsing **done** as an opt-in lever (`StrictParsingSpec`); `omitDefaults` **done** (`OmitDefaultsSpec`: compares against the instance that reading `{}` produces). Defaults for 0.7 need decision 5 in §6. |
| M5 Hardening | JPMS-safe construction path (constructor-based for case classes), `LazyList`, `BigDecimal`/`UUID`/`Instant` converters, `TypedDeserializer` nested-class resolution (known-issues §10), deterministic superclass-converter matching (§11), drop `slf4j-api` | **Done**; the constructor-based construction path exists as the opt-in `caseClassesViaConstructor` lever (`ConstructorConstructionSpec`), its default is an owner decision tied to the JDK roadmap: `LazyList` (`LazyListSpec`), `BigDecimal`/`BigInt`/`UUID`/`Instant`/`OffsetDateTime`/`ZonedDateTime` plus fail-fast for other `java.*` types (`ExtraTypesSpec`), §10, §11, `slf4j-api` removed. |

## 6. Open decisions for the owner (collect answers before M3/M4)

1. Read-side `null` for primitives: error (recommended) or zero/false?
2. Absent `Option` field → `None` (recommended) is a behaviour change; acceptable?
3. Default write policy for `null`: keep `Fail` (recommended) or move to `Omit`?
4. Field order change to declaration order: now, or only with the compact-format major version?
5. Which compact levers become defaults in `0.7`, if any?
6. Is strict parsing (reject missing/trailing commas, trailing garbage) acceptable as a default, or opt-in only?
7. Case-class construction via the primary constructor (fixes defaults, JEP 500 risk) versus keeping `Unsafe` allocation (fastest, no argument marshalling): worth doing in M5 or defer?
