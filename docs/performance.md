# Performance: harness, results, remaining opportunities

## 1. Harness

JMH via `sbt-jmh`, benchmarks in `src/jmh/scala/io/mpjsons/bench/MpjsonsBenchmark.scala`:

```bash
sbt "Jmh/run -f2 -wi 3 -w 1 -i 5 -r 1 -prof gc .*MpjsonsBenchmark.*"
```

- Quick check while iterating: `-f1 -wi 3 -w 1 -i 5 -r 1`, optionally a name filter such as `.*MpjsonsBenchmark.(flat|sdo).*`.
- `-prof gc` prints `gc.alloc.rate.norm` (bytes allocated per operation). On a shared or throttled machine this is the
  **reliable** number: it does not depend on load. Timings on the laptop used here varied by up to 3x between runs
  taken minutes apart (error bars up to ±1.6 µs), so always compare timings from runs taken under the same conditions,
  and prefer allocation deltas for small changes.
- Quirk: the first `Jmh/run` after adding a benchmark class may report "No matching benchmarks" because the bytecode
  generator ran before the new class was compiled. Run `sbt Jmh/compile` once and retry.
- `PerformanceSpec` (ScalaTest, 500 ms for 5000 round trips) is kept for compatibility but cannot detect regressions.

Fixtures: `SimpleDataObject` ("sdo", 11 fields incl. nested bean, two arrays, two lists; 344 chars), `Flat`
(5 primitive/String fields; 40 chars), `Person` (nested case class with `List`, `Option`, `Map`; ~150 chars),
a 225-char escape-free string, `List[Int]` of 100.

## 2. Results

Baseline = commit `a362f8e` (correctness fixes and the harness, no performance work). Current = after commits
`3c7feb5`..`be666f0` (steps 1-7 below). JDK 25.0.0 HotSpot, Windows 11, Scala 2.13.16, 13/10/2026.

### 2.1 Allocation per operation (two forks, stable to ±0.1%)

| Benchmark | Baseline B/op | Current B/op | Reduction |
|---|---:|---:|---:|
| flat deserialize (static) | 1 192 | 224 | 81% |
| flat serialize (static) | 464 | 192 | 59% |
| List[Int](100) deserialize | 11 480 | 2 448 | 79% |
| List[Int](100) serialize | 6 448 | 704 | 89% |
| Person deserialize | 2 804 | 720 | 74% |
| Person serialize | 1 220 | 516 | 58% |
| sdo deserialize (static) | 4 816 | 1 256 | 74% |
| sdo deserialize (TypeTag API) | 4 988 | 1 352 | 73% |
| sdo serialize (static) | 2 484 | 936 | 62% |
| sdo serialize (TypeTag API) | 2 580 | 1 032 | 60% |
| String(225) deserialize | 984 | 272 | 72% |
| String(225) serialize | 952 | 576 | 39% |

What remains is essentially the result object graph plus one output `String` (serialize) or the input
`StringIterator` plus the deserialized objects (deserialize).

Re-measured after the feature work that followed (`null` literal support, `@jsonName`, `MPJsonsConfig` with the
`escapeSlash`/`compactOptions`/`mapsAsObjects`/`isoTime` levers, declaration-order keys), commit `d7d2acc`, single fork:
flat 224 / 208, `List[Int]` 2 464 / 720, Person 688 / 552, sdo 1 256 / 952, String(225) 272 / 592 B/op
(deserialize / serialize). The per-value `null` check and the per-field policy dispatch cost no allocation; the
small serialize-side increases (+16 to +36 B) come from the wider output-buffer margin (see step 7) and the
`Option` field writer.

### 2.2 Time per operation

Baseline timings are from the two-fork run (error bars ±3-10% except where noted). Current timings are from the
single-fork runs taken while the machine was quiet (error bars ±2-5%; the later two-fork run on the current tree
coincided with heavy background load and its timings were 2-3x higher with ±50% error bars, so they are not used).
Treat the ratios as indicative; re-run both trees back-to-back on a quiet machine before quoting them.

| Benchmark | Baseline ns/op | Current ns/op (quiet runs) | Speed-up |
|---|---:|---:|---:|
| flat deserialize (static) | 632 | 119 | ~5x |
| flat serialize (static) | 381 | 100 | ~4x |
| List[Int](100) deserialize | 5 399 | 1 008 | ~5x |
| List[Int](100) serialize | 2 664 | 876-1 098 | ~2.5-3x |
| Person deserialize | 1 691 | 324 | ~5x |
| Person serialize | 857 | 238 | ~3.5x |
| sdo deserialize (static) | 3 862 | 648 | ~6x |
| sdo serialize (static) | 899 (±624) | 477 | ~2x |
| String(225) deserialize | 1 319 | 115 | ~10x |
| String(225) serialize | 1 078 | 161 | ~6x |

