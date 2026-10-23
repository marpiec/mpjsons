package io.mpjsons.bench

import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

/**
 * Compares serialization of the current implementation with the legacy copy of 0.7.2 (src/legacy/scala/io/mpjsons/legacy).
 * `*Bytes` benchmarks add the UTF-8 encoding a caller needs for HTTP or JDBC (String.getBytes(UTF_8)).
 */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
class SerializationBench {

  @Param(Array("legacy", "current"))
  var impl: String = _

  var userStatic: AnyRef => String = _
  var userApi: AnyRef => String = _
  var ordersStatic: AnyRef => String = _
  var beanStatic: AnyRef => String = _
  var numbersStatic: AnyRef => String = _
  var textsStatic: AnyRef => String = _
  var drawingStatic: AnyRef => String = _

  @Setup
  def setup(): Unit = {
    if (impl == "legacy") {
      val m = new io.mpjsons.legacy.MPJsons()
      m.markTypedClass[Shape]()
      userStatic = staticOf(m.buildStaticSerializer[User])
      userApi = o => m.serialize[User](o.asInstanceOf[User])
      ordersStatic = staticOf(m.buildStaticSerializer[Orders])
      beanStatic = staticOf(m.buildStaticSerializer[SimpleBean])
      numbersStatic = staticOf(m.buildStaticSerializer[Numbers])
      textsStatic = staticOf(m.buildStaticSerializer[Texts])
      drawingStatic = staticOf(m.buildStaticSerializer[Drawing])
    } else {
      val m = new io.mpjsons.MPJsons()
      m.markTypedClass[Shape]()
      userStatic = staticOf(m.buildStaticSerializer[User])
      userApi = o => m.serialize[User](o.asInstanceOf[User])
      ordersStatic = staticOf(m.buildStaticSerializer[Orders])
      beanStatic = staticOf(m.buildStaticSerializer[SimpleBean])
      numbersStatic = staticOf(m.buildStaticSerializer[Numbers])
      textsStatic = staticOf(m.buildStaticSerializer[Texts])
      drawingStatic = staticOf(m.buildStaticSerializer[Drawing])
    }
  }

  private def staticOf[T](s: io.mpjsons.legacy.StaticSerializer[T]): AnyRef => String = o => s.serialize(o.asInstanceOf[T])

  private def staticOf[T](s: io.mpjsons.StaticSerializer[T]): AnyRef => String = o => s.serialize(o.asInstanceOf[T])

  @Benchmark
  def user(): String = userStatic(Data.smallUser)

  @Benchmark
  def userViaApi(): String = userApi(Data.smallUser)

  @Benchmark
  def orders(): String = ordersStatic(Data.orders)

  @Benchmark
  def ordersBytes(): Array[Byte] = ordersStatic(Data.orders).getBytes(StandardCharsets.UTF_8)

  @Benchmark
  def bean(): String = beanStatic(Data.simpleBean)

  @Benchmark
  def numbers(): String = numbersStatic(Data.numbers)

  @Benchmark
  def texts(): String = textsStatic(Data.texts)

  @Benchmark
  def textsBytes(): Array[Byte] = textsStatic(Data.texts).getBytes(StandardCharsets.UTF_8)

  @Benchmark
  def typedDrawing(): String = drawingStatic(Data.drawing)
}
