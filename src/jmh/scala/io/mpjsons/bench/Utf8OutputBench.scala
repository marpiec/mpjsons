package io.mpjsons.bench

import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

import io.mpjsons.{MPJsons, StaticSerializer}
import org.openjdk.jmh.annotations._

/**
 * The UTF-8 writer (serializeToBytes) against the StringBuilder path, for each fixture:
 *  - `xString`: serialize, a String,
 *  - `xStringBytes`: serialize(x).getBytes(UTF_8), what callers needing bytes did before,
 *  - `xUtf8`: serializeToBytes.
 * bench-results/utf8-output.txt also has `xUtf8String`, a String decoded from the writer's buffer: an experiment
 * for serialize that was slower on number-heavy fixtures and was removed (performance.md §4d).
 */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
class Utf8OutputBench {

  private val mpjsons = {
    val m = new MPJsons()
    m.markTypedClass[Shape]()
    m
  }

  private val user: StaticSerializer[User] = mpjsons.buildStaticSerializer[User]
  private val orders: StaticSerializer[Orders] = mpjsons.buildStaticSerializer[Orders]
  private val bean: StaticSerializer[SimpleBean] = mpjsons.buildStaticSerializer[SimpleBean]
  private val numbers: StaticSerializer[Numbers] = mpjsons.buildStaticSerializer[Numbers]
  private val texts: StaticSerializer[Texts] = mpjsons.buildStaticSerializer[Texts]
  private val drawing: StaticSerializer[Drawing] = mpjsons.buildStaticSerializer[Drawing]

  @Benchmark def userString(): String = user.serialize(Data.smallUser)
  @Benchmark def userStringBytes(): Array[Byte] = user.serialize(Data.smallUser).getBytes(StandardCharsets.UTF_8)
  @Benchmark def userUtf8(): Array[Byte] = user.serializeToBytes(Data.smallUser)

  @Benchmark def ordersString(): String = orders.serialize(Data.orders)
  @Benchmark def ordersStringBytes(): Array[Byte] = orders.serialize(Data.orders).getBytes(StandardCharsets.UTF_8)
  @Benchmark def ordersUtf8(): Array[Byte] = orders.serializeToBytes(Data.orders)

  @Benchmark def beanString(): String = bean.serialize(Data.simpleBean)
  @Benchmark def beanStringBytes(): Array[Byte] = bean.serialize(Data.simpleBean).getBytes(StandardCharsets.UTF_8)
  @Benchmark def beanUtf8(): Array[Byte] = bean.serializeToBytes(Data.simpleBean)

  @Benchmark def numbersString(): String = numbers.serialize(Data.numbers)
  @Benchmark def numbersStringBytes(): Array[Byte] = numbers.serialize(Data.numbers).getBytes(StandardCharsets.UTF_8)
  @Benchmark def numbersUtf8(): Array[Byte] = numbers.serializeToBytes(Data.numbers)

  @Benchmark def textsString(): String = texts.serialize(Data.texts)
  @Benchmark def textsStringBytes(): Array[Byte] = texts.serialize(Data.texts).getBytes(StandardCharsets.UTF_8)
  @Benchmark def textsUtf8(): Array[Byte] = texts.serializeToBytes(Data.texts)

  @Benchmark def drawingString(): String = drawing.serialize(Data.drawing)
  @Benchmark def drawingStringBytes(): Array[Byte] = drawing.serialize(Data.drawing).getBytes(StandardCharsets.UTF_8)
  @Benchmark def drawingUtf8(): Array[Byte] = drawing.serializeToBytes(Data.drawing)
}
