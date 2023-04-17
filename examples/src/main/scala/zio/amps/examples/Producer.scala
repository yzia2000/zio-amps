package zio.amps.examples

import com.crankuptheamps.client.{Client => AmpsClient}
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import zio._
import zio.amps.client._
import zio.amps.publisher._
import zio.stream._

import scala.util.Using

object Producer {
  def publishSampleTrade(id: String) = {
    val trade = Trade(id.toString(), 1000)
    val payload = trade.asJson.noSpaces

    ZIO.debug(payload) *>
      Publisher.publish("Trades", payload)
      *> ZIO.sleep(1.second)
  }

  def app = ZStream
    .fromIterable(1 to 10)
    .foreach(id => publishSampleTrade(id.toString))
}
