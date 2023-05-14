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
          // We are using ZStream async on AMPs async messsaging interface
          // on the client since this interface allows us to add backpressure.
          val eventHandler = new MessageHandler {
            def invoke(msg: Message): Unit = {
              // AMPS client library mututates the same object.
              // This can cause reference issues and async queue message
              // overriding.
              cb(ZIO.succeed(Chunk(msg.copy())))
            }
          }
          client.subscribe(eventHandler, topic, timeout)
        },
        buffer
      )
    } yield stream
}
