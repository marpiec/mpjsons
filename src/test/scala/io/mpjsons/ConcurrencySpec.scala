package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

import java.time.LocalDateTime
import java.util.concurrent.{ConcurrentLinkedQueue, CountDownLatch, Executors, TimeUnit}

object ConcurrencySpec {
  case class Leaf(a: Int, b: String, c: List[Int], d: Option[String], e: Map[String, Int])
  case class Branch(x: Leaf, y: Vector[Leaf], z: Array[Long])
  case class Misc(pair: (Int, String), either: Either[Int, String], when: LocalDateTime, box: Box[Leaf])
  case class Box[T](value: T, values: List[T])
}

// Top-level because the typed converter resolves subtypes by package + simple name (known-issues §10).
sealed trait Shape
case class Circle(r: Double) extends Shape
case class Square(side: Int) extends Shape

/**
 * Exercises first-time (cold cache) use of many types from several threads at once.
 * Every thread serializes and deserializes the same fixtures; any exception or mismatch fails the test.
 */
class ConcurrencySpec extends AnyFlatSpec {
  import ConcurrencySpec._

  "MPJsons" must "survive concurrent first-time use of many types" in {
    val mpjsons = new MPJsons
    mpjsons.markTypedClass[Shape]

    val leaf = Leaf(1, "one", List(1, 2, 3), Some("x"), Map("k" -> 1))
    val branch = Branch(leaf, Vector(leaf, leaf.copy(a = 2)), Array(1L, 2L))
    val misc = Misc((1, "a"), Right("r"), LocalDateTime.of(2024, 1, 2, 3, 4, 5, 6), Box(leaf, List(leaf)))
    val shapes: List[Shape] = List(Circle(1.5), Square(2))

    val threads = 8
    val iterations = 100
    val start = new CountDownLatch(1)
    val errors = new ConcurrentLinkedQueue[Throwable]()
    val pool = Executors.newFixedThreadPool(threads)

    val futures = (1 to threads).map { _ =>
      pool.submit(new Runnable {
        override def run(): Unit = {
          start.await()
          try {
            var i = 0
            while (i < iterations) {
              mpjsons.deserialize[Leaf](mpjsons.serialize(leaf)) mustBe leaf
              val b = mpjsons.deserialize[Branch](mpjsons.serialize(branch))
              b.x mustBe branch.x
              b.y mustBe branch.y
              b.z.toList mustBe branch.z.toList
              mpjsons.deserialize[Misc](mpjsons.serialize(misc)) mustBe misc
              mpjsons.deserialize[List[Shape]](mpjsons.serialize(shapes)) mustBe shapes
              mpjsons.deserialize[Box[Int]](mpjsons.serialize(Box(1, List(2)))) mustBe Box(1, List(2))
              mpjsons.deserialize[Box[String]](mpjsons.serialize(Box("a", List("b")))) mustBe Box("a", List("b"))
              i += 1
            }
          } catch {
            case t: Throwable => errors.add(t)
          }
        }
      })
    }

    start.countDown()
    futures.foreach(_.get(60, TimeUnit.SECONDS))
    pool.shutdown()

    if (!errors.isEmpty) {
      val first = errors.peek()
      fail(s"${errors.size} thread(s) failed, first: $first", first)
    }
  }
}
