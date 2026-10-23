# Deserialization performance: research for the next round (after 0.7.3)

Date: 23/10/2026. Baseline: branch `claude/mpjsons-2.13-deserialization-perf` (0.7.3), JDK 25.0.4.1, Windows 10.
Labels: **[measured]** = measured here with JMH/JFR, **[pub]** = published by the cited source, **[est]** = my estimate.

## 1. Where 0.7.3 spends its time (JFR, `DeserializationBench`)

CPU, share of samples [measured]:

| Cost centre | Small `User` | 200 orders |
|---|---:|---:|
| Field-name matching (`BeanFieldTable.find` + `lookup` + `regionMatches`/`ArraysSupport.mismatch`) | ~31% | ~21% |
| Per-character `String` access (`charAt`, `checkIndex`, `StringLatin1.charAt`, `length`) | ~19% | ~12% |
| Bean loop, `afterValue`, `tryConsumeNull`, whitespace | ~13% | ~13% |
| Number parsing (`readLong`, `parseExactly`) | ~8% | ~6% |
| Result allocation (`substring`, `allocateInstance`, builders) | ~6% | ~10% |
| `Option` field initialisation (`Field.get` per Option field per object) | – | ~4% |
| Boxing for `Option[Double]` (`BoxesRunTime.boxToDouble`) | – | ~4% |

Allocation [measured]: what remains is almost only the result graph (instances, `String`s, list cells, `Some`).
Exceptions: primitive arrays grow by doubling, so the numbers benchmark allocates ~2.4x the final arrays
(73% of its allocation is `Arrays.copyOf`).

## 2. Spikes that test the main hypotheses [measured, throwaway JMH, 1 fork]

| Hypothesis | Variant | Result |
|---|---|---|
| Field-name matching | hash while scanning + `regionMatches` (current) | 197 µs / 7000 keys |
| | **predicted next key compared while scanning** (no hash, no second pass) | **84 µs (2.3x faster)** |
| Input access, 114 KB orders JSON | `String.charAt` scan (current style) | 166 µs |
| | `getBytes(ISO_8859_1)` copy + `byte[]` scan | 120 µs (copy alone 10.6 µs, ~0.09 ns/char) |
| | `toCharArray` + `char[]` scan | 118 µs (copy 21 µs, 2 bytes/char) |
| 1000-element `double[]` | doubling growth (current) | 2.67 µs, 24 464 B |
| | **reused scratch buffer + one exact copy** | **1.13 µs, 8 016 B** |
| JVM flag `-XX:+UseCompactObjectHeaders` | orders / small `User` / texts / numbers | allocation −12% / −6% / −10% / −9%; throughput +9% / +10% / +18% / ≈0 (noisy run) |

## 3. What the fastest parsers do (sources)

