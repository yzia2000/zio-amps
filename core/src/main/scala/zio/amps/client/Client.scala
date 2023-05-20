package zio.amps.client

import zio._
import zio.stream._
import com.crankuptheamps.client.{Client => AmpsClient}

object Client {
  def live(
      ampsClient: ClientConfig => AmpsClient = config =>
        new AmpsClient(config.name)
  ): ZLayer[ClientConfig, Throwable, AmpsClient] =
    liveZIO(config => ZIO.attempt(ampsClient(config)))

  def liveZIO(
      ampsClient: ClientConfig => Task[AmpsClient] = config =>
        ZIO.attempt(new AmpsClient(config.name))
  ): ZLayer[ClientConfig, Throwable, AmpsClient] =
    ZLayer.scoped {
      for {
        live <- ZIO.service[ClientConfig]
        resource <- ZIO.acquireRelease(
          for {
            client <- ampsClient(live)
            _ <- ZIO.attempt {
              client.connect(live.uri)
              client.logon()
              client
            }
          } yield client
        )(client =>
          ZIO.debug(s"Closing client ${live.name}") *> ZIO.succeed {
            client.unsubscribe()
            client.close()
          }
        )
      } yield resource
    }
}
