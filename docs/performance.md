# Performance: baseline, hot paths, and optimisation plan

## 1. How the numbers were obtained

In-process micro-benchmarks inside a ScalaTest spec (warm-up 20% of iterations, `System.gc()`, then a
`while` loop with `System.nanoTime`), JDK 25.0.0 (HotSpot, mixed mode), Windows 11, sbt 1.11.7, Scala 2.13.16,
13/10/2026. Each number is one run; a second run of the same cases differed by up to 2x in absolute terms
(JIT state, machine load), so **use ratios, not absolutes**. Before accepting or rejecting an optimisation,
add `sbt-jmh` and measure with JMH (`-prof gc` for allocation rate). The harness source is reproduced in §7.

Test object: `PerformanceSpec.SimpleDataObject` (11 fields: `Char, Long, Int, String, Boolean`, a nested bean,
two arrays, two lists, an empty array), serialized length 344 chars. `Flat` = `case class Flat(a: Int, b: Long, c: String, d: Boolean, e: Double)`, 40 chars.

## 2. Baseline (measured)

| Case | µs/op | ops/s | Notes |
|---|---|---|---|
| `MPJsons.serialize[T](sdo)` (TypeTag API) | 1.68 | 596 k | |
| `StaticSerializer.serialize(sdo)` | 0.89 | 1.13 M | per-call overhead of the TypeTag API ≈ 0.8 µs here |
| `StaticSerializer.serializeToStringBuilder(sdo)` | 0.89 | 1.12 M | the final `toString` copy is negligible at this size |
| `MPJsons.deserialize[T](json)` (TypeTag API) | 2.71 | 369 k | |
| `StaticDeserializer.deserialize(json)` | 2.72 | 368 k | ≈ 7.9 ns per input char |
| `Flat` serialize (static) | 0.23 – 0.49 | 2 – 4 M | two runs |
| `Flat` deserialize (static) | 0.55 – 0.91 | 1.1 – 1.8 M | ≈ 14 – 23 ns per char: per-field overhead dominates |
| `String` of 200 chars serialize / deserialize (static) | 0.54 / 0.99 | | ≈ 2.7 / 5 ns per char |
| `List[Int]` of 100 serialize / deserialize (static) | 2.36 / 4.35 | | ≈ 24 / 43 ns per element |

Per-call overhead decomposition on `Flat` (second run, same JIT session):

| Path | serialize µs | deserialize µs |
|---|---|---|
| TypeTag API (`serialize[T](obj)`) | 0.817 | 1.240 |
| Type API (`serialize(obj, tpe)`) | 0.597 | 1.098 |
| Static | 0.491 | 0.906 |
| `typeTag[Flat]` materialisation alone | 0.149 | |
| `t.typeSymbol.typeSignature.typeParams.zip(t.typeArgs).toMap` alone | 0.074 | |

Component micro-costs (same session):

| Operation | ns/op |
|---|---|
| `Field.get(obj)` on an `Int` field (boxes) / `Field.getInt` / `Field.get` on a `String` field | 12 / 12 / 11 |
| `StringBuilder.append(x: Any)` with a boxed Int / `append(int)` | 37 / 17 |
| `IdentifierDeserializer.deserialize("\"abc\":...")` + `Map.get` | 70 |
| `IntDeserializer` on `"1234,"` (new `StringIterator` each time, 8 ns of that) | 68 |
| `Integer.parseInt("1234")` alone | 7.7 |
| `StringDeserializer` on a 20-char string | 196 (≈ 10 ns/char) |
| `StringSerializer` on a 20-char string | 114 (≈ 5.7 ns/char) |
| `java.lang.StringBuilder.append(CharSequence, start, end)` 20 chars | 13 |

Reference point: a mature JSON library (Jackson, jsoniter-scala) parses at roughly 1-2 ns/char and binds a
5-field case class in ~0.1-0.2 µs. The current code is 3-5x off on strings and numbers and 5-10x off on
per-field overhead; the structure (direct `StringBuilder`/cursor, cached per-type objects) is sound, so these
gaps are addressable without an architectural rewrite.

## 3. Where the time goes (by component)

### 3.1 Per-call overhead in the facade (0.2 – 0.8 µs per call, cold-path code run on every call)

`MPJsons.serialize[T](obj)(tag)`:
1. **TypeTag materialisation** at the call site: scalac emits `TypeTag.apply(mirror, new TypeCreator{...})` for every
   invocation; ~0.15 µs. Unavoidable while the API takes an implicit `TypeTag` and the caller does not hoist it.