Per input character the deserializer is now at roughly 2 ns (sdo: 648 ns / 344 chars), which is in the range of
mature JSON libraries; the remaining per-field cost is reflection (`Field.setX` with access checks) and construction.

## 3. What was done (one commit each, all output byte-identical, `sbt test` green)

| Step | Commit | Change |
|---|---|---|
| 1 | `3c7feb5` | Memoizer looks the cache up before building a `Context`; all caches `ConcurrentHashMap`; `extractType` = `tag.tpe`. |
| 2 | `69805e1` | `readString` substring fast path and run-based escape handling; `StringSerializer` bulk runs + escape table; `StringIterator.seek`. |
| 3 | `f0e2918` | Integer accumulation from chars with overflow detection; `parseDouble` on one substring with an integer fast path; "Expected a number" errors. |
| 4 | `ceb7b02` | `BeanFieldTable`: keys hashed while scanning the raw input, compared in place. |
| 5 | `308d743` | Typed `BeanFieldWriter`/`BeanField` (`Field.getInt`/`setInt` ...), precomputed `"name":` prefixes, `while` loops, boolean literal matched in place. |
| 6 | `b372b8b`, `c92f150` | Collections read straight into builders, arrays via `ArrayBuilder` + `ClassTag`; typed primitive serializers, `List` walked without an iterator, `foreachEntry` for maps. |
| 7 | `be666f0` | Output `StringBuilder` pre-sized from the previous output length per type. |

Guard rails kept: custom converters registered for primitive types still take precedence (the typed fast paths are
only chosen when the built-in (de)serializer object is in effect); `StringIterator`'s public members used by custom
converters are unchanged; parser leniency (bare keys, whitespace, missing/trailing commas, trailing garbage) is
unchanged except that `{ }` with inner whitespace is now accepted for beans.

## 4. Where the remaining time goes and what could still be done

Per-call costs after the work above (quiet single-fork runs):

- **Reflection per field** (`Field.getX`/`setX` with access checks; writes on the read side addressed in §4a by `FieldWriters`): roughly 10-15 ns per field. Replacing it with
  `MethodHandle` setters (`privateLookupIn` + `unreflectSetter`, `invokeExact` through generated adapters) or
  `Unsafe.putInt(obj, offset, v)` (Unsafe is already a dependency) would roughly halve the per-field cost of flat beans.
  Constructor-based construction of case classes (call the primary constructor with parsed arguments) would remove
  the `final`-field writes entirely and also address the JDK roadmap risk (known-issues §20).
- **`skipWhitespaceChars` at every separator** and the `consume*` helpers: a combined "skip whitespace, expect char"
  on the raw string with a local index would save a few ns per token.
- **`Option`**: `{}` / `{"value":x}` costs an object parse with a key match per Option; the compact format in
  roadmap §4.2 removes it.
- **`TypedConverter`**: `getSimpleName` and the per-class map lookup per value; fine unless polymorphic collections dominate.
- **`Double`/`Float` text**: reading is addressed in §4a (Eisel–Lemire); `append(double)` on write is the JDK's.
- **`String(225)` serialize** still allocates 576 B: the output `String` (232 B) plus the pre-sized builder; unavoidable
  without `serializeToStringBuilder`.
- **Root `MPJsons.serialize[T]`/`deserialize[T]` API** (partly addressed in §4a): ~100 ns and ~100 B per call for the `TypeTag` materialisation
  at the call site; `buildStaticSerializer`/`buildStaticDeserializer` avoid it.

Format-level levers (smaller output and fewer tokens to parse) are in roadmap.md §4 and need the owner's decisions.

## 4a. Deserialization, second round (after 0.7.2)

Harness: `src/jmh/scala/io/mpjsons/bench/DeserializationBench.scala` runs every benchmark against both this code
(`impl=current`) and the frozen 0.7.2 copy in `src/legacy` (`impl=legacy`) in one JMH run, so both sides are measured
under the same machine state:

```bash
sbt "Jmh/run -f 2 -prof gc .*DeserializationBench.*"
```

