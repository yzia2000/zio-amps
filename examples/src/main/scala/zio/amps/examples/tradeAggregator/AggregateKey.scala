package zio.amps.examples.tradeAggregator

import zio.json._

final case class AggregateKey(symbol: String)

object AggregateKey {
  implicit val aggKeyJsonEncoder: JsonEncoder[AggregateKey] =
    DeriveJsonEncoder.gen[AggregateKey]
  implicit val aggKeyJsonDecoder: JsonDecoder[AggregateKey] =
    DeriveJsonDecoder.gen[AggregateKey]

  def apply(trade: Trade): AggregateKey = {
    trade match {
      case NewTrade(_, symbol, _, _, _)      => AggregateKey(symbol)
      case AmendTrade(_, _, symbol, _, _, _) => AggregateKey(symbol)
    }
  }
}