2. `extractTypeCache.getOrElse(tag, ...)`: the map lookup computes `tag.hashCode`/`equals`, which call `tag.tpe`, so the
   cache cannot avoid the `.tpe` computation it was written to avoid. Replace with plain `tag.tpe` (same cost, less code)
   or key by identity of the `TypeCreator` class (not exposed). Realistically: document `buildStatic*` as the hot-path API
   and make the remaining per-call cost small.
3. `SerializerFactoryMemoizer.getSerializer`: allocates `Context`, computes `typeSymbol.typeSignature.typeParams.zip(typeArgs).toMap`
   (0.07 µs, reflection calls), allocates the `(tpe, Boolean)` tuple, hashes a `Type`. **Fix**: look the cache up first (key on
   `tpe` identity or `(tpe, flag)`), and build `newContext` only on a miss. Expected: removes ~0.1 µs/call from both APIs.
4. `new StringBuilder()` default capacity 16 → several reallocations for a 344-char output. **Fix**: size hint per type
   (remember the last output length per `StaticSerializer`/per serializer and pre-size), or reuse a thread-local builder in
   `serializeToStringBuilder` callers.

### 3.2 Deserialization hot loop

`StringIterator` (`impl/StringIterator.scala`): `nextChar()` does two comparisons, a bounds-checked `charAt`, and two field
writes per character; every reader calls it through a method on a shared mutable object. Keep the class (public API), but:
- hold the input as a `char[]` or keep `String` and avoid double bounds checks; make the end-of-input a single sentinel path;
- let the heavy readers (`readString`, numbers, identifiers) run **local-variable loops** over the underlying string/array with
  an index, and write `nextIndex`/`currentChar` back once at the end. That is where the 10 ns/char goes.

