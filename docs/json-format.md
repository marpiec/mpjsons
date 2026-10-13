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

String escaping on write (`StringSerializer`): `"`→`\"`, `\`→`\\`, `/`→`\/` (**unnecessary, +1 byte per slash**), `\b \f \n \r \t`, other chars `< 0x20` → `\u00XX`, `\u0000` → **throws** `IllegalArgumentException("Null byte in String is not allowed")`. Everything else, including non-ASCII and surrogate pairs, is emitted as raw UTF-16 chars (**verified** `["😀"]`).

On read (`AbstractStringDeserializer.readString`): the eight short escapes plus `\uXXXX` (surrogate pairs reassembled, `\u0000` accepted); any other escape → error.

## 2. Beans (case classes and classes with fields)

```
{"<fieldName>":<value>,...}
```

- Key = the `@jsonName("...")` value if the field carries one (on the parameter, val/var or getter; no meta-annotation needed), otherwise the Scala field name; duplicate JSON names in one type fail at build time. Order = **declaration order**, with fields inherited from a superclass first (`case class Flat(a,b,c,d,e)` → `{"a":..,"b":..,"c":..,"d":..,"e":..}`; `class Derived extends Base` → base fields, then derived fields). Versions up to 0.6.51 wrote reverse declaration order; readers never depended on order.
- `null` field on write follows the instance's `NullPolicy` (`new MPJsons(nullPolicy = ...)`): `Fail` (default) throws `IllegalArgumentException("Null value is not allowed for field ...")` unless the field is `@(nullable @field)`, in which case the key is omitted; `Omit` (also `ignoreNullFields = true`) omits the key; `WriteNull` writes `"field":null`. `None` is always written as `{}` (not affected by the policy).
- Fields found by the reflection scan participate: own and inherited `var`/`val` backing fields; `lazy val` backing fields are excluded.
- Read: keys may be unquoted (`{ a : 1 }`), whitespace is allowed anywhere, unknown keys throw unless `ignoreNonExistingFields`, duplicate keys: last wins. Missing keys leave the field as constructed (null / 0 / false, or the initialiser value when a no-arg constructor ran), except `Option` fields, which read as `None`.
- A `null` **value** reads as `null` for reference fields (including `java.lang.Integer` and friends), `None` for `Option` fields, and fails with `JSON null is not allowed for primitive field 'x' of type Int in ...` for primitives.

## 3. Option / Either / tuples

| Type | Written | Example |
|---|---|---|
| `Option[T]` | `None` → `{}`; `Some(v)` → `{"value":<v>}` | `Some(1)` → `{"value":1}` (11 chars for one digit) |
| `Option[T]` with `MPJsonsConfig(compactOptions = true)` | bean field: `Some(v)` → `"f":<v>`, `None` → key omitted; elsewhere `Some(v)` → `<v>`, `None` → `null`; an Option directly inside an Option keeps the historical form | `Some(1)` → `1` |
| `Either[L,R]` | `{"left":<l>}` / `{"right":<r>}` | |
| `Tuple1..Tuple6` | JSON array `[a,b,...]` | `("x",2)` → `["x",2]` |
| `Tuple7+` | falls into bean handling → `{"_7":..,"_6":..,...,"_1":..}` (**verified** serialize; deserialize unverified) | |

`Option` read (both configurations): `null` → `None`; a value not starting with `{` → `Some(inline value)`; `{` → the historical `{}` / `{"value":v}` form, except that with `compactOptions = true` and an element type that is itself written as a JSON object (bean, `Option`, `Either`, singleton, time type) `{` is the inline element. `{"value":null}` → `Some(null)` (or `Some(None)` for `Option[Option[_]]`). `Some(null)` writes `{"value":null}` historically and `null` compactly (read back as `None`). Tuple slots and `Either` payloads accept and write `null` the same way.

## 4. Collections

| Type | Written | Read into |
|---|---|---|
| `List, Vector, Seq, Iterable, Stream, immutable.Queue, Set, HashSet, ListSet, Array[T]`, any `scala.` mutable `Seq/Set/Iterable` | JSON array | `List` → `List`; `Seq` → `List` (`Seq.from`); `Iterable` → **`ArrayBuffer`** (mutable, **verified**); `Set` → `Set(...)`; `Stream` via `Stream.fromIterator`; `Array` via reflective `java.lang.reflect.Array.set` per element. `mutable.ArrayBuffer/Buffer/Seq/IndexedSeq/Iterable` → `ArrayBuffer`; `mutable.Set/HashSet` → `mutable.HashSet`. Other mutable collections (`ListBuffer` by design) throw. |
| `BitSet` | array of ints | `immutable.BitSet` |
| `Map[K,V]` and all `scala.` map types incl. mutable | **array of pairs** `[[k,v],[k,v]]` | `Map` → `toMap`; `HashMap`; `ListMap` (order preserved, pairs reversed back); `mutable.Map/HashMap` → `mutable.HashMap`. |
| `SimpleMapSerializer[T]` (opt-in converter for `Map[String,T]`) | object `{"k":v}`; `null` values skipped | `SimpleMapDeserializer[T]`: object with quoted or bare keys |
| `SortedSet, TreeSet, SortedMap, TreeMap, ListBuffer` | serialize works for the sorted ones (they are `scala.` collections); deserialize **throws** by design (no `Ordering`) | |

Array/Map read loops accept missing and trailing commas (`[1 2 3]` → `List(1,2,3)`). A `null` element, key or value reads as `null` (`None` for `Option` elements) and fails for primitive element types; null elements are written as `null`.

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

All four can be replaced with `registerConverter` (e.g. ISO-8601 strings), in which case the built-in format is bypassed.

Sizes: a `LocalDateTime` is ~90 bytes vs 19-29 for ISO-8601 `"2023-08-23T14:30:17.000000125"`.

## 8. Parser leniency summary (**all verified**)

| Input | Behaviour |
|---|---|
| `{"a":1} garbage` | trailing content ignored |
| `{"zeta":1 "alpha":2}` | missing comma accepted |
| `{"a":1,}` / `[1,]` | trailing comma accepted |
| `{a:1}` | bare keys accepted |
| `{"a":1,"a":2}` | last wins |
| `""` (empty) | `StringIndexOutOfBoundsException`, not `JsonInnerException` |
| `"   "` | `JsonInnerException` (`Object should start with '{' symbol but was ''`) |
| `null` at top level | `null` for reference types, `None` for `Option`, error for primitives; `serialize(null)` writes `null` |
| deep nesting | recursive descent, no depth limit → `StackOverflowError` possible on hostile input |

There is no validation that the whole input was consumed. "Absent" and "present as `null`" differ only for
fields with a constructor default (absent keeps the default, `null` overwrites it) and for `Option` fields
(both give `None`).

## 9. Size inventory for a typical bean (what a "compact mode" could remove)

| Construct | Today | Possible compact form | Saving |
|---|---|---|---|
| `Some(5)` | `{"value":5}` (11) | `5` (1) — **available**: `compactOptions = true` | 10 bytes per present Option |
| `None` | `{}` (2) + key | omitted key — **available**: `compactOptions = true` | 2 + key length + colon + comma |
| `Map("k"->1)` | `[["k",1]]` (9) | `{"k":1}` (7) | 2 per entry + 2 |
| `"a/b"` | `"a\/b"` | `"a/b"` — **available**: `escapeSlash = false` | 1 per slash |
| `LocalDate` | 30 bytes | `"2023-07-23"` 12 bytes | 18 |
| field keys | full Scala field names | annotated short names (`@jsonName("n")`) | depends |
| default-valued fields (`0`, `false`, `""`, empty collections) | always written | omitted when equal to a known default | depends |
