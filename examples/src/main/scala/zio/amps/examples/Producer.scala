package zio.amps.examples

import zio._
import zio.stream._
import zio.amps.client._
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import scala.util.Using
import com.crankuptheamps.client.{Client => AmpsClient}

object Producer extends ZIOAppDefault {
  val clientConfig =
    ClientConfig("tcp://localhost:9007/amps/json", "TradePublisher-Producer")

  val clientLayer = ZLayer.succeed(clientConfig) >>> Client.live

  def delay = ZIO.sleep(_)

  def publishSampleTrade(id: String, client: AmpsClient) = {
    ZIO.attempt {
      val trade = Trade(id.toString(), 1000)
      val payload = trade.asJson.noSpaces
      println(payload)
      client.publish("Trades", payload)
    } *> ZIO.sleep(100.millis)
  }

  def run = ZStream
    .fromIterable(1 to 1000)
    .foreach(id =>
      for {
        client <- ZIO.service[AmpsClient]
        _ <- publishSampleTrade(id.toString, client)
      } yield ()
    )
    .provide(clientLayer)
}
