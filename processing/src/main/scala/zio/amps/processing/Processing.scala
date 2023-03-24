package zio.amps.processing

import zio._
import zio.stream._

object Processing {
  def foreachZIOPar[Env, In, Out](maxPar: Int) =
    ZPipeline.mapZIOPar[Env, Throwable, In, Out](maxPar)
}
