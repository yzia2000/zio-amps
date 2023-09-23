package zio.amps.subscriber

import zio._
import zio.stream._
import com.crankuptheamps.client._
import java.util.UUID

case class SubscriptionOptions(
    topic: String,
    subId: String = UUID.randomUUID().toString,
    filter: Option[String] = None,
    options: Option[String] = None,
    bookmark: Option[String] = None,
    timeout: Option[Long] = None
)

object Subscriber {
  def subscribe(buffer: Int)(
      options: SubscriptionOptions
  ): ZStream[Client, Throwable, Message] =
    for {
      client <- ZStream.service[Client]
      stream <- ZStream.asyncZIO[Client, Throwable, Message](
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

          val command = new Command(Message.Command.Subscribe)
            .setTopic(options.topic)
            .setSubId(options.subId)
          options.filter.foreach(command.setFilter)
          options.options.foreach(command.setOptions)
          options.timeout.foreach(command.setTimeout)
          options.bookmark.foreach(command.setBookmark)
          client.executeAsync(command, eventHandler)
          ZIO.logInfo(
            s"Created subscription with id ${options.subId} for topic ${options.topic}"
          )
        },
        buffer
      )
    } yield stream
}
