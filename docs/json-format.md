# JSON wire format reference

What `mpjsons 0.6.51` writes and what it accepts. Every row was derived from the serializer/deserializer
source; rows marked **verified** were additionally observed by running code. This is the baseline that
any size-minimisation or `null`-support change must stay compatible with (see roadmap.md §5).

## 1. Scalars

| Scala type | Written | Accepted on read | Notes |
|---|---|---|---|
| `Int, Long, Short, Byte` and boxed `java.lang.*` | `obj.toString` → `123`, `-5` | digits and `-` only (`isCurrentCharADigitPart`); then JVM `toInt`/`toLong` | `1.5` into an `Int` field stops at `.` and fails later with a misleading "Unexpected end of Json" (**verified**). Overflow → `NumberFormatException`. |
| `Double, Float` (+ boxed) | `obj.toString` → `0.75`, `1.0E10`, `NaN`, `Infinity` | digits `- + . e E` then `toDouble`/`toFloat` | `NaN`/`Infinity` are **not valid JSON** but are emitted (**verified**). |
| `Boolean` (+ boxed) | `true` / `false` | lowercase letters collected, must equal `true`/`false` | |
| `Char`, `java.lang.Character` | one-char JSON string `"M"` | string of length 1 | |
| `String`, `mutable.StringBuilder` | JSON string, see escaping | JSON string | |

