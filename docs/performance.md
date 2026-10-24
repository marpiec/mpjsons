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
  nothing), so the reflected type is not rebuilt on every call. *(Removed after 0.7.4: Scala 2.13 already caches
  materialised TypeTags, so this only added a reflective call; see §4c.)*
- Whitespace test with an ASCII fast path (still `Character.isWhitespace` semantics).

Tried and rejected: comparing keys with `String.startsWith` or a char loop instead of `regionMatches` (slower), and
a `StringBuilder` for escaped strings (half the throughput on escape-heavy text).

## 4b. Deserialization, third round (after 0.7.3)

Changes (research and spikes in `deserialization-research.md`, proposals #1–#3):

- **Key prediction**: `BeanFieldTable.find(jsonIterator, expected)` first compares the key with the next key in the
  serializer's order (character by character, no hash); any other key takes the hash lookup, with the same result.
  After a key that is not a field, prediction pauses until a field matches again.
- **Absent `Option` fields** are set to `None` after the key loop from a local bitmask; instances allocated without a
  constructor are not read reflectively.
- **Primitive arrays** (`Int`/`Long`/`Double`) are allocated at the element count (commas up to `]`, counted with the
  `String.indexOf` intrinsic, capped at 4096) instead of growing by doubling and trimming.
- All per-call state is local; `BeanReadingSpec` covers key orders, `Option` defaults, nested deserialization from a
  converter and 16 threads on one instance.

Results (JDK 25.0.4.1, two forks, same session, 23/10/2026; measured). 0.7.3 was measured from commit `0182286` with the
same benchmark source; the last two rows were re-measured after the "pause prediction after unknown keys" change:

| Benchmark | 0.7.2 ops/s | 0.7.3 ops/s | current ops/s | vs 0.7.3 | B/op 0.7.3 → current |
|---|---:|---:|---:|---:|---:|
| `User`, static deserializer | 2 257 228 | 3 161 857 | 3 846 750 | +22% | 256 → 256 |
| `User`, `deserialize[T]` | 1 788 853 | 2 244 289 | 2 585 681 | +15% | 352 → 352 |
| `SimpleDataObject` bean | 746 170 | 947 657 | 1 076 408 | +14% | 1 192 → 1 064 |
| typed shapes | 3 792 | 8 437 | 9 174 | +9% | 58 064 → 58 064 |
| strings | 21 100 | 39 688 | 39 765 | ±0 | 42 464 → 42 464 |
| numbers | 2 561 | 4 762 | 4 607 | −3% | 89 001 → 56 105 |
| 200 orders | 1 652 | 2 468 | 2 965 | +20% | 242 201 → 242 201 |
| orders into a projection (skipping) | 2 618 | 5 075 | 5 275 | +4% | 7 793 → 7 793 |

Trade-off: counting array elements costs ~4% on the numbers benchmark (A/B repeated twice: 4 603 vs 4 795 ops/s) for
37% less allocation per document (56 105 vs 89 001 B). Revert `elementCountHint` to the doubling code if raw speed on
numeric arrays matters more than allocation.

Tried and rejected: scratch arrays owned by the `StringIterator` — each call creates a new iterator, so they still grew
by doubling and saved nothing for documents with one array of each type.

## 4c. Serialization (after 0.7.4)

Changes (research, sources and the remaining proposals in `serialization-research.md`, S1–S2):

- **Field readers (S1)**: `BeanSerializer` reads fields through `FieldReaders`, a generated hidden class holding one
  getter `MethodHandle` per field (`unreflectGetter`) as a `static final` constant, selected by a switch on the field
  index; `Field.getX` is the fallback if the class cannot be defined. Mirror of `FieldWriters`.
- **Primitive arrays (S2)**: `ArraySerializer` writes `Array[Int/Long/Double/Float/Short/Byte/Boolean]` with a loop
  over the primitive array instead of boxing every element, when the built-in element serializer is in effect (a
  custom converter still wins). Same text, same exception for non-finite numbers.
- **Byte output (S3)** was implemented and removed again: as `serialize(obj).getBytes(UTF_8)` it saved nothing over
  what callers already do. It came back with the native UTF-8 writer (§4d).
- `fuzz/SerializationDifferentialSpec` compares the output with the 0.7.2 copy for 12 models in 4 configurations,
  the failures for non-finite numbers and nulls, and 16 threads on one instance. One run with `-Dfuzz.values=5000`
  and 2 seeds compared 480,000 values: all identical.

Results (JDK 25.0.4.1, `SerializationBench`, two forks, same session, 23/10/2026; measured;
`bench-results/serialization-s1-s3.txt`, before: `serialization-baseline.txt`):

| Benchmark | 0.7.2 ops/s | current ops/s | change | B/op 0.7.2 → current |
|---|---:|---:|---:|---:|
| `User`, static serializer | 3 731 417 | 4 302 594 | +15% | 384 → 384 |
| `SimpleBean` | 1 275 817 | 1 520 217 | +19% | 952 → 864 |
| typed shapes | 9 460 | 11 462 | +21% | 45 744 → 45 744 |
| 200 orders | 3 044 | 3 273 | +8% | 250 297 → 250 297 |
| 200 orders + `getBytes(UTF_8)` | 2 876 | 3 089 | +7% | 364 097 → 364 097 |
| numbers | 9 170 | 9 661 | +5% | 148 768 → 100 704 |
| strings | 16 438 | 17 842 | noise | 65 768 → 65 768 |
| strings + `getBytes(UTF_8)` | 18 453 | 18 743 | noise | 118 744 → 118 744 |

`serialize[T]` / `deserialize[T]` with a TypeTag (3 forks, `bench-results/api-paths*.txt`; the 0.7.2 and middle
columns come from `api-paths.txt`, the current column from `api-paths-symbol-check.txt`, a separate run with the same
settings on the same day):

| Benchmark | 0.7.2 ops/s | 0.7.4 + S1–S2 | current | current vs 0.7.2 |
|---|---:|---:|---:|---:|
| `User`, `serialize[T]` | 2 425 241 | 2 451 435 | 2 631 281 | +8% |
| `User`, `deserialize[T]` | 1 715 632 | 2 564 697 | 2 739 915 | +60% |
| `User`, `deserialize(json, typeTag[User].tpe)` | 1 703 671 | 2 731 996 | 2 750 089 | +61% |

- **TypeTag cache removed**: the `TypeTagTypes` cache from 0.7.3 (keyed by the `TypeCreator` class) is gone.
  - Since 2.13.7, Scala returns the same TypeTag for every materialisation at a call site:
    `TypeTag.apply` → `JavaMirror.typeTag`, a `ClassValue` per creator class
    ([TypeTags.scala L292–305](https://github.com/scala/scala/blob/v2.13.16/src/reflect/scala/reflect/api/TypeTags.scala#L292-L305),
    [JavaMirrors.scala L122–145](https://github.com/scala/scala/blob/v2.13.16/src/reflect/scala/reflect/runtime/JavaMirrors.scala#L122-L145)).
  - So `tag.tpe` is already computed once, and the cache's reflective call only cost time. Removing it gave
    +3.4% on `deserialize[T]` and +4.0% on `serialize[T]`.
- **"No type given" check**: it now tests the resolved type's symbol. Comparing TypeTags with `==` compares mirrors and
  types ([TypeTags.scala L264](https://github.com/scala/scala/blob/v2.13.16/src/reflect/scala/reflect/api/TypeTags.scala#L264)).
  That gave another +3.3%, and `deserialize[T]` now runs as fast as passing `tag.tpe` directly. The error and its
  message are unchanged (`BugFixesSpec`).
- **TypeTag materialisation (`TypeTagBench`)**:
  - Materialising `typeTag[User]` at the call costs 62 ns on one thread (16.1 M/s). A tag created once costs ~1 ns.
  - Materialising takes Scala reflection's global lock. The generated code calls `runtimeMirror(<class>.getClassLoader)`
    ([reify/package.scala L37–51](https://github.com/scala/scala/blob/v2.13.16/src/compiler/scala/reflect/reify/package.scala#L37-L51)),
    which is `gilSynchronized`
    ([JavaMirrors.scala L63–68](https://github.com/scala/scala/blob/v2.13.16/src/reflect/scala/reflect/runtime/JavaMirrors.scala#L63-L68)).
    8 threads doing nothing else reach only 10.9 M/s in total.
  - Callers on very hot paths can keep the TypeTag in a `val` or use `buildStaticSerializer`.

The string benchmarks are not reliable to ±10%: in the baseline run, where both implementations had the same
serializer code, `textsBytes` measured 18 498 vs 14 422 ops/s. The other rows differed by at most 2.4% in that run.

## 4d. Native UTF-8 output (S5, after 0.7.5)

`serializeToBytes` on `MPJsons` and `StaticSerializer`:
- **Mechanism:** every built-in serializer also implements the internal `Utf8Writer.writeUtf8(obj, out: Utf8Output)`,
  writing bytes into a growable array.
- **Escaping and copying:** string escaping uses the same table and runs as `serialize`. ASCII runs are copied in bulk
  (`String.getBytes(int, int, byte[], int)`); other runs are encoded.
- **Numbers and names:** ints and longs are written from a digit-pair table. Doubles and floats are formatted by
  `StringBuilder.append` into a scratch builder and copied. Field names and type wrappers are pre-encoded.
- **Contract:** the bytes are exactly `serialize(obj).getBytes(UTF_8)`, including `?` for an unpaired surrogate.
  The same checks run in the same order, so the same exceptions are thrown.
- **Custom converters and subclasses:** custom converters keep the `StringBuilder` SPI and are bridged (one builder
  reused per output). A subclass of a built-in serializer is bridged too, because only the exact built-in class
  writes bytes itself, so an overridden `serialize` is honoured.

Verification:
- `fuzz/ByteOutputDifferentialSpec` covers all fuzz models and extra ones in 5 configurations, through every entry
  point. The extras are unpaired and reversed surrogates, java.time/UUID/big numbers, tuples 1–6, scalar map keys,
  non-ASCII field and type names, fields holding defaults, and custom converters.
- It also checks 15 failure cases and 16 threads.
- `Utf8OutputSpec` compares 200,000+ random ints and longs, 200,000 doubles and floats, and 70,000 strings with
  JDK formatting and `getBytes`.
- Mutation checks:
  - Bridging subclasses switched off: caught.
  - Wrong surrogate encoding: caught by 10 tests.
  - Default omission dropped in the Int, Double and Object field writers: caught. A model with default-valued fields
    was added for this, because the generic fuzz rarely produces defaults.

Results (JDK 25.0.4.1, `Utf8OutputBench`, two forks, 23/10/2026; measured; `bench-results/utf8-output.txt`):

| Fixture | `serialize` ops/s | `serialize(x).getBytes(UTF_8)` | `serializeToBytes` | bytes: new vs before | B/op before → now |
|---|---:|---:|---:|---:|---:|
| `User` | 4 164 847 | 3 912 543 | 4 895 104 | +25% | 504 → 424 |
| `SimpleBean` | 1 490 125 | 1 407 015 | 1 742 372 | +24% | 1 200 → 832 |
| strings (non-ASCII) | 18 094 | 16 604 | 26 641 | +60% | 118 723 → 27 328 |
| 200 orders | 3 273 | 3 180 | 3 319 | +4% | 364 097 → 250 337 |
| numbers | 9 636 | 9 226 | 9 064 | −2% | 150 997 → 100 744 |
| typed shapes (doubles) | 11 822 | 11 798 | 11 420 | −3% | 68 553 → 45 784 |

- **Where it is slower:** number-heavy output, by 2–3% (within about 1.5 error bars). Doubles are formatted by the JDK
  into a scratch builder and then copied. Copying with `getChars`, or using `Double.toString` with a bulk copy, was
  slower: −4.5%/−2.4%, and −9%/−14% with 3x allocation. Formatting doubles ourselves (S8) is the remaining lever.
- **`serialize` still uses the `StringBuilder`.** A String decoded from the writer's buffer measured +17% (`User`),
  +20% (bean), +6% (strings) and +1% (orders), but −6% (numbers) and −5% (shapes). It also could not keep unpaired
  surrogates. So it was not adopted (`xUtf8String` rows in `utf8-output.txt`).

## 4e. 0.7.6 against 0.6.51 (the last release before 0.7)

How the comparison was set up (measured 23/10/2026, JDK 25.0.4.1, two forks with `-prof gc`, warm-up 3×2 s,
measurement 5×2 s):

- **Released jars:** 0.6.51 from the Maven repository and 0.7.6 as published from `377e666`. The two versions share
  package names, so they cannot run side by side.
- **Harness:** a standalone JMH project ran the same benchmark code with the same fixtures (`Models.scala`) twice,
  once per version. Only the API calls differ (`markTypedClass`, the lenient constructor, bytes).
- **Same content written:** both versions produce identical JSON content for every fixture (same length, same
  multiset of bytes); 0.6.51 writes keys in reverse declaration order (docs/upgrading.md).
- **Same values read:** each version reads its own output. 0.6.51 reads equal values, though its maps iterate in a
  different order.
- **Old key order:** the `old key order` rows show 0.7.6 reading JSON written by 0.6.51, as data stored by
  applications on 0.6.x looks.
- **Raw results:** `bench-results/release-0.6.51.txt` and `release-0.7.6.txt`.

| Benchmark | 0.6.51 ops/s | 0.7.6 ops/s | speed-up | B/op 0.6.51 → 0.7.6 |
|---|---:|---:|---:|---:|
| **read** `User`, static deserializer | 950 640 | 3 081 474 | 3.24× | 1 896 → 256 |
| read `User`, `deserialize[T]` | 783 411 | 2 825 938 | 3.61× | 1 992 → 352 |
| read `User`, old key order | 950 619 | 2 901 440 | 3.05× | 1 896 → 256 |
| read 200 orders (114 KB) | 956 | 2 746 | 2.87× | 1 705 876 → 244 601 |
| read 200 orders, old key order | 967 | 2 412 | 2.50× | 1 705 876 → 242 201 |
| read 200 orders into a projection (skipping) | 1 697 | 5 913 | 3.48× | 1 012 058 → 7 793 |
| read `SimpleBean` | 343 508 | 1 081 272 | 3.15× | 4 816 → 1 032 |
| read number arrays | 1 561 | 4 671 | 2.99× | 852 466 → 56 105 |
| read strings (escapes, non-ASCII) | 14 155 | 42 550 | 3.01× | 116 273 → 42 464 |
| read typed shapes | 2 350 | 9 611 | 4.09× | 676 009 → 58 064 |
| **write** `User`, static serializer | 2 167 121 | 4 159 166 | 1.92× | 824 → 384 |
| write `User`, `serialize[T]` | 1 585 417 | 3 049 086 | 1.92× | 920 → 480 |
| write 200 orders | 1 981 | 3 256 | 1.64× | 701 778 → 250 297 |
| write `SimpleBean` | 908 574 | 1 508 612 | 1.66× | 2 584 → 864 |
| write number arrays | 6 074 | 9 577 | 1.58× | 469 689 → 100 704 |
| write strings | 16 999 | 18 012 | ~1.06× (noise: ±19% on 0.6.51) | 100 276 → 65 768 |
| write typed shapes | 5 994 | 11 497 | 1.92× | 336 593 → 45 744 |
| **UTF-8 bytes** `User` (0.6.51: `getBytes`; 0.7.6: `serializeToBytes`) | 2 134 298 | 4 894 429 | 2.29× | 944 → 424 |
| UTF-8 bytes, 200 orders | 1 936 | 3 328 | 1.72× | 815 578 → 250 337 |
| UTF-8 bytes, strings | 13 884 | 24 857 | 1.79× | 153 233 → 27 328 |

Reading is 2.5–4.1× faster with 63–99% less allocation. Writing is 1.6–1.9× faster (strings within noise), and
UTF-8 output 1.7–2.3× faster, with 34–86% less allocation.

Reading JSON in the old key order costs 0.7.6 about 12% on orders (2 412 vs 2 746 ops/s; the second row is noisy at
±15%) and about 6% on `User`: key prediction misses, and every key takes the hash lookup. Data re-written by 0.7.x
gets the full speed.

Noisy rows (error above 5%):
- 0.7.6 read orders: ±15.2%;
- 0.6.51 write strings and strings bytes: ±19.1% and ±15.7%;
- 0.7.6 write `SimpleBean`: ±8.2%.

## 4f. Reading after 0.7.6: jsonb input and many tiny documents

What prompted it: a production report that JSON read back from PostgreSQL `jsonb` columns was slower than mpjsons' own
output, and that events are read one by one as documents of 10–30 characters.

Why jsonb input is slower:
- jsonb does not keep key order; it prints keys sorted by length, then bytes, with `", "` and `": "` separators
  (PostgreSQL `jsonb_util.c` `lengthCompareJsonbString`, `jsonb.c`).
- In 0.7.6, key prediction followed only the serializer's order. On jsonb input almost every key missed and took the
  hash lookup.

Changes, all with `sbt test` green (310 specs, including the differential fuzzers against the 0.7.2 copy):
- **Two predicted key orders** (`BeanFieldTable`): the serializer's and jsonb's. `expected` indexes a precomputed
  `predictions` array, "after field p, in order m". The bean loop calls the small `matchPredicted` and only on a miss
  `find`.
  - A first version kept the prediction in one bigger method. In one harness C2 compiled it standalone first and then
    would not inline it ("already compiled into a big method"), which hid the whole gain.
  - Replaced after 0.7.7 by learned hints, see §4g.
- **`matchKey` uses `String.regionMatches`** instead of comparing char by char. Measured +5 to +19% in the standalone
  harness, −3 to −5% in the in-repo one (`round5-key-compare.txt`); kept.
- **Escaped strings**: bulk copies between escapes, `\u` decoded without a substring.
- **Memoizer lookup before type resolution** when the context binds no type parameters: `deserialize(json, tpe)` and
  typed values skip the reflection.
- **Typed values**: type names already read are matched in place on the raw input (`TypedDeserializer.knownNames`),
  only while the built-in String reader is in effect. This saves a String and a hash per value.
- **Absent `Option` fields** get `None` through the generated `FieldWriters` instead of `Field.set`.
- **Tried and reverted**: skipping whitespace runs out of line. It was +3% on large jsonb documents in-repo, but the
  inline loop measured equal or better in the standalone harness (jsonb `User` +4.3%, tiny jsonb documents −3 to −4% ns)
  (`round5-whitespace.txt`).

Measured 24/10/2026, JDK 25.0.4.1. Standalone JMH project (the one used for §4e), 2 forks, `-prof gc`. Raw results
are in `round5-release-0.7.6.txt` and `round5-candidate.txt`. The candidate still had the out-of-line whitespace
skip; the revert measured equal or better (above).

| Read benchmark | 0.7.6 ops/s | candidate ops/s | change |
|---|---:|---:|---:|
| `User`, static deserializer | 2 956 224 | 3 400 433 | +15.0% |
| `User`, `deserialize(json, tpe)` | 2 781 744 | 3 304 891 | +18.8% |
| `User`, jsonb shape | 2 247 043 | 2 665 842 | +18.6% |
| `User`, jsonb shape, `deserialize(json, tpe)` | 2 124 744 | 2 514 104 | +18.3% |
| `User`, old key order | 2 538 443 | 2 550 529 | +0.5% |
| 200 orders | 2 373 | 2 642 | +11.3% |
| 200 orders, jsonb shape | 1 877 | 2 050 | +9.2% |
| 200 orders, old key order | 2 168 | 2 031 (±4%) | −6.3% |
| 200 orders into a projection (skipping) | 5 133 | 4 973 | −3.1% |
| strings | 37 472 | 43 731 (±6%) | +16.7% |
| escaped documents | 4 421 (±5%) | 4 998 | +13.0% |
| escaped documents, jsonb shape | 4 472 | 5 234 | +17.0% |
| typed shapes | 8 622 | 9 232 | +7.1% (B/op −41%) |
| typed shapes, jsonb shape | 6 900 | 7 505 | +8.8% (B/op −41%) |

Tiny documents (`TinyBench`, ns/op, lower is better; compact / jsonb shape):

| Document | 0.7.6 | candidate |
|---|---:|---:|
| `{"id":42}`, static | 42.7 / 46.8 | 44.2 / 50.7 |
| `{"id":42}`, `deserialize(json, tpe)` | 55.7 / 56.9 | 51.6 / 58.0 |
| `{"id":42}`, `deserialize(json, className)` | 55.0 / 60.6 | 49.6 / 55.9 |
| `{"id":7}` into `(Long, Option[String])` | 48.1 / 51.0 | 44.5 / 50.6 |
| `{"TinyCreated":{"id":5}}`, typed, static | 103.6 / 111.8 | 66.6 / 76.4 (B/op 128 → 72) |
| same, `deserialize(json, tpe)` | 136.7 / 172.1 | 73.6 / 83.8 (B/op 144–160 → 72) |
| `{"a":1,"b":true}` | 53.2 / 69.5 | 62.9 / 82.7 |
| `{"v":"abc"}` | 44.4 / 51.1 | 49.5 / 57.0 |

Reading the tiny-document rows:
- Bimodal results: depending on what C2 compiles first, the bean reader is either inlined into the root call or not.
  When it is, escape analysis removes the `StringIterator` (48 B less per call) and the read takes 8–14 ns less.
- 0.7.6 landed in the fast mode more often in this harness (`pair`/`text` compact: ±6.6 / ±4.8 ns, B/op 48 / 88 are
  averages of both modes).
- A single fork with flags passed through JMH showed the reverse (0.7.6 48.4 ns, candidate 43.3 ns on `text`).
- The jsonb rows of `pair`/`text` (same mode, same B/op) are 6–13 ns slower in this run, with the reverted whitespace
  skip; after the revert they measured 3–4% faster than this.
- In an application where the same bean type is also read nested, the slow mode is the likely one for both versions.

The lesson for later work: results moved by 15–25% between harnesses with byte-identical library code (in-repo
`JsonbShapeBench` vs the standalone project, and even the standalone benchmark class copied into the repo). The
cause was C2 compile order and inlining, not the code under test. Check a change in both harnesses with several forks
before trusting a few percent.

## 4g. After 0.7.7: key order and Polish text without a penalty

What prompted it: Neula's profile of 0.7.7 showed key matching no faster than in 0.7.6. Its explanation was Polish
letters: they make Java store the whole input as UTF-16, and `regionMatches` against Latin-1 field names then takes a
char-by-char path in the JDK. The owner's goals were no penalty for Polish text or for key order, and no complexity
that does not pay for itself.

What changed, all with `sbt test` green (311 specs, including the differential fuzzers against the 0.7.2 copy):
- **Learned key hints replace the two fixed orders.**
  - The table remembers the first key and, in each field, the key that followed it in an object read earlier.
  - Any consistent order then predicts well after the first object: mpjsons', jsonb's sorted one, 0.6.x's reversed
    one, or a client's own.
  - Hints are plain fields written by any thread. They only point to fields `find` returned for that name, so a stale
    hint costs one compare, never a wrong field. The 16-thread spec reads all key permutations of one type at once.
  - Net code is smaller than 0.7.7's prediction: no order encoding and no jsonb sorting.
- **Order-independent lookup on a miss**:
  - the end of a quoted key comes from `String.indexOf`;
  - the slot comes from the key's length and its first and last characters;
  - keys are compared with `regionMatches`.

What the measurements decided (`round6-variants.txt`, both harnesses, 2 forks):
- **Lookup only, no prediction**: order-independent, but 13–23% slower on keys in the expected order. Prediction pays
  for itself.
- **Hints compared char by char instead of `regionMatches`**: 3–9% slower than `regionMatches`. Polish input
  included; Neula's concern did not reproduce here.
- **Typed-name matching in place, kept**: without it a tiny typed event took 18–28 ns more (+28–36%), and typed shapes
  read 4–8% slower.

Result against 0.7.7 (standalone harness, 24/10/2026, JDK 25.0.4.1, 2 forks, `-prof gc`; `round6-0.7.7.txt`,
`round6-learned-hints.txt`). No benchmark got slower, and allocation is unchanged.

| Read | 0.7.7 ops/s | now ops/s | change |
|---|---:|---:|---:|
| `User`, compact | 3 331 309 | 3 485 469 | +4.6% |
| `User`, old (reversed) key order | 2 466 224 | 3 460 341 | +40.3% |
| `User`, jsonb shape | 2 770 595 | 3 288 154 | +18.7% |
| `User`, Polish text | 3 268 954 | 3 429 572 | +4.9% |
| `User`, Polish text, jsonb shape | 2 772 571 | 3 228 261 | +16.4% |
| 200 orders, compact | 2 730 | 2 840 | +4.0% |
| 200 orders, old key order | 2 088 (±7%) | 2 855 | +36.7% |
| 200 orders, jsonb shape | 2 226 | 2 438 | +9.5% |
| 200 orders, Polish text | 2 590 | 2 685 | +3.6% |
| 200 orders, Polish text, jsonb shape | 2 161 | 2 349 | +8.7% |
| typed shapes / jsonb shape | 8 807 / 7 379 | 9 232 / 8 222 | +4.8% / +11.4% |
| projection, escaped documents, strings | | | +0.2% to +2.6% |
| tiny documents (`TinyBench`, ns/op) | | | 0 to −10.5% |

What is left:
- **Key order no longer costs anything**: `User` reads at 3.49 M ops/s in mpjsons' order and 3.46 M in reversed order;
  orders at 2 840 and 2 855 ops/s.
- **jsonb shape** is still 6–14% slower than compact. Those are the spaces after `,` and `:`, one more character per
  token to skip.
- **Polish text** costs nothing on `User` (−1.6%, within noise) and 5.5% on orders. That remainder is in the strings,
  not the keys: a `substring` of a UTF-16 input goes through `StringUTF16.newString`, which first tries to compress
  each value back to Latin-1. Every Java JSON library that returns compact Strings pays this. Avoiding it would mean
  copying the input to a `char[]` per document, which is not worth it for 5%.
- **Reference field writes** (Neula's request 5) were not changed. The generated writers already hold constant method
  handles. A `VarHandle` performs the same receiver and type checks, and a direct `putfield` cannot write the final
  fields of case classes, so there is no simple win.

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
