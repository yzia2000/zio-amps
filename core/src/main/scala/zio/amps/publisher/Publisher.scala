package zio.amps.publisher

import com.crankuptheamps.client.{Client => AmpsClient}
import zio._

case class Publisher(client: AmpsClient, sem: Semaphore)

object Publisher {
  def publish(
      topic: String,
      data: String
  ): ZIO[Publisher, Throwable, Unit] = {
    for {
      publisher <- ZIO.service[Publisher]
      _ <- publisher.sem.withPermit(
        ZIO.attempt(publisher.client.publish(topic, data))
      )
    } yield ()
  }

  def live = ZLayer.scoped {
    for {
      client <- ZIO.service[AmpsClient]
      sem <- Semaphore.make(permits = 1)
    } yield Publisher(client, sem)
  }
}
