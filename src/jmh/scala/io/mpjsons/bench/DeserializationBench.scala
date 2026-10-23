package io.mpjsons.bench

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

import scala.reflect.runtime.universe._

/**
 * Compares deserialization of the current implementation with the legacy copy of 0.7.2 (src/legacy/scala/io/mpjsons/legacy).
 * Static benchmarks use buildStaticDeserializer, "Api" benchmarks go through mpjsons.deserialize[T](json).
 */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
class DeserializationBench {

  @Param(Array("legacy", "current"))
  var impl: String = _

  var legacy: io.mpjsons.legacy.MPJsons = _
  var current: io.mpjsons.MPJsons = _
  var isLegacy: Boolean = _

  var userJson: String = _
  var ordersJson: String = _
  var beanJson: String = _
  var numbersJson: String = _
  var textsJson: String = _
  var drawingJson: String = _

  var userStatic: String => AnyRef = _
  var ordersStatic: String => AnyRef = _
  var ordersProjectionStatic: String => AnyRef = _
  var beanStatic: String => AnyRef = _
  var numbersStatic: String => AnyRef = _
  var textsStatic: String => AnyRef = _
  var drawingStatic: String => AnyRef = _

  @Setup
  def setup(): Unit = {
    // JSON is always produced by the current serializer, so both implementations read identical input
    val serializer = new io.mpjsons.MPJsons()
    serializer.markTypedClass[Shape]()
    userJson = serializer.serialize(Data.smallUser)
    ordersJson = serializer.serialize(Data.orders)
    beanJson = serializer.serialize(Data.simpleBean)
    numbersJson = serializer.serialize(Data.numbers)
    textsJson = serializer.serialize(Data.texts)
    drawingJson = serializer.serialize(Data.drawing)

    isLegacy = impl == "legacy"
    if (isLegacy) {
      legacy = new io.mpjsons.legacy.MPJsons()
      legacy.markTypedClass[Shape]()
      val skipping = new io.mpjsons.legacy.MPJsons(true)
      userStatic = staticOf(legacy.buildStaticDeserializer[User])
      ordersStatic = staticOf(legacy.buildStaticDeserializer[Orders])
      ordersProjectionStatic = staticOf(skipping.buildStaticDeserializer[OrdersProjection])
      beanStatic = staticOf(legacy.buildStaticDeserializer[SimpleBean])
      numbersStatic = staticOf(legacy.buildStaticDeserializer[Numbers])
      textsStatic = staticOf(legacy.buildStaticDeserializer[Texts])
      drawingStatic = staticOf(legacy.buildStaticDeserializer[Drawing])
    } else {
      current = new io.mpjsons.MPJsons()
      current.markTypedClass[Shape]()
      val skipping = new io.mpjsons.MPJsons(true)
      userStatic = staticOf(current.buildStaticDeserializer[User])
      ordersStatic = staticOf(current.buildStaticDeserializer[Orders])
      ordersProjectionStatic = staticOf(skipping.buildStaticDeserializer[OrdersProjection])
      beanStatic = staticOf(current.buildStaticDeserializer[SimpleBean])
      numbersStatic = staticOf(current.buildStaticDeserializer[Numbers])
      textsStatic = staticOf(current.buildStaticDeserializer[Texts])
      drawingStatic = staticOf(current.buildStaticDeserializer[Drawing])
    }
  }

  private def staticOf[T](d: io.mpjsons.legacy.StaticDeserializer[T]): String => AnyRef = json => d.deserialize(json).asInstanceOf[AnyRef]

  private def staticOf[T](d: io.mpjsons.StaticDeserializer[T]): String => AnyRef = json => d.deserialize(json).asInstanceOf[AnyRef]

  /** Typical call: TypeTag is materialized at the call site on every invocation. */
  @Benchmark
  def userApi(): AnyRef = if (isLegacy) legacy.deserialize[User](userJson) else current.deserialize[User](userJson)

  @Benchmark
  def userStaticDeserializer(): AnyRef = userStatic(userJson)

  @Benchmark
  def orders(): AnyRef = ordersStatic(ordersJson)

  @Benchmark
  def ordersProjectionSkippingFields(): AnyRef = ordersProjectionStatic(ordersJson)

  @Benchmark
  def bean(): AnyRef = beanStatic(beanJson)

  @Benchmark
  def numbers(): AnyRef = numbersStatic(numbersJson)

  @Benchmark
  def texts(): AnyRef = textsStatic(textsJson)

  @Benchmark
  def typedDrawing(): AnyRef = drawingStatic(drawingJson)
}
