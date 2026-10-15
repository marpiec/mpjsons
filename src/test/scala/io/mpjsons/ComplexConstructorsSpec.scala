package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

/**
 * @author Marcin Pieciukiewicz
 */

class TestObject {
  var someString: String = _
}

class ContainerObject(testObject: TestObject) {
  var otherString: String = testObject.someString
}

class ComplexConstructorsSpec extends AnyFlatSpec {

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
