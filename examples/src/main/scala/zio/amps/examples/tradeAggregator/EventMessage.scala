package zio.amps.examples.tradeAggregator

import zio.json._

import java.time.LocalDateTime

sealed trait EventMessage

case object EmitAggregate extends EventMessage

sealed trait Trade extends EventMessage {
  def parentId: String
  def price: BigDecimal
  def qty: BigDecimal

  def transactionTime: LocalDateTime
}

@jsonHint("new-trade") final case class NewTrade(
    id: String,
    symbol: String,
    price: BigDecimal,
    qty: BigDecimal,
    transactionTime: LocalDateTime = LocalDateTime.now
) extends Trade {
  def parentId: String = id
}

@jsonHint("amend-trade") final case class AmendTrade(
    id: String,
    parentId: String,
    symbol: String,
    price: BigDecimal,
    qty: BigDecimal,
    transactionTime: LocalDateTime = LocalDateTime.now
) extends Trade

@jsonHint("cancel-trade") final case class CancelTrade(
    id: String,
    parentId: String,
    transactionTime: LocalDateTime = LocalDateTime.now
) extends EventMessage

object Trade {
  def flatten(trades: List[Trade]): List[Trade] = {
    trades
      .groupBy {
        _.parentId
      }
      .values
      .flatMap(arr => arr.sorted.reverse.headOption)
      .toList
  }
}

object EventMessage {
  implicit val encoder: JsonEncoder[EventMessage] =
    DeriveJsonEncoder.gen[EventMessage]
  implicit val decoder: JsonDecoder[EventMessage] =
    DeriveJsonDecoder.gen[EventMessage]

  implicit val tradeEncoder: JsonEncoder[Trade] =
    DeriveJsonEncoder.gen[Trade]
  implicit val tradeDecoder: JsonDecoder[Trade] =
    DeriveJsonDecoder.gen[Trade]

  implicit val ord: Ordering[Trade] = Ordering.by[Trade, LocalDateTime] {
    trade =>
      trade.transactionTime
  }
}
