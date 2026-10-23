package io.mpjsons

import io.mpjsons.impl.serializer.FieldReaders
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

object FieldReadersSpec {
  case class AllTypes(i: Int, l: Long, d: Double, f: Float, z: Boolean, s: Short, b: Byte, c: Char, text: String, list: List[Int])

  class Parent {
    var inherited: Int = 7
  }

  class PrivateBean extends Parent {
    private var hidden: Long = 42L
    var text: String = "t"
    def hiddenValue: Long = hidden
  }
}

class FieldReadersSpec extends AnyFlatSpec with Matchers {

  import FieldReadersSpec._

  private def fieldsOf(c: Class[_]) = c.getDeclaredFields.filterNot(f => java.lang.reflect.Modifier.isStatic(f.getModifiers)).map { f =>
    f.setAccessible(true)
    f
  }

  private val allTypes = AllTypes(1, 2L, 3.5, 4.5f, z = true, 6, 7, 'x', "hello", List(1, 2))

  "FieldReaders" must "be generated as a hidden class and read all field types, including final ones" in {
    val fields = fieldsOf(classOf[AllTypes])
    val readers = FieldReaders.forFields(fields)
    readers.getClass.isHidden mustBe true

    def index(name: String) = fields.indexWhere(_.getName == name)
    readers.getInt(allTypes, index("i")) mustBe 1
    readers.getLong(allTypes, index("l")) mustBe 2L
    readers.getDouble(allTypes, index("d")) mustBe 3.5
    readers.getFloat(allTypes, index("f")) mustBe 4.5f
    readers.getBoolean(allTypes, index("z")) mustBe true
    readers.getShort(allTypes, index("s")) mustBe 6.toShort
    readers.getByte(allTypes, index("b")) mustBe 7.toByte
    readers.getChar(allTypes, index("c")) mustBe 'x'
    readers.getObject(allTypes, index("text")) mustBe "hello"
    readers.getObject(allTypes, index("list")) mustBe List(1, 2)
  }

  it must "box primitive values read as objects, like Field.get" in {
    val fields = fieldsOf(classOf[AllTypes])
    val readers = FieldReaders.forFields(fields)
    fields.indices.foreach { index =>
      readers.getObject(allTypes, index) mustBe fields(index).get(allTypes)
    }
    readers.getObject(allTypes, fields.indexWhere(_.getName == "i")) mustBe a[java.lang.Integer]
  }

  it must "read private and inherited fields" in {
    val fields = fieldsOf(classOf[PrivateBean]) ++ fieldsOf(classOf[Parent])
    val readers = FieldReaders.forFields(fields)
    val bean = new PrivateBean
    readers.getLong(bean, fields.indexWhere(_.getName.endsWith("hidden"))) mustBe bean.hiddenValue
    readers.getObject(bean, fields.indexWhere(_.getName == "text")) mustBe "t"
    readers.getInt(bean, fields.indexWhere(_.getName == "inherited")) mustBe 7
  }

  it must "reject an index of a field of another type" in {
    val fields = fieldsOf(classOf[AllTypes])
    val readers = FieldReaders.forFields(fields)
    an[IllegalStateException] must be thrownBy readers.getInt(allTypes, fields.indexWhere(_.getName == "l"))
    an[IllegalStateException] must be thrownBy readers.getObject(allTypes, fields.length)
  }

  it must "be used when serializing case classes" in {
    new MPJsons().serialize(allTypes) mustBe """{"i":1,"l":2,"d":3.5,"f":4.5,"z":true,"s":6,"b":7,"c":"x","text":"hello","list":[1,2]}"""
  }
}
