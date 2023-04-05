package zio.amps.processing

import zio._
import zio.stream._
import com.crankuptheamps.client.Message

object Processing {
  type AckNackHandler =
    ZIO[Any, Throwable, Unit] => ZIO[Message, Throwable, Unit]

  implicit def ackNackHandler(
      f: ZIO[Any, Throwable, Unit]
  ): ZIO[Message, Throwable, Unit] =
    ZIO
      .serviceWithZIO[Message](msg =>
        f.logError
          .foldZIO(
            err => ZIO.attempt(msg.ack("cancel")),
            _ => ZIO.attempt(msg.ack())
          )
          .logError
          .ignore
      )

  def foreachZIO[Env](
      maxPar: Int
  )(f: Message => Task[Unit])(implicit ackNackHandler: AckNackHandler) =
    ZPipeline.mapZIO[Env, Any, Message, Unit](msg =>
      ackNackHandler(f(msg)).provide(ZLayer.succeed(msg))
    )

  def foreachZIOPar[Env](
      maxPar: Int
  )(f: Message => Task[Unit])(implicit ackNackHandler: AckNackHandler) =
    ZPipeline.mapZIOPar[Env, Any, Message, Unit](maxPar) { msg =>
      ackNackHandler(f(msg)).provide(ZLayer.succeed(msg))
    }
}
