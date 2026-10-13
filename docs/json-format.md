# JSON wire format reference

What `mpjsons 0.6.51` writes and what it accepts. Every row was derived from the serializer/deserializer
source; rows marked **verified** were additionally observed by running code. This is the baseline that
any size-minimisation or `null`-support change must stay compatible with (see roadmap.md §5).

## 1. Scalars

| Scala type | Written | Accepted on read | Notes |
|---|---|---|---|
| `Int, Long, Short, Byte` and boxed `java.lang.*` | `obj.toString` → `123`, `-5` | digits and `-` only (`isCurrentCharADigitPart`); then JVM `toInt`/`toLong` | `1.5` into an `Int` field stops at `.` and fails later with a misleading "Unexpected end of Json" (**verified**). Overflow → `NumberFormatException`. |
| `Double, Float` (+ boxed) | JVM text form → `0.75`, `1.0E10` | digits `- + . e E` then `toDouble`/`toFloat` | `NaN`/`Infinity` throw `IllegalArgumentException` on write (JSON cannot represent them). |
| `Boolean` (+ boxed) | `true` / `false` | lowercase letters collected, must equal `true`/`false` | |
| `Char`, `java.lang.Character` | one-char JSON string `"M"` | string of length 1 | |
| `String`, `mutable.StringBuilder` | JSON string, see escaping | JSON string | |
| `BigDecimal`, `BigInt`, `java.math.BigDecimal`, `java.math.BigInteger` | JSON number, exact (`toString`, so `1E+10` is possible) | the full number literal text | |
| `java.util.UUID`, `java.time.Instant`, `OffsetDateTime`, `ZonedDateTime` | JSON string (`toString`: `"2023-07-23T10:15:30.123Z"`, `"...+02:00[Europe\/Warsaw]"`) | `parse` / `UUID.fromString` of the string | `/` in zone ids follows `escapeSlash` |
| any other `java.*` type | **error** at (de)serializer build time naming the type (`use registerConverter`) | | replaces the `InaccessibleObjectException` the bean fallback used to throw under JPMS |

