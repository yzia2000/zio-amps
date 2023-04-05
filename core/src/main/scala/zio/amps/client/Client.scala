package zio.amps.client

import zio._
import zio.stream._
import com.crankuptheamps.client.{Client => AmpsClient}

object Client {
  val live: ZLayer[ClientConfig, Throwable, AmpsClient] =
    ZLayer.scoped {
      for {
        config <- ZIO.service[ClientConfig]
        resource <- ZIO.acquireRelease(
          ZIO.attempt {
            val client = new AmpsClient(config.name)
            client.connect(config.uri)
            client.logon()
            client.setAutoAck(false)
            client
          }
        )(client =>
          ZIO.debug(s"Closing client ${config.name}") *> ZIO.succeed(
            client.close()
          )
        )
      } yield resource
    }
}
