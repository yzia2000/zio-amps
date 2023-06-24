package zio.amps.publisher

import com.crankuptheamps.client.{
  Command,
  FailedWriteHandlerV4,
  Message,
  MessageHandler,
  Client => AmpsClient
}
import zio._

case class Publisher(client: AmpsClient, sem: Semaphore)

case class PublishFailedException(msg: String = "Failed to publish message")
    extends Exception(msg)

case class PublishPayload(
    data: String,
    correlationId: Option[String] = None,
    sowKey: Option[String] = None,
    expiration: Option[Int] = None,
    ackType: Int = Message.AckType.Completed,
    timeoutMs: Int = 60000
)

object Publisher {
  def publish(
      topic: String,
      payload: PublishPayload
  ): ZIO[Publisher, PublishFailedException, Unit] = {
    for {
      publisher <- ZIO.service[Publisher]
      _ <- publisher.sem
        .withPermit(
          ZIO
            .async[Any, PublishFailedException, Unit] { cb =>
              val handler = new MessageHandler {
                def invoke(msg: Message) = {
                  msg.getCommand() match {
                    case Message.Command.Ack =>
                      cb(ZIO.unit)
                    case _ =>
                  }
                }
              }
              val command = new Command(Message.Command.Publish)
              command.setData(payload.data)
              command.setAckType(payload.ackType)
              command.setTopic(topic)
              payload.correlationId.foreach(command.setCorrelationId)
              payload.sowKey.foreach(command.setSowKey)
              payload.expiration.foreach(command.setExpiration)
              // This is an async function. You need to install a FailedWriteHandler
              // and a potential PublishStore in the client to make sure things don't go south.
              publisher.client.executeAsync(command, handler)
            }
            .timeoutFail(
              new PublishFailedException(
                s"Timeout: Failed to publish message ${payload.data}"
              )
            )(payload.timeoutMs.millis)
        )
    } yield ()
  }

  def live: URLayer[AmpsClient, Publisher] = ZLayer.scoped {
    for {
      client <- ZIO.service[AmpsClient]
      sem <- Semaphore.make(permits = 1)
    } yield Publisher(client, sem)
  }
}
