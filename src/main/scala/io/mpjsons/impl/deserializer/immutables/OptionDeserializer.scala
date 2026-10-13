package io.mpjsons.impl.deserializer.immutables

import io.mpjsons.{JsonTypeDeserializer, MPJsonsConfig}
import io.mpjsons.impl.deserializer.IdentifierDeserializer
import io.mpjsons.impl.util.{Context, JsonShapes, NullValues, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * Reads both Option forms:
 *  - historical: {} -> None, {"value":v} -> Some(v);
 *  - compact: null -> None, v -> Some(v).
 * A value starting with '{' is taken as the historical form unless `compactOptions` is on and the element type is
 * itself written as a JSON object (then it is the inline element, including a nested Option in its historical form).
 * An Option nested directly inside another Option is always read in the historical form (matching OptionSerializer).
 * @author Marcin Pieciukiewicz
 */

class OptionDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type, context: Context, config: MPJsonsConfig)
  extends JsonTypeDeserializer[Option[E]] {

  def this(deserializerFactory: DeserializerFactory, tpe: Type, context: Context) = this(deserializerFactory, tpe, context, MPJsonsConfig())

  private val compact = config.compactOptions
  private val subType = TypesUtil.getSubElementsType(tpe)
  private val subTypeDeserializer: JsonTypeDeserializer[E] = {
    if (compact && subType <:< OptionDeserializer.optionType) {
      new OptionDeserializer[Any](deserializerFactory, subType, context, config.copy(compactOptions = false)).asInstanceOf[JsonTypeDeserializer[E]]
    } else {
      deserializerFactory.getDeserializer(subType, context).asInstanceOf[JsonTypeDeserializer[E]]
    }
  }
  private val subTypeNull = NullValues.forType(subType, config)
  private val braceStartsInlineValue = compact && JsonShapes.isObjectLike(subType, config)

  override def deserialize(jsonIterator: StringIterator): Option[E] = {

    if (jsonIterator.tryConsumeNull()) {
      return None
    }

    if (jsonIterator.currentChar != '{' || braceStartsInlineValue) {
      return Some(subTypeDeserializer.deserialize(jsonIterator))
    }

    jsonIterator.consumeObjectStart()

    jsonIterator.skipWhitespaceChars()

    val option = if (jsonIterator.currentChar == '}') {
      None
    } else {
      val identifier = IdentifierDeserializer.deserialize(jsonIterator)

      if (identifier != "value") {
        throw new IllegalArgumentException("Expected 'value' but was = " + identifier)
      }

      jsonIterator.consumeFieldValueSeparator()
      val value = NullValues.read(jsonIterator, subTypeNull, subTypeDeserializer)
      Some[E](value)
    }

    jsonIterator.skipWhitespaceChars()
    jsonIterator.nextCharOrNullIfLast
    option
  }
}

object OptionDeserializer {
  private val optionType = typeOf[Option[_]]
}
