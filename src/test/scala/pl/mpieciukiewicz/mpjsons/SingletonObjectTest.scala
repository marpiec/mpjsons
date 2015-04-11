package pl.mpieciukiewicz.mpjsons

import org.testng.Assert
import org.testng.Assert.{fail, assertEquals}
import org.testng.annotations.Test
import scala.reflect.runtime.universe._

class User(val name: String)

case object UserMarcin extends User("Marcin")

case object UserRegisterMessage

@Test
class ObjectTest {

  def testObjectSerializationAndDeserialization() {

    val userMarcinJson = MPJson.serialize()

    assertEquals(userMarcinJson, "{}")

    val deserializedUserMarcin = MPJson.deserialize[User](userMarcinJson)

    assertEquals(UserMarcin, deserializedUserMarcin)

    MPJson.deserialize[User]("{}")

    assertEquals(UserMarcin, deserializedUserMarcin)

    try {
      MPJson.deserialize[User]( """{"name":"Marcin"}""")
      fail("Expected Exception")
    } catch {
      case _:Exception => ()
    }
  }

  def testNoValueObjectSerializationAndDeserialization() {

    val message = MPJson.serialize(UserRegisterMessage)

    assertEquals(message, "{}")

    val deserializedMessage = MPJson.deserialize(message)

    assertEquals(UserRegisterMessage, deserializedMessage)
  }

}
