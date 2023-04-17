package zio.amps.examples

import zio._
import zio.amps.client.Client
import zio.amps.publisher.Publisher
import zio.amps.subscriber.Subscriber
import zio.amps.client.ClientConfig

object Main extends ZIOAppDefault {
  val clientConfig: ULayer[ClientConfig] =
    ZLayer.succeed(
      ClientConfig("tcp://localhost:9007/amps/json", "TradeClient")
    )

  def run: ZIO[Environment & (ZIOAppArgs & Scope), Any, Any] = {
    (Producer.app.fork *> Consumer.app.fork *> ZIO.never).provide(
      clientConfig,
      Client.live,
      Publisher.live
    )
  }
}