String escaping on write (`StringSerializer`): `"`→`\"`, `\`→`\\`, `/`→`\/` (**unnecessary, +1 byte per slash**), `\b \f \n \r \t`, other chars `< 0x20` → `\u00XX`, `\u0000` → **throws** `IllegalArgumentException("Null byte in String is not allowed")`. Everything else, including non-ASCII and surrogate pairs, is emitted as raw UTF-16 chars (**verified** `["😀"]`).

On read (`AbstractStringDeserializer.readString`): the eight short escapes plus `\uXXXX` (surrogate pairs reassembled, `\u0000` accepted); any other escape → error.

## 2. Beans (case classes and classes with fields)

```
{"<fieldName>":<value>,...}
```

- Key = the `@jsonName("...")` value if the field carries one (on the parameter, val/var or getter; no meta-annotation needed), otherwise the Scala field name; duplicate JSON names in one type fail at build time. On read, a renamed field also accepts its Scala field name as a fallback (JSON written before the rename still loads), unless another field owns that name as its JSON name. Order = **declaration order**, with fields inherited from a superclass first (`case class Flat(a,b,c,d,e)` → `{"a":..,"b":..,"c":..,"d":..,"e":..}`; `class Derived extends Base` → base fields, then derived fields). Versions up to 0.6.51 wrote reverse declaration order; readers never depended on order.
- `null` field on write follows the instance's `NullPolicy` (`new MPJsons(nullPolicy = ...)`): `Fail` (default) throws `IllegalArgumentException("Null value is not allowed for field ...")` unless the field is `@(nullable @field)`, in which case the key is omitted; `Omit` (also `ignoreNullFields = true`) omits the key; `WriteNull` writes `"field":null`. `None` is always written as `{}` (not affected by the policy).
- Fields found by the reflection scan participate: own and inherited `var`/`val` backing fields; `lazy val` backing fields are excluded.
- Read: keys may be unquoted (`{ a : 1 }`), whitespace is allowed anywhere, unknown keys throw unless `ignoreNonExistingFields`, duplicate keys: last wins. Missing keys leave the field as constructed (null / 0 / false, or the initialiser value when a no-arg constructor ran), except `Option` fields, which read as `None`.
- A `null` **value** is accepted only where it is declared: `None` for `Option` fields, `null` for reference fields annotated `@nullable` (including `java.lang.Integer` and friends). Under the default `NullPolicy.Fail` any other field rejects it with `JSON null is not allowed for field 'x' ... (declare it with @nullable or Option, ...)`; under `Omit` / `WriteNull` every reference field accepts it. Primitive fields never do.

## 3. Option / Either / tuples

| Type | Written | Example |
|---|---|---|
| `Option[T]` | `None` → `{}`; `Some(v)` → `{"value":<v>}` | `Some(1)` → `{"value":1}` (11 chars for one digit) |
| `Option[T]` with `MPJsonsConfig(compactOptions = true)` | bean field: `Some(v)` → `"f":<v>`, `None` → key omitted; elsewhere `Some(v)` → `<v>`, `None` → `null`; an Option directly inside an Option keeps the historical form | `Some(1)` → `1` |
| `Either[L,R]` | `{"left":<l>}` / `{"right":<r>}` | |
| `Tuple1..Tuple6` | JSON array `[a,b,...]` | `("x",2)` → `["x",2]` |
| `Tuple7+` | falls into bean handling → `{"_7":..,"_6":..,...,"_1":..}` (**verified** serialize; deserialize unverified) | |

`Option` read (both configurations): `null` → `None`; a value not starting with `{` → `Some(inline value)`; `{` → the historical `{}` / `{"value":v}` form, except that with `compactOptions = true` and an element type that is itself written as a JSON object (bean, `Option`, `Either`, singleton, time type) `{` is the inline element. `{"value":null}` → `Some(None)` for `Option[Option[_]]`; for other element types it is `Some(null)` only under `NullPolicy.Omit` / `WriteNull` and an error under `Fail`. `Some(null)` throws on write under `Fail`, and writes `{"value":null}` (historical) or `null` (compact, read back as `None`) otherwise. Tuple slots and `Either` payloads follow the same rule.

## 4. Collections

| Type | Written | Read into |
|---|---|---|
| `List, Vector, Seq, Iterable, Stream, LazyList, immutable.Queue, Set, HashSet, ListSet, Array[T]`, any `scala.` mutable `Seq/Set/Iterable` | JSON array | `List` → `List`; `Seq` → `List` (`Seq.from`); `Iterable` → **`ArrayBuffer`** (mutable, **verified**); `Set` → `Set(...)`; `Stream` via `Stream.fromIterator`; `LazyList` built eagerly; `Array` via reflective `java.lang.reflect.Array.set` per element. `mutable.ArrayBuffer/Buffer/Seq/IndexedSeq/Iterable` → `ArrayBuffer`; `mutable.Set/HashSet` → `mutable.HashSet`. Other mutable collections (`ListBuffer` by design) throw. |
| `BitSet` | array of ints | `immutable.BitSet` |
| `Map[K,V]` and all `scala.` map types incl. mutable | **array of pairs** `[[k,v],[k,v]]`; with `MPJsonsConfig(mapsAsObjects = true)` and a `String`/number/`Boolean`/`Char` key type: object `{"k":v}` (key text as JSON key; null key fails) | both forms always accepted (`[` → pairs, `{` → object, keys parsed back from their text; object form rejected for other key types). `Map` → `toMap`; `HashMap`; `ListMap` (input order); `mutable.Map/HashMap` → `mutable.HashMap`. |
| `SimpleMapSerializer[T]` (opt-in converter for `Map[String,T]`) | object `{"k":v}`; `null` values skipped | `SimpleMapDeserializer[T]`: object with quoted or bare keys |
| `SortedSet, TreeSet, SortedMap, TreeMap, ListBuffer` | serialize works for the sorted ones (they are `scala.` collections); deserialize **throws** by design (no `Ordering`) | |

Array/Map read loops accept missing and trailing commas (`[1 2 3]` → `List(1,2,3)`). A `null` element, key or value reads as `None` for `Option` element types; for other reference types it reads as `null` only under `NullPolicy.Omit` / `WriteNull` (error under `Fail`); primitives never accept it. On write, null elements, keys and values throw under `Fail` and are written as `null` otherwise.

## 5. Singleton objects

`object Foo` (and `case object`) serialize to `{}` (detection via `typeSymbol.isModuleClass`). Reading `{}` (whitespace allowed) for a `Foo.type` field or at top level returns the singleton instance itself; any key inside the object is an error (`SingletonObjectSpec`).

## 6. Polymorphic ("typed") values

After `markTypedClass[Base]`, a value whose **static** type is `Base` is written as `{"ConcreteSimpleName":{...fields...}}`; a value whose static type is already the concrete class is written plainly. Reading resolves `Class.forName(<package of Base>.<SimpleName>)`.

## 7. `java.time`

| Type | Written (fixed key order) | Read |
|---|---|---|
| `LocalTime` | `{"hour":12,"minute":30,"second":17,"nano":125}` | keys in any order, any whitespace; all 4 required (missing key reported by name); unknown keys rejected |
| `LocalDate` | `{"year":2023,"month":7,"day":23}` | all 3 required, same rules |
| `LocalDateTime` | `{"date":{...},"time":{...}}` | both required, same rules |
| `Duration` | `{"seconds":123,"nanos":456}` | both required; negative seconds OK |

With `MPJsonsConfig(isoTime = true)` all four are written as ISO-8601 strings instead (`"2023-07-23"`, `"12:30:17.000000125"`,
`"2023-08-23T14:30:17.000000125"`, `"PT2M3.000000456S"`, i.e. the `toString` of the `java.time` value). Both forms are always
accepted on read (`"` → `parse`, `{` → object form). All four can also be replaced with `registerConverter`.

Sizes: a `LocalDateTime` is ~90 bytes vs 19-29 for ISO-8601 `"2023-08-23T14:30:17.000000125"`.

## 8. Parser leniency summary

Default (lenient) behaviour; with `MPJsonsConfig(strict = true)` the first three rows are errors instead.

| Input | Behaviour |
|---|---|
| `{"a":1} garbage` | trailing content ignored (strict: `Unexpected content after the JSON value`) |
| `{"zeta":1 "alpha":2}` | missing comma accepted (strict: `Expected ',' or '}'`) |
| `{"a":1,}` / `[1,]` | trailing comma accepted (strict: `Trailing ','`) |
| `{a:1}` | bare keys accepted |
| `{"a":1,"a":2}` | last wins |
| `""` (empty) | `StringIndexOutOfBoundsException`, not `JsonInnerException` |
| `"   "` | `JsonInnerException` (`Object should start with '{' symbol but was ''`) |
| `null` at top level | `None` for `Option`; under `Fail` an error for every other type; under `Omit` / `WriteNull` `null` for reference types. `serialize(null)` throws under `Fail` and writes `null` otherwise |
| deep nesting | recursive descent, no depth limit → `StackOverflowError` possible on hostile input |

There is no validation that the whole input was consumed. "Absent" and "present as `null`" differ only for
fields with a constructor default (absent keeps the default, `null` overwrites it) and for `Option` fields
(both give `None`).

## 9. Size inventory for a typical bean (what a "compact mode" could remove)

| Construct | Today | Possible compact form | Saving |
|---|---|---|---|
| `Some(5)` | `{"value":5}` (11) | `5` (1) — **available**: `compactOptions = true` | 10 bytes per present Option |
| `None` | `{}` (2) + key | omitted key — **available**: `compactOptions = true` | 2 + key length + colon + comma |
| `Map("k"->1)` | `[["k",1]]` (9) | `{"k":1}` (7) — **available**: `mapsAsObjects = true` | 2 per entry + 2 |
| `"a/b"` | `"a\/b"` | `"a/b"` — **available**: `escapeSlash = false` | 1 per slash |
| `LocalDate` | 30 bytes | `"2023-07-23"` 12 bytes — **available**: `isoTime = true` | 18 |
| field keys | full Scala field names | annotated short names (`@jsonName("n")`) | depends |
| default-valued fields (`0`, `false`, `null`, `None`, constructor initialisers) | always written | omitted when equal to what reading `{}` yields — **available**: `omitDefaults = true` | depends |
