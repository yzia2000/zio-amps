package zio.amps.client

import zio._
import zio.stream._
import com.crankuptheamps.client.{Client => AmpsClient}

object Client {
  def live(
      ampsClient: ClientConfig => AmpsClient = config =>
        new AmpsClient(config.name)
  ): ZLayer[ClientConfig, Throwable, AmpsClient] =
    ZLayer.scoped {
      for {
        live <- ZIO.service[ClientConfig]
        resource <- ZIO.acquireRelease(
          ZIO.attempt {
            val client = ampsClient(live)
            client.connect(live.uri)
            client.logon()
            client
          }
        )(client =>
          ZIO.debug(s"Closing client ${live.name}") *> ZIO.succeed {
            client.unsubscribe()
            client.close()
          }
        )
      } yield resource
    }
}
