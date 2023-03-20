package zio.amps.publisher

import com.crankuptheamps.client.{Client => AmpsClient}
import zio._

object Publisher {
  def publish(topic: String, data: String): ZIO[AmpsClient, Throwable, Unit] = {
    for {
      client <- ZIO.service[AmpsClient]
      _ <- ZIO.attempt(client.publish(topic, data))
    } yield ()
  }
}
