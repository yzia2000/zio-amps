package zio.amps.examples.tradeAggregator

import zio._
import zio.amps.client.Client
import zio.amps.publisher.Publisher
import zio.amps.subscriber.Subscriber
import zio.amps.client.ClientConfig

object TradeAggregatorExampleMain extends ZIOAppDefault {
  val ampsTopic = "/zio/amps/examples/trades"

  val clientConfig: ULayer[ClientConfig] =
    ZLayer.succeed(
      ClientConfig("tcp://localhost:9007/amps/json", "TradeClient")
    )

  def run: ZIO[Environment & (ZIOAppArgs & Scope), Any, Any] = {
    (Producer.app.logError.fork *> TradeAggregator.app.logError.fork *> ZIO.never)
      .provide(
        clientConfig,
        Client.live(),
        Publisher.live
      )
  }
}
