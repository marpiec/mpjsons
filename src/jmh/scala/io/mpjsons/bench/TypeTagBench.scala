package io.mpjsons.bench

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

import scala.reflect.runtime.universe._

/**
 * Cost of a TypeTag materialised at the call (what `serialize[T]` / `deserialize[T]` callers do on every call) against
 * a tag created once. Materialising calls `runtimeMirror(classLoader)`, which takes Scala reflection's global lock;
 * run with `-t` > 1 to see contention.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
class TypeTagBench {

  private val createdOnce: TypeTag[User] = typeTag[User]

  @Benchmark
  def materialisedAtCall(): Type = typeTag[User].tpe

  @Benchmark
  def createdOnceTag(): Type = createdOnce.tpe
}
