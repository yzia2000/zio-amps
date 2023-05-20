package zio.amps.examples.tradeAggregator

import com.crankuptheamps.client.fields.Field
import com.crankuptheamps.client.{
  DefaultServerChooser,
  HAClient,
  LoggedBookmarkStore,
  MemoryBookmarkStore,
  Client => AmpsClient
}
import zio._
import zio.amps.client.{Client, ClientConfig, HAClientConfig}
import zio.amps.publisher.Publisher
import zio.amps.subscriber.Subscriber

import java.nio.file.{Files, Path, Paths}

object TradeAggregatorExampleMain extends ZIOAppDefault {
  val ampsTopic = "/zio/amps/examples/trades"
  val subscriptionId: String = "TradeAggregator-Example"

  val clientConfig: ULayer[HAClientConfig] =
    ZLayer.succeed(
      HAClientConfig(List("tcp://localhost:9007/amps/json"), "TradeClient")
    )

  val clientLive: RLayer[HAClientConfig, AmpsClient] = Client.haLive()

  def run: Task[Unit] = {
    (TradeProducer.app.logError.fork *> TradeAggregator.app.logError.fork *> ZIO.never)
      .provide(
        clientConfig,
        clientLive,
        Publisher.live
      )
  }
}
