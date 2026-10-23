package io.mpjsons

import io.mpjsons.impl.deserializer.FieldWriters
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

object FieldWritersSpec {
  case class AllTypes(i: Int, l: Long, d: Double, f: Float, z: Boolean, s: Short, b: Byte, c: Char, text: String, list: List[Int])

  class VarBean {
    var i: Int = _
    var text: String = _
  }
}

class FieldWritersSpec extends AnyFlatSpec with Matchers {

  import FieldWritersSpec._

  private def fieldsOf(c: Class[_]) = c.getDeclaredFields.filterNot(f => java.lang.reflect.Modifier.isStatic(f.getModifiers)).map { f =>
    f.setAccessible(true)
    f
  }

  "FieldWriters" must "be generated as a hidden class and write all field types, including final ones" in {
    val fields = fieldsOf(classOf[AllTypes])
    val writers = FieldWriters.forFields(fields)
    writers.getClass.isHidden mustBe true

    val instance = new AllTypes(0, 0, 0, 0, false, 0, 0, ' ', null, null)
    def index(name: String) = fields.indexWhere(_.getName == name)
    writers.setInt(instance, index("i"), 1)
    writers.setLong(instance, index("l"), 2L)
    writers.setDouble(instance, index("d"), 3.5)
    writers.setFloat(instance, index("f"), 4.5f)
    writers.setBoolean(instance, index("z"), true)
    writers.setShort(instance, index("s"), 6.toShort)
    writers.setByte(instance, index("b"), 7.toByte)
    writers.setChar(instance, index("c"), 'x')
    writers.setObject(instance, index("text"), "hello")
    writers.setObject(instance, index("list"), List(1, 2))

    instance mustBe AllTypes(1, 2L, 3.5, 4.5f, z = true, 6, 7, 'x', "hello", List(1, 2))
  }

  it must "be used when deserializing beans and case classes" in {
    val mpjsons = new MPJsons
    mpjsons.deserialize[AllTypes]("""{"i":1,"l":2,"d":3.5,"f":4.5,"z":true,"s":6,"b":7,"c":"x","text":"hello","list":[1,2]}""") mustBe
      AllTypes(1, 2L, 3.5, 4.5f, z = true, 6, 7, 'x', "hello", List(1, 2))
    val bean = mpjsons.deserialize[VarBean]("""{"i":5,"text":"t"}""")
    bean.i mustBe 5
    bean.text mustBe "t"
  }

  it must "reject a value of a wrong type for a reference field" in {
    val fields = fieldsOf(classOf[VarBean])
    val writers = FieldWriters.forFields(fields)
    a[ClassCastException] must be thrownBy writers.setObject(new VarBean, fields.indexWhere(_.getName == "text"), Integer.valueOf(1))
  }
}
