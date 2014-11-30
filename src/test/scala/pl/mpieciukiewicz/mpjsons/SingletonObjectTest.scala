package pl.mpieciukiewicz.mpjsons

import org.testng.Assert
import org.testng.Assert.{fail, assertEquals}
import org.testng.annotations.Test

class User(val name: String)

case object UserMarcin extends User("Marcin")

case object UserRegisterMessage

@Test
class ObjectTest {

  def testObjectSerializationAndDeserialization() {

    val userMarcinJson = MPJson.serialize(UserMarcin)

    assertEquals(userMarcinJson, "{}")

    val deserializedUserMarcin = MPJson.deserialize(userMarcinJson, UserMarcin.getClass)

    assertEquals(UserMarcin, deserializedUserMarcin)

    MPJson.deserialize("{}", UserMarcin.getClass)

    assertEquals(UserMarcin, deserializedUserMarcin)

    try {
      MPJson.deserialize( """{"name":"Marcin"}""", UserMarcin.getClass)
      fail("Expected Exception")
    } catch {
      case _:Exception => ()
    }
  }

  def testNoValueObjectSerializationAndDeserialization() {

    val message = MPJson.serialize(UserRegisterMessage)

    assertEquals(message, "{}")

    val deserializedMessage = MPJson.deserialize(message, UserRegisterMessage.getClass)

    assertEquals(UserRegisterMessage, deserializedMessage)
  }

}
