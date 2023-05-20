package zio.amps.examples.tradeAggregator

import java.util.UUID

sealed trait Committed[X] extends Committable[X] {
  def previousId: String

  // Sometimes we just need to discard a committed aggregate. In this domain, an committed aggregate
  // gets discard when all of its internal trades get cancelled
  def discardOption: Option[Committed[X]]
}

sealed trait Committable[X] {
  def data: X
  def commit: Committable[X] with Committed[X]

  def <>(r: Committable[X]): Committable[X]

  def removeData(cancel: CancelTrade): Option[Committable[X]]
}

final case class UncommittedAggregateList(
    id: String = UUID.randomUUID.toString,
    data: AggregateList
) extends Committable[AggregateList] {
  def commit: CommittedAggregateList =
    CommittedAggregateList(previousId = id, data = data)

  def <>(r: Committable[AggregateList]): UncommittedAggregateList =
    UncommittedAggregateList(id, data <> r.data)

  def removeData(cancel: CancelTrade): Option[Committable[AggregateList]] =
    this.data.filterCancel(cancel).map(data => this.copy(data = data))
}

final case class CommittedAggregateList(
    id: String = UUID.randomUUID().toString,
    previousId: String,
    data: AggregateList
) extends Committed[AggregateList] {
  def commit: CommittedAggregateList =
    CommittedAggregateList(previousId = id, data = data)

  def discardOption: Option[CommittedAggregateList] =
    Option.unless(data.trades.isEmpty)(this)

  def <>(r: Committable[AggregateList]): CommittedAggregateList =
    CommittedAggregateList(previousId = previousId, data = data <> r.data)

  def removeData(cancel: CancelTrade): Option[Committable[AggregateList]] =
    Some(
      this.copy(data =
        this.data.filterCancel(cancel).getOrElse(AggregateList.default)
      )
    )
}
