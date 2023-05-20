package zio.amps.examples.tradeAggregator

import zio._
import zio.amps.publisher._
import zio.json._
import zio.stream._

object TradeProducer {
  def publishNewTrade(id: String): ZIO[Publisher, Throwable, Unit] = {
    val trade: EventMessage = NewTrade(id, "AAPL", 1000, 1000)
    ZIO.debug(trade.toJson) *> Publisher.publish(
      TradeAggregatorExampleMain.ampsTopic,
      PublishPayload(trade.toJson, sowKey = Some(id))
    )
  }

  def publishAmendTrade(id: String): ZIO[Publisher, Throwable, Unit] = {
    val trade: EventMessage = AmendTrade(id, id, "AAPL", 500, 1000)
    ZIO.debug(trade.toJson) *> Publisher.publish(
      TradeAggregatorExampleMain.ampsTopic,
      PublishPayload(trade.toJson, sowKey = Some(id))
    )
  }

  def publishCancelTrade(id: String): ZIO[Publisher, Throwable, Unit] = {
    val trade: EventMessage = CancelTrade(id, id)
    ZIO.debug(trade.toJson) *> Publisher.publish(
      TradeAggregatorExampleMain.ampsTopic,
      PublishPayload(trade.toJson, sowKey = Some(id))
    )
  }

  def triggerAggregateEmission: ZIO[Publisher, Throwable, Unit] = {
    ZIO.debug(EmitAggregate.toString) *> Publisher.publish(
      TradeAggregatorExampleMain.ampsTopic,
      PublishPayload((EmitAggregate: EventMessage).toJson)
    )
  }

  val testTradeIds = (1 to 10)

  def app: ZIO[Publisher, Throwable, Unit] = ZStream
    .fromIterable(testTradeIds)
    .foreach(id =>
      publishNewTrade(id.toString)
    ) *> triggerAggregateEmission *> ZStream
    .fromIterable(testTradeIds)
    .foreach(id =>
      publishAmendTrade(id.toString)
    ) *> triggerAggregateEmission *> ZStream
    .fromIterable(testTradeIds)
    .foreach(id => publishCancelTrade(id.toString)) *> triggerAggregateEmission
}
