package pl.mpieciukiewicz.mpjsons

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._

class User(val name: String)

case object UserMarcin extends User("Marcin")

case object UserRegisterMessage

class SingletonObjectSpec extends FlatSpec {

  "Serializer" must "handle singleton Objects" in {

    val userMarcinJson = MPJson.serialize()

    userMarcinJson mustBe "{}"

    val deserializedUserMarcin = MPJson.deserialize[User](userMarcinJson)

    deserializedUserMarcin mustBe UserMarcin

    MPJson.deserialize[User]("{}")

    deserializedUserMarcin mustBe UserMarcin

    try {
      MPJson.deserialize[User]( """{"name":"Marcin"}""")
      fail("Expected Exception")
    } catch {
      case _:Exception => ()
    }
  }

  "Serializer" must "handle singleton Objects without value" in {

    val message = MPJson.serialize(UserRegisterMessage)

    message mustBe "{}"

    val deserializedMessage = MPJson.deserialize[AnyRef](message)

    deserializedMessage mustBe UserRegisterMessage
  }

}