Fixtures: a small case class `User` (7 fields) through `deserialize[T]` and through a static deserializer, the
`SimpleDataObject` bean, 200 orders (~100 KB: nested case classes, `List`, `Set`, `Map`, `Option`), the same orders read
into a 2-field projection with `ignoreNonExistingFields`, numbers (1000 doubles, 1000 longs, 1000 ints), 300 strings
(a third with escapes and non-ASCII characters), and 500 typed shapes (`markTypedClass`).

Results (JDK 25.0.4.1, Windows 10, two forks, 23/10/2026; measured, not estimated):

| Benchmark | 0.7.2 ops/s | current ops/s | Speed-up | 0.7.2 B/op | current B/op |
|---|---:|---:|---:|---:|---:|
| `User`, `deserialize[T]` | 1 817 136 | 2 287 150 | 1.26x | 440 | 352 |
| `User`, static deserializer | 2 295 336 | 3 177 922 | 1.38x | 344 | 256 |
| `SimpleDataObject` bean | 742 361 | 1 017 821 | 1.37x | 1 256 | 1 192 |
| 200 orders | 1 740 | 2 522 | 1.45x | 386 450 | 242 201 |
| orders into a projection (skipping) | 2 804 | 5 149 | 1.84x | 523 729 | 7 793 |
| strings | 20 442 | 39 908 | 1.95x | 60 808 | 42 464 |
| numbers | 2 783 | 4 861 | 1.75x | 486 145 | 89 001 |
| typed shapes | 3 928 | 9 241 | 2.35x | 279 717 | 58 064 |

What changed (reading side only; output and accepted input unchanged, verified by `fuzz/DifferentialFuzzSpec`
against the 0.7.2 copy in four configurations):

- Field writes go through `FieldWriters`: a hidden class per bean holding each field's setter `MethodHandle` as a
  `static final` constant, so the JIT inlines it to a field store (was `Field.setX` through a non-constant method handle).
- `readString`: closing quote found with `String.indexOf` and a cached next-backslash index; escapes decoded into a
  char buffer owned by the `StringIterator` (one per document) instead of a `StringBuilder` per string.
- Doubles: Clinger fast path and Eisel–Lemire instead of `Double.parseDouble(substring)`; results bit-identical.
- Arrays of `Int`/`Long`/`Double` filled without boxing each element.
- Skipping unknown values: strings, keys and `true`/`false`/`null` are not materialised, and no closure is allocated
  per skipped object or array when `maxDepth` is off.
- `Option` / `Either` keys compared in place; `TypedDeserializer` no longer materialises `typeOf[String]` per value.
- `deserialize[T]` resolves the TypeTag's `Type` by the class of its `TypeCreator` (cached when the creator captures
  nothing), so the reflected type is not rebuilt on every call.
- Whitespace test with an ASCII fast path (still `Character.isWhitespace` semantics).

Tried and rejected: comparing keys with `String.startsWith` or a char loop instead of `regionMatches` (slower), and
a `StringBuilder` for escaped strings (half the throughput on escape-heavy text).

## 5. Constraints to respect while optimising

- Public API: `JsonTypeSerializer.serialize(obj, jsonBuilder: scala.collection.mutable.StringBuilder)`,
  `JsonTypeDeserializer.deserialize(jsonIterator: StringIterator)`, and the `StringIterator` members used by custom
  converters in the wild (`currentChar`, `nextChar`, `skipWhitespaceChars`, `consume*`, `nextIndex`, `seek`) must keep
  working. Add faster internal entry points next to them rather than replacing them.
- Behavioural compatibility the tests pin: whitespace and bare keys accepted, unknown-field skipping (including `null`
  values), `\uXXXX` round trip, scientific notation, negative `Duration`, exact output strings in `TimeSpec`,
  `TupleSpec`, `JavaPrimitivesSpec` (field order!), `TypedConverterSpec`, `UtfControlCharacterSpec`,
  `GenericTypeParamsSpec`, `InheritanceSpec`.
- Custom converters for built-in types must keep taking precedence over the typed fast paths (`eq` checks against
  the built-in serializer/deserializer objects in `BeanFieldWriter.apply` and `BeanField.apply`).
- Do not touch the factory dispatch chains for performance reasons: they run once per type.

## 6. Measurement checklist for a PR

1. JMH, `-f2 -wi 3 -w 1 -i 5 -r 1 -prof gc`, before and after, same machine state; report ns/op and B/op for
   `flat`, `sdo`, `Person`, `String(225)`, `List[Int](100)`.
2. `sbt test` green.
3. Serialized output for the test fixtures byte-identical unless the PR is a declared format change.
