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

  "Serializer" must "handle deserialization without calling constructors" in {
    val testObject = new TestObject
    testObject.someString = "Some test string"
    val containerObject = new ContainerObject(testObject)

    val json = MPJsonS.serialize(containerObject)
    val deserialized = MPJsonS.deserialize[ContainerObject](json)

    deserialized must not be null
    deserialized.otherString mustEqual containerObject.otherString

  }

}
