package pl.mpieciukiewicz.mpjsons

import org.testng.Assert._
import org.testng.annotations.Test

/**
 * @author Marcin Pieciukiewicz
 */

class TestObject {
  var someString: String = _
}

class ContainerObject(testObject: TestObject) {
  var otherString: String = testObject.someString
}

@Test
class MPJsonComplexConstructorsTest {

  def testDeserializationWithoutCallingConstructors() {
    val testObject = new TestObject
    testObject.someString = "Some test string"
    val containerObject = new ContainerObject(testObject)

    val json = MPJson.serialize(containerObject)
    val deserialized = MPJson.deserialize(json, classOf[ContainerObject]).asInstanceOf[ContainerObject]

    assertNotNull(deserialized)
    assertEquals(deserialized.otherString, containerObject.otherString)

  }

}
