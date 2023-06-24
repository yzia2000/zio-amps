package zio.amps.examples.tradeAggregator

import zio.json._

sealed trait Aggregate {
  def id: String
}

case class NewAggregate(
    id: String,
    key: AggregateKey,
    price: BigDecimal,
    qty: BigDecimal
) extends Aggregate

case class AmendAggregate(
    id: String,
    previousId: String,
    key: AggregateKey,
    price: BigDecimal,
    qty: BigDecimal
) extends Aggregate

case class CancelAggregate(
    id: String,
    previousId: String
) extends Aggregate

object Aggregate {
  implicit val aggJsonEncoder: JsonEncoder[Aggregate] =
    DeriveJsonEncoder.gen[Aggregate]
  implicit val aggJsonDecoder: JsonDecoder[Aggregate] =
    DeriveJsonDecoder.gen[Aggregate]

  def apply(agg: (AggregateKey, Committable[AggregateList])): Aggregate = {
    agg match {
      case (key, value: CommittedAggregateList) =>
        value.data.trades match {
          case Nil =>
            CancelAggregate(id = value.id, previousId = value.previousId)
          case _ =>
            AmendAggregate(
              value.id,
              value.previousId,
              key,
              value.data.grossPrice / value.data.qty,
              value.data.qty
            )
        }
      case (key, value: UncommittedAggregateList) =>
        NewAggregate(
          value.id,
          key,
          value.data.grossPrice / value.data.qty,
          value.data.qty
        )
    }
  }
}
