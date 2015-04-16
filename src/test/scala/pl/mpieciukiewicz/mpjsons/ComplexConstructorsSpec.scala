package pl.mpieciukiewicz.mpjsons

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._

/**
 * @author Marcin Pieciukiewicz
 */

class TestObject {
  var someString: String = _
}

class ContainerObject(testObject: TestObject) {
  var otherString: String = testObject.someString
}

class ComplexConstructorsSpec extends FlatSpec {

  val mpjsons = new MPJsons

  "Serializer" must "handle deserialization without calling constructors" in {
    val testObject = new TestObject
    testObject.someString = "Some test string"
    val containerObject = new ContainerObject(testObject)

    val json = mpjsons.serialize(containerObject)
    val deserialized = mpjsons.deserialize[ContainerObject](json)

    deserialized must not be null
    deserialized.otherString mustEqual containerObject.otherString

  }

}
