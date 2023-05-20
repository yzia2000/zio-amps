package zio.amps.examples.tradeAggregator

final case class AggregateList(
    trades: List[Trade]
) {
  def grossPrice: BigDecimal =
    trades.foldLeft(0: BigDecimal)((acc, trade) =>
      acc + trade.price * trade.qty
    )

  def qty: BigDecimal =
    trades.foldLeft(0: BigDecimal)((acc, trade) => acc + trade.qty)

  def <>(next: AggregateList): AggregateList = {
    AggregateList(
      Trade.flatten(trades ++ next.trades)
    )
  }

  def filterCancel(cancel: CancelTrade): Option[AggregateList] = {
    Option(
      AggregateList(trades = trades.filter(_.parentId == cancel.parentId))
    ).filter(_.trades.nonEmpty)
  }
}

object AggregateList {
  def apply(trade: Trade): AggregateList = AggregateList(List(trade))
  def default: AggregateList = AggregateList(List())
}
