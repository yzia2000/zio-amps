package zio.amps.examples

import com.crankuptheamps.client.Command
import com.crankuptheamps.client.Message
import com.crankuptheamps.client.MessageHandler
import com.crankuptheamps.client.MessageStream
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import zio._
import zio.amps._
import zio.amps.client._
import zio.amps.subscriber._
import zio.stream._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Try
import scala.util.Using

import collection.JavaConverters._

object Processor {
  def dbCall = ZIO.sleep(10.seconds)

  def process(msg: Message) = {
    decode[Trade](msg.getData()) match {
      case Right(trade) => {
        ZIO.debug(s"Began processing ${trade.id}") *> dbCall *> Console
          .printLine(trade) *> ZIO.debug(s"Completed processing ${trade.id}")
      }
      case Left(err) => Console.printError(err)
    }
  }
}

object Consumer extends ZIOAppDefault {
  val subscriberBufferSize = 1000
  val maxConcurrentEffects = 1000
  val ampsTopic = "Trades"
  val clientConfig =
    ClientConfig("tcp://localhost:9007/amps/json", "TradePublisher-Consumer")

  val app = Subscriber
    .subscribe(subscriberBufferSize)(ampsTopic)
    .mapZIOPar(maxConcurrentEffects)(Processor.process)
    .runDrain

  val clientLayer = ZLayer.succeed(clientConfig) >>> Client.live

  def run = {
    app.provide(clientLayer)
  }
}
