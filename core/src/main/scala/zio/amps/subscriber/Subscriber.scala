package zio.amps.subscriber

import zio._
import zio.stream._
import com.crankuptheamps.client._

object Subscriber {
  def subscribe(buffer: Int)(
      topic: String,
      timeout: Int = 10000
  ): ZStream[Client, Throwable, Message] =
    for {
      client <- ZStream.service[Client]
      stream <- ZStream.async[Client, Throwable, Message](
        cb => {
          val eventHandler = new MessageHandler {
            def invoke(msg: Message) = {
              cb(ZIO.succeed(Chunk.succeed(msg)))
            }
          }
          client.subscribe(eventHandler, topic, timeout)
        },
        buffer
      )
    } yield stream
}