String escaping on write (`StringSerializer`): `"`→`\"`, `\`→`\\`, `/`→`\/` (**unnecessary, +1 byte per slash**), `\b \f \n \r \t`, other chars `< 0x20` → `\u00XX`, `\u0000` → **throws** `IllegalArgumentException("Null byte in String is not allowed")`. Everything else, including non-ASCII and surrogate pairs, is emitted as raw UTF-16 chars (**verified** `["😀"]`).

On read (`AbstractStringDeserializer.readString`): the eight short escapes plus `\uXXXX` (surrogate pairs reassembled, `\u0000` accepted); any other escape → error.

## 2. Beans (case classes and classes with fields)

```
{"<fieldName>":<value>,...}
```

- Key = Java field name (Scala backing-field name). Order = **reverse declaration order** (**verified**: `case class Flat(a,b,c,d,e)` → `{"e":..,"d":..,"c":..,"b":..,"a":..}`). The JavaPrimitivesSpec expectation string encodes this order.
- `null` field: default throws `IllegalArgumentException("Null value is not allowed for field ...")`; with `@(nullable @field)` or `MPJsons(ignoreNullFields = true)` the key is **omitted**. A `null` literal is never written.
- Only fields found by the reflection scan participate: own `var`/`val` backing fields; **inherited fields are excluded** (bug, known-issues §2); `lazy val` backing fields are excluded (**verified**).
- Read: keys may be unquoted (`{ a : 1 }`), whitespace is allowed anywhere, unknown keys throw unless `ignoreNonExistingFields`, missing keys leave the field as constructed (null / 0 / false / `None`-less `null` for `Option`), duplicate keys: last wins (**verified**).
- A `null` **value** for a known field is **not accepted** for any type (**verified**: String → `String value should start with '"', but was [n]`, Option → `Object should start with '{' symbol but was 'n'`, Int → `NumberFormatException: For input string: ""`). It is accepted only while *skipping* an unknown field.

## 3. Option / Either / tuples

| Type | Written | Example |
|---|---|---|
| `Option[T]` | `None` → `{}`; `Some(v)` → `{"value":<v>}` | `Some(1)` → `{"value":1}` (11 chars for one digit) |
| `Either[L,R]` | `{"left":<l>}` / `{"right":<r>}` | |
| `Tuple1..Tuple6` | JSON array `[a,b,...]` | `("x",2)` → `["x",2]` |
| `Tuple7+` | falls into bean handling → `{"_7":..,"_6":..,...,"_1":..}` (**verified** serialize; deserialize unverified) | |

`Option` read: `{}` → `None`; `{"value":...}` → `Some`; any other key → error; `null` → error. `Some(null)` on write → raw `NullPointerException` (**verified**).

## 4. Collections

| Type | Written | Read into |
|---|---|---|
| `List, Vector, Seq, Iterable, Stream, immutable.Queue, Set, HashSet, ListSet, Array[T]`, any `scala.` mutable `Seq/Set/Iterable` | JSON array | `List` → `List`; `Seq` → `List` (`Seq.from`); `Iterable` → **`ArrayBuffer`** (mutable, **verified**); `Set` → `Set(...)`; `Stream` via `Stream.fromIterator`; `Array` via reflective `java.lang.reflect.Array.set` per element. Mutable collections: **no deserializer** (falls to bean logic and fails). |
| `BitSet` | array of ints | `immutable.BitSet` |
| `Map[K,V]` and all `scala.` map types incl. mutable | **array of pairs** `[[k,v],[k,v]]` | `Map` → `toMap`; `HashMap`; `ListMap` (order preserved, pairs reversed back). Mutable → NPE. |
| `SimpleMapSerializer[T]` (opt-in converter for `Map[String,T]`) | object `{"k":v}`; `null` values skipped | `SimpleMapDeserializer[T]`: object with quoted or bare keys |
| `SortedSet, TreeSet, SortedMap, TreeMap, ListBuffer` | serialize works for the sorted ones (they are `scala.` collections); deserialize **throws** by design (no `Ordering`) | |

Array/Map read loops accept missing and trailing commas (`[1 2 3]` → `List(1,2,3)`, **verified**). A `null` element is not accepted; `null` elements on write → raw NPE (**verified** `List("a", null)`).

## 5. Singleton objects

`object Foo` (and `case object`) serialize to `{}` (**verified**, detection via a `MODULE$` field). Reading a field declared as `Foo.type` crashes with `ClassCastException` (known-issues §3). Top-level `deserialize[Foo.type]` is likewise broken; the README states singletons are unsupported.

## 6. Polymorphic ("typed") values

After `markTypedClass[Base]`, a value whose **static** type is `Base` is written as `{"ConcreteSimpleName":{...fields...}}`; a value whose static type is already the concrete class is written plainly. Reading resolves `Class.forName(<package of Base>.<SimpleName>)`.

## 7. `java.time`

| Type | Written (fixed key order) | Read |
|---|---|---|
| `LocalTime` | `{"hour":12,"minute":30,"second":17,"nano":125}` | exactly 4 keys, any order, any whitespace; missing-key checks are dead code (`< -1`), so a missing key surfaces as `DateTimeException` |
| `LocalDate` | `{"year":2023,"month":7,"day":23}` | exactly 3 keys |
| `LocalDateTime` | `{"date":{...},"time":{...}}` | exactly 2 keys |
| `Duration` | `{"seconds":123,"nanos":456}` | exactly 2 keys; negative seconds OK |

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
| `null` at top level for a bean | error |
| deep nesting | recursive descent, no depth limit → `StackOverflowError` possible on hostile input |

There is no validation that the whole input was consumed, and no distinction between "absent" and
"present as `null`" on read; both of those are design inputs for the `null` work in roadmap.md.

## 9. Size inventory for a typical bean (what a "compact mode" could remove)

| Construct | Today | Possible compact form | Saving |
|---|---|---|---|
| `Some(5)` | `{"value":5}` (11) | `5` (1) | 10 bytes per present Option |
| `None` | `{}` (2) + key | omitted key | 2 + key length + colon + comma |
| `Map("k"->1)` | `[["k",1]]` (9) | `{"k":1}` (7) | 2 per entry + 2 |
| `"a/b"` | `"a\/b"` | `"a/b"` | 1 per slash |
| `LocalDate` | 30 bytes | `"2023-07-23"` 12 bytes | 18 |
| field keys | full Scala field names | annotated short names (`@jsonName("n")`) | depends |
| default-valued fields (`0`, `false`, `""`, empty collections) | always written | omitted when equal to a known default | depends |
