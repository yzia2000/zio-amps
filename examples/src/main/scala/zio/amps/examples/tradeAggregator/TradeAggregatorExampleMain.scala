package zio.amps.examples.tradeAggregator

import com.crankuptheamps.client.fields.Field
import com.crankuptheamps.client.{
  LoggedBookmarkStore,
  MemoryBookmarkStore,
  Client as AmpsClient
}
import zio.*
import zio.amps.client.{Client, ClientConfig}
import zio.amps.publisher.Publisher
import zio.amps.subscriber.Subscriber

import java.nio.file.{Files, Path, Paths}

object TradeAggregatorExampleMain extends ZIOAppDefault {
  val ampsTopic = "/zio/amps/examples/trades"
  val subscriptionId: String = "TradeAggregator-Example"

  val clientConfig: ULayer[ClientConfig] =
    ZLayer.succeed(
      ClientConfig("tcp://localhost:9007/amps/json", "TradeClient")
    )

  val clientLive = Client.liveZIO(config => {
    val bookmarkDir = "/tmp/zio-amps-examples-trade-aggregator"
    val bookmarkFile = "bookmark.log"
    ZIO.attempt {
      Files.createDirectories(Paths.get(bookmarkDir))
    } *> ZIO.attempt {
      val client = new AmpsClient(config.name)
      client.setAutoAck(true)
      val bookmarkStore = new LoggedBookmarkStore(s"$bookmarkDir/$bookmarkFile")
      bookmarkStore.prune()
      client.setBookmarkStore(bookmarkStore)
      client
    }
  })

  def run: Task[Unit] = {
    (Producer.app.logError.fork *> TradeAggregator.app.logError.fork *> ZIO.never)
      .provide(
        clientConfig,
        clientLive,
        Publisher.live
      )
  }
}
