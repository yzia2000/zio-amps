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

case class PublishPayload(
    data: String,
    correlationId: Option[String] = None,
    sowKey: Option[String] = None,
    expiration: Option[Int] = None
)

object Publisher {
  def publish(
      topic: String,
      payload: PublishPayload
  ): ZIO[Publisher, Throwable, Unit] = {
    for {
      publisher <- ZIO.service[Publisher]
      _ <- publisher.sem.withPermit(
        ZIO.attempt {
          val command = new Command(Message.Command.Publish)
          command.setData(payload.data)
          command.setTopic(topic)
          payload.correlationId.foreach(command.setCorrelationId)
          payload.sowKey.foreach(command.setSowKey)
          payload.expiration.foreach(command.setExpiration)
          // This is an async function. You need to install a FailedWriteHandler
          // and a potential PublishStore in the client to make sure things don't go south.
          publisher.client.executeAsync(command, null)
        }
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
