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
 */
final case class MPJsonsConfig(ignoreNonExistingFields: Boolean = false,
                               nullPolicy: NullPolicy = NullPolicy.Fail,
                               escapeSlash: Boolean = true,
                               compactOptions: Boolean = false,
                               mapsAsObjects: Boolean = false,
                               isoTime: Boolean = false,
                               strict: Boolean = false)
