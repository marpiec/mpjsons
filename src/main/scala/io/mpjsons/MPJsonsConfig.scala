package io.mpjsons

/**
 * Configuration of one MPJsons instance. All format levers default to the historical (0.6.x) wire format;
 * readers accept both the historical and the compact forms wherever the two are distinguishable.
 *
 * @param ignoreNonExistingFields on read, skip JSON keys that do not exist on the target type instead of failing
 * @param nullPolicy              what to do with null bean fields on write (see [[NullPolicy]])
 * @param escapeSlash             write "/" as "\/" (historical) or unescaped (smaller output)
 * @param compactOptions          write Some(v) as v and None as an omitted bean field (or null elsewhere) instead of
 *                                {"value":v} / {}; an Option nested directly in an Option keeps the historical form.
 *                                On read, an inline value is recognised whenever it cannot be the historical object
 *                                form; for element types that are themselves JSON objects the compact setting decides.
 * @param mapsAsObjects           write maps whose key type is String, a number, Boolean or Char as a JSON object
 *                                {"key":value} instead of an array of [key,value] pairs; other key types keep the
 *                                pair form. Both forms are always accepted on read.
 * @param isoTime                 write java.time LocalDate/LocalTime/LocalDateTime/Duration as ISO-8601 strings
 *                                ("2023-07-23", "12:30:17", "2023-07-23T12:30:17", "PT2M3S") instead of objects.
 *                                Both forms are always accepted on read.
 * @param strict                  on read, reject missing commas between members or elements, trailing commas and
 *                                any content after the root value. Bare (unquoted) keys and whitespace are always
 *                                accepted. Off by default (historical, lenient parsing).
 * @param maxDepth                on read, the maximum nesting of values that contain other values (objects, collections,
 *                                maps, Options, tuples, Either), including skipped unknown fields; deeper input fails
 *                                with an error instead of a StackOverflowError. 0 (default) means unlimited.
 * @param caseClassesViaConstructor on read, build case classes by calling their primary constructor with the parsed
 *                                values instead of allocating an instance and writing its final fields reflectively
 *                                (future JDKs restrict final-field mutation). Absent keys still yield zero / false /
 *                                null / None. Off by default; costs some boxing per value.
 * @param omitDefaults            on write, leave out every bean field whose value equals what reading `{}` would
 *                                produce for that type (the no-arg constructor's initialisers, or zero / false /
 *                                null / None for types constructed without a constructor). Lossless for round
 *                                trips through this library; takes precedence over `nullPolicy` for such fields.
 * @param maxOutputBytes          on write, the largest output allowed, in bytes (100 MiB by default; String output
 *                                counts characters, none of which takes less than a byte in UTF-8). Larger output fails
 *                                with a [[JsonTooLargeException]] while it is being built, instead of growing until the
 *                                JDK's own limit (about 2^30 UTF-16 characters for a String) throws an
 *                                OutOfMemoryError. Each serialize method also takes a `maxBytes` argument for one call.
 *                                Keep it well below that JDK limit: String output is checked every 1024 collection
 *                                elements, so it can run a little past the limit before failing.
 * @param streamChunkBytes        for `serializeToStream`, how many bytes are collected before they are written to the
 *                                stream (64 KiB by default). One write can be up to about twice this, and a single
 *                                string longer than this is written whole. Smaller values use less memory per call in
 *                                progress and write to the stream more often.
 */
final case class MPJsonsConfig(ignoreNonExistingFields: Boolean = false,
                               nullPolicy: NullPolicy = NullPolicy.Fail,
                               escapeSlash: Boolean = true,
                               compactOptions: Boolean = false,
                               mapsAsObjects: Boolean = false,
                               isoTime: Boolean = false,
                               strict: Boolean = false,
                               omitDefaults: Boolean = false,
                               caseClassesViaConstructor: Boolean = false,
                               maxDepth: Int = 0,
                               maxOutputBytes: Int = 100 * 1024 * 1024,
                               streamChunkBytes: Int = 64 * 1024) {
  require(maxOutputBytes > 0, "maxOutputBytes must be positive, was " + maxOutputBytes)
  require(streamChunkBytes > 0, "streamChunkBytes must be positive, was " + streamChunkBytes)
}
