package io.mpjsons.bench

case class User(id: Long, firstName: String, lastName: String, email: String, age: Int, active: Boolean, score: Double)

case class Item(sku: String, name: String, quantity: Int, price: Double, discount: Option[Double])

case class Order(id: Long, customer: User, items: List[Item], tags: Set[String],
                 attributes: Map[String, String], note: Option[String], createdAt: Long)

case class Orders(orders: Vector[Order])

case class OrderProjection(id: Long, createdAt: Long)

case class OrdersProjection(orders: Vector[OrderProjection])

class InnerBean {
  var intValue: Int = _
  var stringValue: String = _
}

class SimpleBean {
  var charValue: Char = _
  var longValue: Long = _
  var intValue: Int = _
  var stringValue: String = _
  var booleanValue: Boolean = _
  var innerObject: InnerBean = _
  var arrayObject: Array[String] = _
  var arrayPrimitive: Array[Long] = _
  var listObject: List[String] = _
  var listPrimitive: List[Int] = _
  var emptyArray: Array[Long] = _
}

case class Numbers(doubles: Array[Double], longs: Array[Long], ints: List[Int])

case class Texts(texts: List[String])

trait Shape

case class Circle(x: Double, y: Double, r: Double) extends Shape

case class Rect(x: Double, y: Double, w: Double, h: Double) extends Shape

case class Drawing(shapes: List[Shape])

object Data {

  private val random = new scala.util.Random(42)

  private def word(): String = {
    val length = 3 + random.nextInt(8)
    new String(Array.fill(length)(('a' + random.nextInt(26)).toChar))
  }

  private def user(i: Int): User =
    User(1000000L + i, word().capitalize, word().capitalize, word() + "@" + word() + ".com",
      18 + random.nextInt(60), random.nextBoolean(), random.nextInt(100000) / 100.0)

  val smallUser: User = user(1)

  val orders: Orders = Orders(Vector.tabulate(200) { i =>
    Order(
      id = 5000000000L + i,
      customer = user(i),
      items = List.tabulate(1 + random.nextInt(5)) { j =>
        Item("SKU-" + random.nextInt(100000), word() + " " + word(), 1 + random.nextInt(10),
          random.nextInt(100000) / 100.0, if (random.nextBoolean()) Some(random.nextInt(50) / 100.0) else None)
      },
      tags = List.fill(random.nextInt(4))(word()).toSet,
      attributes = Map((0 until random.nextInt(4)).map(_ => word() -> word()): _*),
      note = if (random.nextBoolean()) Some(word() + " " + word() + " " + word()) else None,
      createdAt = 1700000000000L + random.nextInt(1000000000)
    )
  })

  val simpleBean: SimpleBean = {
    val sdo = new SimpleBean
    sdo.charValue = 'M'
    sdo.longValue = 1234567891234L
    sdo.intValue = 1111
    sdo.stringValue = "Hello Json \n\" parser\\serializer \""
    sdo.booleanValue = true
    sdo.innerObject = new InnerBean
    sdo.innerObject.intValue = -2222
    sdo.innerObject.stringValue = "inner string"
    sdo.arrayObject = Array[String]("Be", "or", "not to be")
    sdo.arrayPrimitive = Array[Long](6, 8, 10)
    sdo.listObject = List[String]("Hello", "json", "serializer")
    sdo.listPrimitive = List[Int](15, 30, 1)
    sdo.emptyArray = Array()
    sdo
  }

  val numbers: Numbers = Numbers(
    Array.fill(1000)(random.nextDouble() * 1000000 - 500000),
    Array.fill(1000)(random.nextLong()),
    List.fill(1000)(random.nextInt()))

  val texts: Texts = Texts(List.tabulate(300) { i =>
    if (i % 3 == 0) "Line \"" + word() + "\"\n\ttab \\ slash / " + word() + " ąęó €"
    else word() + " " + word() + " " + word() + " " + word() + " " + word()
  })

  val drawing: Drawing = Drawing(List.tabulate(500) { i =>
    if (i % 2 == 0) Circle(random.nextInt(1000), random.nextInt(1000), random.nextInt(100) / 10.0)
    else Rect(random.nextInt(1000), random.nextInt(1000), random.nextInt(500), random.nextInt(500))
  })
}
