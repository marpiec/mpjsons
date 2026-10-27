package io.mpjsons.elsewhere

import io.mpjsons.{OpenShape, typeName}

/** A subclass of a non-sealed trait outside the trait's package: its alias cannot be resolved (see TypeNameSpec). */
@typeName("far") case class FarOpen(a: Int) extends OpenShape
