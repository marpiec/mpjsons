package pl.mpieciukiewicz.mpjsons

import org.scalatest.{MustMatchers, FlatSpec}

import pl.mpieciukiewicz.mpjsons.impl.deserializer.BeanDeserializer
import pl.mpieciukiewicz.mpjsons.impl.serializer.BeanSerializer
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, SerializerFactory, StringIterator}

import scala.reflect.runtime.universe._

trait SpecialTypeTrait

case class SomeSpecialType(name: String) extends SpecialTypeTrait

class TypedConverter[T <: AnyRef](packageName: String) extends JsonTypeConverter[T] {



  override def serialize(obj: T, jsonBuilder: StringBuilder)
                        (implicit serializerFactory: SerializerFactory): Unit = {
    val simpleName: String = obj.getClass.getSimpleName
    jsonBuilder.append("{\"" +simpleName + "\":")
    BeanSerializer.serialize(obj.asInstanceOf[AnyRef], jsonBuilder)
    jsonBuilder.append('}')
  }

  override def deserialize(jsonIterator: StringIterator, tpe: Type)
                          (implicit deserializerFactory: DeserializerFactory): T = {

    jsonIterator.consumeObjectStart()


    val typeName = extractTypeName(jsonIterator)
    val elementType = TypesUtil.getTypeFromClass(Class.forName(packageName + "." + typeName))

    jsonIterator.skipWhitespaceChars()

    jsonIterator.nextChar()

    jsonIterator.skipWhitespaceChars()
    val value = BeanDeserializer.deserialize(jsonIterator, elementType).asInstanceOf[T]

    jsonIterator.skipWhitespaceChars()
    jsonIterator.nextChar()

    value
  }

  def extractTypeName(jsonIterator: StringIterator)
                     (implicit deserializerFactory: DeserializerFactory): String = {
    val deserializer = deserializerFactory.getDeserializer(typeOf[String])
    jsonIterator.skipWhitespaceChars()
    deserializer.deserialize(jsonIterator, typeOf[String]).asInstanceOf[String]
  }

}

class CustomConverterSupportSpec extends FlatSpec with MustMatchers {



  "Serializer" must "be extendable with custom converters (type converter)" in {
    val mpjsons = new MPJsons
    mpjsons.registerConverter(new TypedConverter[SomeSpecialType](classOf[SomeSpecialType].getPackage.getName))
    val serialized = mpjsons.serialize(SomeSpecialType("Marcin"))
    serialized mustNot be ("""{"name":"Marcin"}""")
    val deserialized = mpjsons.deserialize[SomeSpecialType](serialized)
    deserialized mustBe SomeSpecialType("Marcin")
  }

  "Serializer" must "be extendable with custom converters (super type converter)" in {
    val mpjsons = new MPJsons
    mpjsons.registerSuperclassConverter(new TypedConverter[SpecialTypeTrait](classOf[SpecialTypeTrait].getPackage.getName))
    val serialized = mpjsons.serialize(SomeSpecialType("Marcin"))
    serialized mustNot be ("""{"name":"Marcin"}""")
    val deserialized = mpjsons.deserialize[SomeSpecialType](serialized)
    deserialized mustBe SomeSpecialType("Marcin")
  }

}
