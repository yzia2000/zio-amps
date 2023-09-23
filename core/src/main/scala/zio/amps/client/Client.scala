package zio.amps.client

import com.crankuptheamps.client.{
  DefaultServerChooser,
  HAClient,
  Client => AmpsClient
}
import zio._
import zio.stream._

object Client {
  def simpleLive(
      ampsClient: SimpleClientConfig => AmpsClient = config =>
        new AmpsClient(config.name)
  ): RLayer[SimpleClientConfig, AmpsClient] =
    simpleLiveZIO(config => ZIO.attempt(ampsClient(config)))

  def simpleLiveZIO(
      ampsClient: SimpleClientConfig => Task[AmpsClient] = config =>
        ZIO.attempt(new AmpsClient(config.name))
  ): RLayer[SimpleClientConfig, AmpsClient] =
    ZLayer.scoped {
      for {
        live <- ZIO.service[SimpleClientConfig]
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

  def defaultHaClientFactory(config: HAClientConfig): HAClient = {
    val client = new HAClient(config.name)
    client.setPublishStore(config.publishLogStore)
    client.setBookmarkStore(config.subscribeLogStore)
    val chooser = config.serverChooser getOrElse {
      val chooser = new DefaultServerChooser()
      config.uris.foreach(chooser.add)
      chooser
    }
    client.setServerChooser(chooser)
    client
  }

  def haLive(
      ampsClient: HAClientConfig => HAClient = config =>
        defaultHaClientFactory(config)
  ): RLayer[HAClientConfig, HAClient] =
    haLiveZIO(config => ZIO.attempt(ampsClient(config)))
  def haLiveZIO(
      ampsClient: HAClientConfig => Task[HAClient] = config =>
        ZIO.attempt(defaultHaClientFactory(config))
  ): RLayer[HAClientConfig, HAClient] =
    ZLayer.scoped {
      for {
        live <- ZIO.service[HAClientConfig]
        resource <- ZIO.acquireRelease(
          for {
            client <- ampsClient(live)
            _ <- ZIO.attempt {
              client.connectAndLogon()
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