`readString` (`AbstractStringDeserializer.scala:11-62`): allocates a Scala `StringBuilder` (wrapper + `java.lang.StringBuilder`),
appends char-by-char, then `toString`. **Fix**: scan for the closing quote or a backslash; if no escape is found, return
`stringValue.substring(start, end)` (one allocation, intrinsic copy); otherwise bulk-append runs via
`underlying.append(CharSequence, start, end)` (Scala's `StringBuilder` has **no** 3-arg range append; use `.underlying`, which is public).
Expected: 196 ns → 30-40 ns for 20 chars.

Numbers (`AbstractIntegerDeserializer`, `AbstractFloatingPointDeserializer`): `StringBuilder` + `toString` + `parseInt`. `parseInt`
itself is 8 ns; the rest is allocation. **Fix**: accumulate `Int`/`Long` directly from chars with overflow checks (emit the same
`NumberFormatException` message on overflow to keep behaviour); for `Double`/`Float` scan the extent and call
`java.lang.Double.parseDouble(stringValue.substring(start, end))` (one allocation, no builder), or a fast path for pure integers.
Also validate the first char and raise "expected number but found 'x'" to fix the misleading errors (known-issues §12).

Identifiers (`IdentifierDeserializer` + `fieldsByName: immutable.Map[String, ...]`): builds a `String` per key, then hashes it
(`String.hashCode` is cached per instance, so it is recomputed for every fresh key string) and probes an immutable `HashMap`.
**Fix options**, in increasing ambition: (a) keep the `String` but use a `java.util.HashMap` and pre-size; (b) compute a hash while
scanning and match against a small open-addressing table of known keys, comparing chars in place (zero allocation on the happy path);
(c) a `switch` on key length + first char generated per type. (b) is the usual sweet spot.

`BeanDeserializer.deserialize`: `field.set(instance, boxedValue)` for primitives boxes the value returned by the primitive deserializer
(`JsonTypeDeserializer[AnyRef]` cast) and unboxes in `Field.set`. **Fix**: per-field "setter" objects that call `field.setInt/setLong/...`
or `Unsafe.putInt(obj, offset, v)` (Unsafe is already a hard dependency) or `MethodHandle` setters (`MethodHandles.privateLookupIn` +
`unreflectSetter`, invoked `invokeExact` from generated adapters); specialised `JsonTypeDeserializer` subtypes with `deserializeInt`
etc. to avoid boxing end-to-end. Also `skipWhitespaceChars` is called redundantly (before identifier, after value, inside `consume*`).

Collections: `AbstractJsonArrayDeserializer` fills an `ArrayBuffer[T]` (boxing primitives) then copies (`List.from`, `Vector.from`,
`Set(...: _*)`, `HashSet.from`). `ArrayDeserializer.toArray` converts the buffer to a `List` and sets elements with
`java.lang.reflect.Array.set` one by one (reflective, boxing). **Fix**: build the target directly (`List.newBuilder`,
`Vector.newBuilder`, `HashSet.newBuilder`); for arrays allocate with the cached element `Class` and use a `ClassTag`-typed
`Array` or `System.arraycopy` from a primitive buffer. `AbstractJsonMapDeserializer` builds a `List[(K,V)]` then `toMap`: use
`Map.newBuilder` / `HashMap.newBuilder`.

`OptionDeserializer`: parses an object with an identifier each time (`{"value":..}`); format-level fix in roadmap.md §4.

### 3.3 Serialization hot loop

`BeanSerializer.serialize`: `for (field <- fieldsWithSerializers)` is `ArrayOps.foreach` with a closure (fine after inlining, but a
`while` loop with an index is cheaper and allocation-free); `field.field.get(obj)` boxes primitives; `append('"').append(name).append('"').append(':')`
is four calls, replace with a precomputed `",\"name\":"` / `"\"name\":"` string per field; the `isNotFirstField` branch disappears if the
first non-null field uses the prefix without the comma (keep a flag only for the null-skipping case).

`SimpleToStringSerializer.serialize(obj: Any)` does `StringBuilder.append(Any)` → `String.valueOf` → one `String` allocation + copy per
number (37 ns vs 17 ns for `append(int)`). **Fix**: typed serializers (`IntSerializer` appends `int`, `LongSerializer` appends `long`,
booleans append a constant) and typed field access (`Field.getInt`) so no boxing happens. `Double`/`Float` `toString` cannot be avoided
cheaply but can go through `append(double)` (Java's `StringBuilder.append(double)` is allocation-free since JDK 19 (Ryu-like algorithm)).

`StringSerializer`: per-char `match` with eight cases plus two guards, `append(char)` per char, `"\\u" + toHex(c)` allocations for control
chars. **Fix**: scan for the next char that needs escaping (`< 0x20`, `"`, `\`; drop the `/` escape to save bytes), bulk-append the safe
run via `underlying.append(s, start, i)`, use a 32-entry precomputed escape table for control chars. Expected: 114 ns → ~25 ns for 20 chars.

`IteratorSerializer.serializeIterator` allocates an `Iterator` and a closure per collection; for `List` walk with `while (xs ne Nil)`,
for `Array` index loop, for `Vector` `foreach` is fine.

Scala `StringBuilder` wrapper: every `append` goes through `scala.collection.mutable.StringBuilder` → `java.lang.StringBuilder`. Keep the
public signature (`jsonBuilder: StringBuilder`) but let hot serializers grab `jsonBuilder.underlying` once per call.

### 3.4 Cold path (do not optimise for throughput)

The dispatch chains in `*FactoryImpl` and the reflection scan run **once per type**. The two "Optimization by claude" commits
(`2bfcbb2`, `6442f3c`, both reverted) only rewrote those chains (pre-computed symbol tables, `baseClasses` hoisting). Whatever the
reason for the revert, such a change cannot move steady-state throughput at all, and `PerformanceSpec` cannot detect either a win or
a regression (its 500 ms budget for 5000 round-trips is ~20x the actual cost). Do not repeat that work; spend the effort on §3.1-3.3.

## 4. Prioritised plan with expected effect

Ordered by (gain × confidence) / risk. Gains are estimates from the component numbers above; verify with JMH.

| # | Change | Expected effect | Risk / notes |
|---|---|---|---|
| 0 | Add `sbt-jmh` + a `bench/` module with the harness cases from §7 (serialize/deserialize `SimpleDataObject`, `Flat`, strings, `List[Int]`, nested generics); run `-prof gc`. | Enables every other row. | None; also replaces the meaningless `PerformanceSpec` assertion. |
| 1 | Memoizer: cache lookup before `Context` construction; `MPJsons.extractType` → `tag.tpe`. | −0.1…0.2 µs per `serialize`/`deserialize` call (10-25% on small objects). | None; purely internal. |
| 2 | `readString` fast path: substring when no escapes, bulk-append runs otherwise; `StringSerializer` run-based escaping with lookup table. | Strings 3-5x faster; typical bean deserialize −20-30%. | Keep exact escape semantics (`\u0000` rejection on write, `\uXXXX` on read). |
| 3 | Allocation-free integer parsing; `parseDouble` on substring; proper type-mismatch errors. | Numbers 3-5x faster; fixes known-issues §12. | Preserve overflow → `NumberFormatException` wrapped in `JsonInnerException` (tests may rely on the exception type only). |
| 4 | Identifier matching without `String` allocation (hash while scanning + in-place compare against known keys); `java.util.HashMap` fallback for unknown keys. | −50-70 ns per field. | Must keep bare-key and quoted-key semantics; escapes in quoted keys need the slow path. |
| 5 | Typed primitive (de)serializers + `Field.getX/setX` (or `Unsafe` offsets / `MethodHandle` setters) in bean code; precomputed `"name":` prefixes; `while` loops. | −30-50% on flat beans. | `MethodHandle` setters require `privateLookupIn` (JDK 9+); `Unsafe` path is simplest and already present. |
| 6 | Collection builders instead of buffer-then-copy; non-reflective array construction. | Collections 1.5-2x. | None. |
| 7 | Output `StringBuilder` pre-sizing (last length per serializer) and `underlying` usage. | −5-10% serialize, fewer GC pauses. | None. |
| 8 | Format-level: Option inline, Map-as-object for String keys, ISO times (roadmap.md §4). | Smaller output and fewer parse steps. | **Wire-format change**: opt-in or versioned. |

A realistic target after rows 1-7: deserialize ≈ 2.5-3x faster, serialize ≈ 2-3x faster on the `SimpleDataObject` case, with
allocation per call reduced to the result object graph plus one output string.

## 5. Constraints to respect while optimising

- Public API: `JsonTypeSerializer.serialize(obj, jsonBuilder: scala.collection.mutable.StringBuilder)`,
  `JsonTypeDeserializer.deserialize(jsonIterator: StringIterator)`, and the `StringIterator` members used by custom converters
  in the wild (`currentChar`, `nextChar`, `skipWhitespaceChars`, `consume*`, `nextIndex`) must keep working. Add faster internal
  entry points next to them rather than replacing them.
- Behavioural compatibility the tests pin: whitespace and bare keys accepted, unknown-field skipping (including `null` values),
  `\uXXXX` round trip, scientific notation, negative `Duration`, exact output strings in `TimeSpec`, `TupleSpec`,
  `JavaPrimitivesSpec` (field order!), `TypedConverterSpec`, `UtfControlCharacterSpec`.
- Decide explicitly which leniencies (missing/trailing commas, trailing garbage) to keep; a stricter loop is *cheaper*, not more expensive,
  but it is a behaviour change.
- Do not touch the dispatch chains for performance reasons (§3.4).

## 6. Measurement checklist for a PR

1. JMH, `-wi 5 -i 5 -f 2`, modes `avgt` with `-prof gc`; report before/after for serialize and deserialize of `SimpleDataObject`, `Flat`, `String(200)`, `List[Int](100)`.
2. `sbt test` green.
3. Serialized output for the test fixtures byte-identical unless the PR is a declared format change.

## 7. Harness used for the numbers above (reproduce in a scratch spec or a JMH module)

```scala
def time[A](label: String, iters: Int)(f: => A): Unit = {
  var i = 0; while (i < iters / 5) { f; i += 1 }          // warm-up
  System.gc()
  val t0 = System.nanoTime(); i = 0
  while (i < iters) { f; i += 1 }
  val perOp = (System.nanoTime() - t0).toDouble / iters
  println(f"[BENCH] $label%-55s ${perOp / 1000}%9.3f us/op")
}
val mpjsons = new MPJsons
val staticSer = mpjsons.buildStaticSerializer[SimpleDataObject]
val staticDes = mpjsons.buildStaticDeserializer[SimpleDataObject]
val json = staticSer.serialize(sdo)
time("serialize TypeTag API", 200000) { mpjsons.serialize(sdo) }
time("serialize static",      200000) { staticSer.serialize(sdo) }
time("deserialize TypeTag API", 200000) { mpjsons.deserialize[SimpleDataObject](json) }
time("deserialize static",      200000) { staticDes.deserialize(json) }
```

(`sdo` is built exactly as in `PerformanceSpec`.)
