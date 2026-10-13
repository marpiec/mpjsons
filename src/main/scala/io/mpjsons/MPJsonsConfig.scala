package io.mpjsons

/**
 * Configuration of one MPJsons instance. All format levers default to the historical (0.6.x) wire format;
 * readers accept both the historical and the compact forms wherever the two are distinguishable.
 *
 * @param ignoreNonExistingFields on read, skip JSON keys that do not exist on the target type instead of failing
 * @param nullPolicy              what to do with null bean fields on write (see [[NullPolicy]])
 * @param escapeSlash             write "/" as "\/" (historical) or unescaped (smaller output)
 */
final case class MPJsonsConfig(ignoreNonExistingFields: Boolean = false,
                               nullPolicy: NullPolicy = NullPolicy.Fail,
                               escapeSlash: Boolean = true)
