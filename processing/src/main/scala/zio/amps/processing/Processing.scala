package zio.amps.processing

import zio._
import zio.stream._
import com.crankuptheamps.client.Message

object Processing {
  def defaultAcker(
      msg: Message
  ): ZIO[Message, Throwable, Unit] =
    ZIO.attempt(msg.ack()).logError

  def defaultNacker(msg: Message): Task[Unit] =
    ZIO.attempt(msg.ack("cancel")).logError
}
