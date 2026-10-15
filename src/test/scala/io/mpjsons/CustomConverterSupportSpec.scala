package io.mpjsons

import io.mpjsons.impl.deserializer.BeanDeserializer
import io.mpjsons.impl.serializer.BeanSerializer
import io.mpjsons.impl.util.{Context, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, SerializerFactory, StringIterator}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

import scala.reflect.runtime.universe._

trait SpecialTypeTrait

case class SomeSpecialType(name: String) extends SpecialTypeTrait

class TypedSerializer[T <: AnyRef](packageName: String, serializerFactory: SerializerFactory)
                                  (implicit tag: TypeTag[T])
                                 extends JsonTypeSerializer[T] {

  val innerElementSerializer = new BeanSerializer(serializerFactory, tag.tpe, Context(List.empty, Map.empty))

  override def serialize(obj: T, jsonBuilder: StringBuilder): Unit = {
    val simpleName: String = obj.getClass.getSimpleName
    jsonBuilder.append("{\"" + simpleName + "\":")

    innerElementSerializer.serialize(obj, jsonBuilder)
    jsonBuilder.append('}')
  }
}


class TypedDeserializer[T <: AnyRef](packageName: String, deserializerFactory: DeserializerFactory)
    extends JsonTypeDeserializer[T] {

  override def deserialize(jsonIterator: StringIterator): T = {

    jsonIterator.consumeObjectStart()

    val typeName = extractTypeName(jsonIterator)
    val elementType = TypesUtil.getTypeFromClass(Class.forName(packageName + "." + typeName))

    jsonIterator.skipWhitespaceChars()

    jsonIterator.nextChar()

    jsonIterator.skipWhitespaceChars()
    val value = new BeanDeserializer[T](deserializerFactory, elementType, Context(List.empty, Map.empty), false).deserialize(jsonIterator).asInstanceOf[T]

    jsonIterator.skipWhitespaceChars()
    jsonIterator.nextChar()

    value
  }

  def extractTypeName(jsonIterator: StringIterator): String = {
    val deserializer = deserializerFactory.getDeserializer[String](typeOf[String], Context(List.empty, Map.empty))
    jsonIterator.skipWhitespaceChars()
    deserializer.deserialize(jsonIterator)
  }

}

class CustomConverterSupportSpec extends AnyFlatSpec {



  "Serializer" must "be extendable with custom converters (type converter)" in {
    val mpjsons = new MPJsons
    mpjsons.registerConverter(sf => new TypedSerializer[SomeSpecialType](classOf[SomeSpecialType].getPackage.getName, sf),
                              df => new TypedDeserializer[SomeSpecialType](classOf[SomeSpecialType].getPackage.getName, df))
    val serialized = mpjsons.serialize(SomeSpecialType("Marcin"))
    serialized mustNot be ("""{"name":"Marcin"}""")
    val deserialized = mpjsons.deserialize[SomeSpecialType](serialized)
    deserialized mustBe SomeSpecialType("Marcin")
  }

  "Serializer" must "be extendable with custom converters (super type converter)" in {
    val mpjsons = new MPJsons
    mpjsons.registerConverter(sf => new TypedSerializer[SomeSpecialType](classOf[SomeSpecialType].getPackage.getName, sf),
                                        df => new TypedDeserializer[SomeSpecialType](classOf[SomeSpecialType].getPackage.getName, df))
    val serialized = mpjsons.serialize(SomeSpecialType("Marcin"))
    serialized mustNot be ("""{"name":"Marcin"}""")
    val deserialized = mpjsons.deserialize[SomeSpecialType](serialized)
    deserialized mustBe SomeSpecialType("Marcin")
  }

}
