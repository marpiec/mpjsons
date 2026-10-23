# Serialization performance and byte output: research (after 0.7.4)

Date: 23/10/2026. JDK 25.0.4.1, Windows 10, Scala 2.13.16.
Labels: **[measured]** here with JMH/JFR, **[pub]** published by the cited source, **[est]** my estimate.
Sources were checked in the code of each project (default branches on 23/10/2026), not only in READMEs.

## 1. Baseline

`SerializationBench` (src/jmh) runs the same fixtures as `DeserializationBench` through `StaticSerializer`, against the
0.7.2 copy in `src/legacy` [measured, 2 forks]:

| Benchmark | ops/s | B/op | Note |
|---|---:|---:|---|
| small `User` (static) | 3 656 725 | 384 | |
| small `User` via `serialize[T]` | 2 245 662 | 504 | |
| `SimpleDataObject` bean | 1 261 613 | 952 | |
| 200 orders (~114 KB) | 3 016 | 250 297 | as fast as *reading* them (2 965 ops/s) |
| 200 orders + `getBytes(UTF_8)` | 2 914 | 364 097 | +45% allocation for the bytes a socket/HTTP needs |
| strings (some non-Latin-1) | 17 620 | 65 768 | |
| numbers | 9 008 | 148 768 | |
| typed shapes | 10 086 | 45 744 | |

Serialization is unchanged since 0.7.2 (all ratios 0.98–1.02x). Fast libraries write about twice as fast as they read;
mpjsons does not.

## 2. Where the time goes (JFR) [measured]

| Cost centre | orders | small `User` | strings | numbers |
|---|---:|---:|---:|---:|
| Reflective field reads (`Field.get`/`getX` → non-constant `MethodHandle`: `invokeExact_MT`, `LambdaForm`, `checkCustomized`) | ~10–12% | ~10% | – | – |
| `StringBuilder.append(String)` for field-name prefixes and runs, buffer checks | ~15% | ~17% | ~5% | – |
| String escaping loop (`charAt` + table) | ~8% | ~10% | ~38% | – |
| Run appends after the builder became UTF-16 (`append(CharSequence,int,int)`, `StringUTF16.putCharsSB`) | – | – | ~25% | – |
| JDK number formatting (`DoubleToDecimal`, `DecimalDigits`) | ~4% | ~5% | – | ~70% |
| Boxing of array elements (`boxToDouble` via the generic iterator) | – | – | – | ~5% |

## 3. Spikes [measured, throwaway JMH, 1 fork]

| Hypothesis | Result |
|---|---|
| Escaping 100 × 40 ASCII chars: `StringBuilder` run appends (current) | 5.6 µs |
| ... the same written as UTF-8 bytes, char by char | 8.1 µs (slower: run appends are bulk copies) |
| ... bytes, 8 chars packed per `long` (jsoniter-scala style SWAR) | 7.3 µs |
| Same strings after one non-Latin-1 char (builder inflated to UTF-16) | same time, **3x allocation** (24.6 KB vs 8.2 KB) |
| End of call, 114 KB: `toString` | 12.1 µs, 114 KB |
| ... `toString` + `getBytes(UTF_8)` | 26.3 µs, 228 KB (~8% of one orders write) |
| ... one `Arrays.copyOf` of a byte buffer | 5.3 µs, 66 KB |

Conclusion: a byte writer only helps if it copies ASCII runs in bulk too; its sure wins are fewer end-of-call copies,
no UTF-16 inflation, and output that HTTP can send without re-encoding.

## 4. What fast writers do (verified in source)