- **jsoniter-scala** (fastest Scala library; JDK 25 results [pub], ops/s: TwitterAPIReading 133 738 vs jackson-module-scala
  54 921 vs circe 36 093; NestedStructsReading 276 929 vs 33 074 — [jdk-25.txt](https://github.com/plokhotnyuk/jsoniter-scala/blob/gh-pages/jdk-25.txt)):
  - String input is converted once with `s.getBytes(UTF_8)` and parsed as bytes; the README calls String input "much less
    efficient" ([JsonReader.scala](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-core/jvm-native/src/main/scala/com/github/plokhotnyuk/jsoniter_scala/core/JsonReader.scala), [README](https://github.com/plokhotnyuk/jsoniter-scala)).
  - Readers are pooled per thread (`readerPool: ThreadLocal[JsonReader]`) with buffers shrunk back after big inputs ([package.scala](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-core/jvm-native/src/main/scala/com/github/plokhotnyuk/jsoniter_scala/core/package.scala)).
  - Generated codecs: for ≤8 fields a plain if-chain of `isCharBufEqualsTo(len, "name")`, otherwise a `@switch` on a
    hash; values go to locals and the constructor is called once ([JsonCodecMaker.scala](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-macros/shared/src/main/scala-2/com/github/plokhotnyuk/jsoniter_scala/macros/JsonCodecMaker.scala)).
  - SWAR via `MethodHandles.byteArrayViewVarHandle` (no Unsafe): one 8-byte mask finds `"`, `\`, control and non-ASCII
    bytes; digits are parsed 8 at a time ([ByteArrayAccess.java](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-core/jvm/src/main/java/com/github/plokhotnyuk/jsoniter_scala/core/ByteArrayAccess.java)).
- **DSL-JSON**: generated code assumes fields arrive in declaration order and checks each expected name cheaply
  (`fillNameWeakHash` + `wasLastName`), falling back to an FNV hash switch on the first miss
  ([ConverterTemplate.java](https://github.com/ngs-doo/dsl-json/blob/master/library/src/main/java/com/dslplatform/json/processor/ConverterTemplate.java)).
  mpjsons writes fields in a fixed order, so its own JSON would always hit this path.
- **fastjson2**: reads `String`'s internal Latin-1 `byte[]` without copying via `IMPL_LOOKUP`/Unsafe and matches names by a
  switch on the first 4 raw bytes plus one 8-byte compare; field writes via Unsafe
  ([JSONReader.java](https://github.com/alibaba/fastjson2/blob/main/core/src/main/java/com/alibaba/fastjson2/JSONReader.java), [JDKUtils.java](https://github.com/alibaba/fastjson2/blob/main/core/src/main/java/com/alibaba/fastjson2/util/JDKUtils.java)).
  Users report JEP 498 Unsafe warnings on JDK 25 ([#3790](https://github.com/alibaba/fastjson2/issues/3790)); not a model to copy.
- **Jackson** copies String input into a recycled `char[]` and canonicalises names as int "quads"
  ([JsonFactory.java](https://github.com/FasterXML/jackson-core/blob/2.19/src/main/java/com/fasterxml/jackson/core/JsonFactory.java), [ByteQuadsCanonicalizer.java](https://github.com/FasterXML/jackson-core/blob/2.19/src/main/java/com/fasterxml/jackson/core/sym/ByteQuadsCanonicalizer.java)).
- **zio-json** matches keys with `StringMatrix`, a bitset that prunes candidates character by character, with no String
  ([lexer.scala](https://github.com/zio/zio-json/blob/series/2.x/zio-json/shared/src/main/scala/zio/json/internal/lexer.scala)).
- **glaze (C++)** finds at build time a character position that is unique among an object's keys and dispatches on that
  single character ("search at compile time for a unique character column among keys") ([v3.0.0 notes](https://github.com/stephenberry/glaze/releases/tag/v3.0.0)).
- **Sonic (ByteDance, Go)** JIT-compiles a decoder per type at runtime, caches it, and offers `Pretouch` to compile ahead
  of the first request ([INTRODUCTION.md](https://github.com/bytedance/sonic/blob/main/docs/INTRODUCTION.md)) — the same idea
  as mpjsons' generated `FieldWriters`, taken to the whole decoder.
- **yyjson** parses in place and validates fully; 1.7–2.4 GB/s [pub] ([ibireme/yyjson](https://github.com/ibireme/yyjson)).
- **simdjson-java** (Vector API, incubator, Java 24+): schema-based twitter.json 1 217 ops/s vs jsoniter-scala 614 vs Jackson 340
  (256-bit) [pub] ([README](https://github.com/simdjson/simdjson-java)); pays off on large documents.
- **JDK**: `String.getBytes(UTF_8)` of an ASCII Latin-1 string is an intrinsic scan plus `clone()`
  ([StringCoding.java](https://github.com/openjdk/jdk25u/blob/master/src/java.base/share/classes/java/lang/StringCoding.java));
  compact headers: SPECjbb2015 −22% heap, a parallel JSON parser −10% time [pub] ([JEP 519](https://openjdk.org/jeps/519));
  Value Objects are a preview in JDK 28 only ([JEP 401](https://openjdk.org/jeps/401)); Leyden AOT cache speeds start-up
  (Spring PetClinic 4.486 → 2.604 s [pub], [JEP 483](https://openjdk.org/jeps/483)) but does not cover user class loaders.

## 4. Proposals, ranked by expected gain per effort

Gains are **[est]** unless marked; all keep the public API and accepted JSON unchanged and can be checked with the
existing `DifferentialFuzzSpec` against 0.7.2.

| # | Proposal | Evidence | Expected gain | Effort | Memory |
|---|---|---|---|---|---|
| 1 | **Ordered-key prediction fused with the key scan** (DSL-JSON style): compare the key against the next expected field while scanning; hash lookup only on a miss | spike 2.3x on key matching; keys are 20–30% of time | 10–20% on bean-heavy JSON | 1–2 days | none |
| 2 | **Primitive-array scratch buffers** owned by the iterator (one exact copy at the end) | spike 2.4x, 3x less allocation | 10–30% on numeric arrays, −60% allocation | hours | less |
| 3 | **Option fields: track assigned fields** (bitmask) instead of a reflective `Field.get` per Option field per object; with `allocateInstance` nothing needs reading | JFR ~4% on orders | 3–5% on Option-heavy beans | hours | none |
| 4 | **ASCII byte fast path**: `s.getBytes(UTF_8)` once; when `bytes.length == s.length` (pure ASCII, same indexes as chars) built-in readers scan `byte[]`; custom converters keep the `String` view; non-ASCII input keeps today's path | spike: byte scan 28% faster incl. copy; jsoniter-scala and Jackson both copy String input | 15–25% on medium/large ASCII documents | 1–2 weeks | +1 byte/char transient (pooled buffer possible) |
| 5 | **SWAR on top of #4** (`byteArrayViewVarHandle`): 8 bytes per step for string ends, 8-digit number parsing | jsoniter-scala, borer, FastDoubleParser | further 10–20% on string/number-heavy JSON | 1 week | none |
| 6 | **Thread-local scratch** (escape buffer, array scratch) reused across calls, shrunk after large inputs | jsoniter-scala `readerPool` | small, mainly allocation | days | bounded per thread (virtual-thread caveat) |
| 7 | **Whole-decoder generation per bean** (Sonic-style, extending `FieldWriters`): a hidden class with the key dispatch as a `lookupswitch` (or glaze-style unique-character switch), typed readers called directly and constant setters — removes the megamorphic `BeanField.set` and the generic table | jsoniter-scala NestedStructs 8.4x Jackson [pub] shows the ceiling of specialised code | 1.5–2x on bean-heavy JSON | 3–6 weeks | one hidden class per type |
| 8 | **Root `deserialize[T]` overhead** (still ~28% slower than `buildStaticDeserializer` on small objects [measured]): a Scala 2.13 macro overload that caches a static deserializer per call site | measured gap 2.29M vs 3.18M ops/s | up to 1.3x for small messages via the API | 1 week, API decision | none |
| 9 | **Recommend JVM flags** in README: `-XX:+UseCompactObjectHeaders` (opt-in in JDK 25) | measured −6 to −12% allocation | ~5–10% | docs | less |

Not recommended: `sun.misc.Unsafe` reads/writes (JEP 498 warnings on JDK 24+, deny later), reading `String.value`
through `IMPL_LOOKUP` hacks (fastjson2; brittle and unsupported), simdjson/Vector API (incubator module, setup cost
dominates ~300 ns objects), switching default construction to constructors (changes semantics; already opt-in).

## 5. Suggested order

1. #1, #2, #3 together: low risk, no format or memory trade-offs; expected 15–25% on typical bean JSON.
2. #4 + #5 as one project, behind the same differential fuzzing (plus a non-ASCII corpus): the step from ~2 ns/char
   towards jsoniter-scala territory.
3. #7 only if the profile after 1–2 is dominated by dispatch; it is the largest change and costs one class per type.
4. #8 is an API decision for the owner; #9 is a README note.
