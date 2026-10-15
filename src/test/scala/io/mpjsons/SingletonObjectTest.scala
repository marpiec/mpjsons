package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

// For now singleton objects are not supported!


//class User(val name: String)
//
//case object UserMarcin extends User("Marcin")
//
//case object UserRegisterMessage
//
//class SingletonObjectSpec extends AnyFlatSpec {
//
//  "Serializer" must "handle singleton Objects" in {
//
//    val userMarcinJson = MPJson.serialize(UserMarcin)
//
//    userMarcinJson mustBe "{}"
//
//    val deserializedUserMarcin = MPJson.deserialize[AnyRef](userMarcinJson)
//
//    deserializedUserMarcin mustBe UserMarcin
//
//    MPJson.deserialize[User]("{}")
//
//    deserializedUserMarcin mustBe UserMarcin
//
//    try {
//      MPJson.deserialize[User]( """{"name":"Marcin"}""")
//      fail("Expected Exception")
//    } catch {
//      case _:Exception => ()
//    }
//  }
//
//  "Serializer" must "handle singleton Objects without value" in {
//
//    val message = MPJson.serialize(UserRegisterMessage)
//
//    message mustBe "{}"
//
//    val deserializedMessage = MPJson.deserialize[AnyRef](message)
//
//    deserializedMessage mustBe UserRegisterMessage
//  }
//
//}
