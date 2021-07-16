package io.mpjsons

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._

case class UtfControlCharacterHolder(text: String)

class UtfControlCharacterSpec extends FlatSpec {

  val mpjsons = new MPJsons

  "Serializer" must "handle tuples deserialization with whitespaces" in {

    val json = " {\"text\": \"Hello" +
      "\\u0000\\u0001\\u0002\\u0003\\u0004\\u0005\\u0006\\u0007" +
      "\\u0008\\u0009\\u000a\\u000b\\u000c\\u000d\\u000e\\u000f" +
      "\\u0010\\u0011\\u0012\\u0013\\u0014\\u0015\\u0016\\u0017" +
      "\\u0018\\u0019\\u001a\\u001b\\u001c\\u001d\\u001e\\u001f" +
      "\\u1234" +
      "ABC\" }  "

    val deserialized = mpjsons.deserialize[UtfControlCharacterHolder](json)

    deserialized.text mustBe "Hello" +
      "\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007" +
      "\u0008\u0009\u000a\u000b\u000c\u000d\u000e\u000f" +
      "\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017" +
      "\u0018\u0019\u001a\u001b\u001c\u001d\u001e\u001f" +
      "\u1234" +
      "ABC"
  }

}