- **jsoniter-scala** `JsonWriter` ([source](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-core/jvm-native/src/main/scala/com/github/plokhotnyuk/jsoniter_scala/core/JsonWriter.scala), [package.scala](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-core/jvm-native/src/main/scala/com/github/plokhotnyuk/jsoniter_scala/core/package.scala)):
  writes UTF-8 bytes into a `ThreadLocal` pooled 32 KB buffer (shrunk back after big outputs); `writeToArray` = one
  `Arrays.copyOf`; `writeToString` = `new String(buf, 0, count, UTF_8)`; `writeToStream`/`writeToSubArray`/`writeToByteBuffer`
  avoid the final copy. Strings: "SWAR checking of 8 chars", speculative 8-byte stores via `byteArrayViewVarHandle`
  (no Unsafe); ints after James Anhalt's algorithm; doubles via xjb. JDK 25 results [pub]
  ([jdk-25.txt](https://plokhotnyuk.github.io/jsoniter-scala/jdk-25.txt)), ops/s: TwitterAPIWriting 231 382 vs Jackson 131 640
  vs circe 29 055; NestedStructsWriting 653 502 vs 140 854 vs 41 090; writing into a preallocated array
  (`writeToSubArray`) adds ~11–22% [est from the same file].
- **Jackson** ([ObjectMapper](https://github.com/FasterXML/jackson-databind), [UTF8JsonGenerator](https://github.com/FasterXML/jackson-core/blob/2.19/src/main/java/com/fasterxml/jackson/core/json/UTF8JsonGenerator.java)):
  `writeValueAsBytes` uses a UTF-8 generator; field names are pre-encoded quoted UTF-8 (`SerializedString`); buffers
  come from `RecyclerPool`s — Jackson 3 defaults to a concurrent deque pool because a ThreadLocal pool "may not work
  well" with virtual threads ([RecyclerPool javadoc](https://github.com/FasterXML/jackson-core/blob/2.19/src/main/java/com/fasterxml/jackson/core/util/RecyclerPool.java)).
- **fastjson2** pre-encodes `"name":` as bytes/longs and writes a short name with one 8-byte store, but through
  `sun.misc.Unsafe` and String internals ([JSONWriterUTF8](https://github.com/alibaba/fastjson2/blob/main/core/src/main/java/com/alibaba/fastjson2/JSONWriterUTF8.java));
  JEP 498 warns on JDK 24+ and will deny later ([JEP 498](https://openjdk.org/jeps/498)) — not a model.
- **DSL-JSON**: `byte[]` output, pre-encoded names (`("\"" + name + "\":").getBytes(UTF_8)`), Ryu doubles, 3-digit table for ints
  ([JsonWriter](https://github.com/ngs-doo/dsl-json/blob/master/library/src/main/java/com/dslplatform/json/JsonWriter.java)).
- **JDK**: `Double.toString` uses Raffaello Giulietti's Schubfach since JDK 19 ("17.7x speedup", [PR 3402](https://github.com/openjdk/jdk/pull/3402) [pub]);
  in JDK 25 `StringBuilder.append(double)` writes straight into the builder (JDK-8334328), `append(long)` uses a digit-pair
  table. A builder that receives one non-Latin-1 char is inflated to UTF-16 and stays so (`setLength(0)` does not reset
  the coder); `toString` always copies ([AbstractStringBuilder, jdk25u](https://github.com/openjdk/jdk25u/blob/master/src/java.base/share/classes/java/lang/AbstractStringBuilder.java)).
- **Virtual threads**: "do not use thread locals to pool costly resources" ([JEP 444](https://openjdk.org/jeps/444)).

## 5. Output types for Pekko HTTP and ScalikeJDBC (verified in source)

**Pekko HTTP** ([ByteString](https://github.com/apache/pekko/blob/main/actor/src/main/scala/org/apache/pekko/util/ByteString.scala), [HttpEntity](https://github.com/apache/pekko-http/blob/main/http-core/src/main/scala/org/apache/pekko/http/scaladsl/model/HttpEntity.scala)):
- `HttpEntity(ContentTypes.`application/json`, string)` = one `getBytes(UTF_8)` and no further copy.
- `HttpEntity(contentType, bytes: Array[Byte])` and `ByteString(bytes)` **copy** the array; `ByteString.fromArrayUnsafe(bytes)` does not.
- Cheapest from a `byte[]`: `HttpEntity.Strict(ContentTypes.`application/json`, ByteString.fromArrayUnsafe(bytes))`, which is
  exactly what the jsoniter-scala marshaller in pekko-http-json does
  (`Marshaller.withFixedContentType(contentType) { obj => HttpEntity.Strict(contentType, ByteString.fromArrayUnsafe(writeToArray(obj, config))) }`,
  [JsoniterScalaSupport](https://github.com/pjfanning/pekko-http-json/blob/main/pekko-http-jsoniter-scala/src/main/scala/com/github/pjfanning/pekkohttpjsoniterscala/JsoniterScalaSupport.scala));
  streaming maps each element to `ByteString.fromArrayUnsafe(writeToArray(a))` through a framing renderer.

**ScalikeJDBC + PostgreSQL** ([pgjdbc PgPreparedStatement](https://github.com/pgjdbc/pgjdbc/blob/master/pgjdbc/src/main/java/org/postgresql/jdbc/PgPreparedStatement.java), [SimpleParameterList](https://github.com/pgjdbc/pgjdbc/blob/master/pgjdbc/src/main/java/org/postgresql/core/v3/SimpleParameterList.java)):
- A `json`/`jsonb` parameter can only be passed as text: `setObject(i, string, Types.OTHER)` binds it as `UNSPECIFIED`
  (the server infers jsonb, no connection property needed); `PGobject("jsonb")` is text-only; `setBytes` means `bytea`.
- pgjdbc encodes every String parameter with `String.getBytes(UTF_8)` when sending; there is no public API to pass
  pre-encoded text bytes. A custom `PGBinaryObject` with `binaryTransferEnable` could send jsonb binary format
  (version byte + text), but `jsonb_recv` still parses the text on the server [verified in code, untested; not worth it].
- So for writing to PostgreSQL the right output is the **String** mpjsons already returns; recommended binder:
  `ParameterBinderFactory[Json](j => (ps, i) => ps.setObject(i, j.value, java.sql.Types.OTHER))`
  ([ScalikeJDBC ParameterBinderFactory](https://github.com/scalikejdbc/scalikejdbc/blob/master/scalikejdbc-core/src/main/scala/scalikejdbc/ParameterBinderFactory.scala)).
  (For reading, `rs.bytes` gives the raw UTF-8 — useful only once mpjsons can parse bytes; see deserialization-research.md #4.)

## 6. Proposals, ranked

All keep the existing API, output and the `JsonTypeSerializer` SPI (custom serializers keep writing into a `StringBuilder`).
Gains are [est] unless marked.

| # | Proposal | Evidence | Expected gain | Effort | Risk |
|---|---|---|---|---|---|
| S1 | **Generated field readers** (mirror of `FieldWriters`): constant getter `MethodHandle`s in a hidden class instead of `Field.get`/`getX` | 10–12% of orders/User time is reflective reads; the same change gave +13% on reads | 10–15% on bean-heavy JSON | 1–2 days | low |
| S2 | **Primitive arrays and primitive collections without boxing** (`Array[Double/Long/Int]` writers) | 5% boxing + generic iterator on numbers | 5–15% on numeric arrays, less garbage | hours | low |
| S3 | **Byte output, stage 1 API**: `serializeToBytes(obj): Array[Byte]` (UTF-8) on `MPJsons`/`StaticSerializer`, plus `serializeToStream(obj, out: OutputStream)`; implemented as builder → bytes at first | gives callers one method; Pekko integration below | none at first (same copies as caller does today) | hours | none |
| S4 | **`mpjsons-pekko-http` module** (separate artifact, no Pekko dependency in core): `ToEntityMarshaller` with `Marshaller.withFixedContentType(application/json)` returning `HttpEntity.Strict(…, ByteString.fromArrayUnsafe(serializeToBytes(x)))`, plus a streaming variant for `Source[T, _]` | pekko-http-json's jsoniter module does exactly this | removes the copy that `HttpEntity(ct, bytes)` would add; ergonomic `complete(x)` | 1–2 days | low |
| S5 | **Native UTF-8 writer** behind S3: built-in serializers write bytes into a growable buffer (bulk-copy ASCII runs via `String.getBytes(int,int,byte[],int)` after the escape scan, pre-encoded `"name":` byte arrays, UTF-8 encoding of non-ASCII inline); custom serializers bridged through a temporary `StringBuilder` | spikes: avoids 26 µs / 228 KB per 114 KB document and UTF-16 inflation (3x memory); jsoniter-scala, Jackson, DSL-JSON all write bytes | 8–15% for HTTP output of large documents, 30–45% less allocation | 2–3 weeks | medium (two writer paths per type) |
| S6 | **Buffer reuse without ThreadLocals** for S5 (small lock-free pool or keep `OutputSizeHint` presizing) | JEP 444 guidance; Jackson 3 moved off ThreadLocal pools | 5–10% on small messages | days | low |
| S7 | **Whole-serializer generation per bean** (Sonic-style; extends S1): one hidden class writing all fields with constant getters and pre-encoded names, no per-field virtual call | jsoniter-scala NestedStructsWriting 4.6x Jackson [pub] shows the ceiling | 1.5x+ on bean-heavy JSON | 3–6 weeks | medium |
| S8 | Own integer/double formatting (Anhalt / xjb / Ryu) | JDK 25 already uses Schubfach and allocation-free appends | ≤5% except number-heavy output | 1 week | medium (exactness) |

Not recommended: `sun.misc.Unsafe` stores or String-internals access (fastjson2 style, JEP 498), ThreadLocal pools
(virtual threads), binary jsonb parameters for PostgreSQL (the server parses the text anyway).

## 7. Suggested order

1. S1 + S2 (small, low risk, measurable right away).
2. S3 + S4: the API and the Pekko marshaller; useful immediately and the place where S5 later plugs in.
3. S5 (+S6) when large HTTP responses matter; validate with the same differential fuzzing, comparing bytes to
   `serialize(x).getBytes(UTF_8)` for every fixture.
4. For ScalikeJDBC keep `serialize` (String) and the `setObject(…, Types.OTHER)` binder.

## 8. Done so far

- S1 and S2 are implemented (performance.md §4c) [measured, vs 0.7.2]:
  - `SimpleBean` +19%, typed shapes +21%, static `User` +15%, 200 orders +8%;
  - numbers +5% with 32% less allocation;
  - output identical to 0.7.2 in 480,000 fuzzed values.
- S3 was implemented and removed: without S5 behind it, `serializeToBytes` was only `serialize(obj).getBytes(UTF_8)`.
  Byte output is worth an API only together with S5.
- S5 is implemented: `serializeToBytes` writes UTF-8 directly (performance.md §4d) [measured, vs
  `serialize(x).getBytes(UTF_8)`]:
  - `User` +25%, bean +24%, non-ASCII strings +60%, orders +4%;
  - number-heavy output −2 to −3%;
  - 16–77% less allocation.
  `serialize` keeps the StringBuilder: a String built from the bytes was slower for numbers.
- S4 is not planned. It would add no dependency to the core artifact: it would be a separate one depending on
  `pekko-http`. An application with its own JSON support trait needs only a few lines (README, "With Pekko HTTP").
- Found while measuring the API path for Pekko users: the 0.7.3 TypeTag cache duplicated Scala's own (2.13.7+), and
  comparing TypeTags in the `Nothing` guard cost time. Both are fixed, giving `serialize[T]` +7% and `deserialize[T]`
  +7% on a small object [measured]; details in performance.md §4c.
