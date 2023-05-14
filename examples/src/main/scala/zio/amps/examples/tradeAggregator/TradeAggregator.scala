package zio.amps.examples.tradeAggregator

import com.crankuptheamps.client.{Client => AmpsClient}
import zio._
import zio.amps.subscriber._
import zio.json._
import zio.stream._

import java.util.UUID

object TradeAggregator {
  val subscriberBufferSize = 1024 * 1024 * 2

  case class AggregateKey(symbol: String)

  object AggregateKey {
    def apply(trade: Trade): AggregateKey = {
      trade match {
        case NewTrade(_, symbol, _, _)      => AggregateKey(symbol)
        case AmendTrade(_, _, symbol, _, _) => AggregateKey(symbol)
      }
    }
  }

  case class AggregateList(
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

  sealed trait Committed {
    def previousId: String
  }

  sealed trait Committable[X] {
    def data: X
    def commit: Committable[X] with Committed

    def <>(r: Committable[X]): Committable[X]

    def removeData(cancel: CancelTrade): Option[Committable[X]]
  }

  case class UncommittedAggregateList(
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

  case class CommittedAggregateList(
      id: String = UUID.randomUUID().toString,
      previousId: String,
      data: AggregateList
  ) extends Committable[AggregateList]
      with Committed {
    def commit: CommittedAggregateList =
      CommittedAggregateList(previousId = id, data = data)

    def <>(r: Committable[AggregateList]): CommittedAggregateList =
      CommittedAggregateList(previousId = previousId, data = data <> r.data)

    def removeData(cancel: CancelTrade): Option[Committable[AggregateList]] =
      Some(
        this.copy(data =
          this.data.filterCancel(cancel).getOrElse(AggregateList.default)
        )
      )
  }

  sealed trait Aggregate
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

  object AggregateList {
    def apply(trade: Trade): AggregateList = AggregateList(List(trade))
    def default: AggregateList = AggregateList(List())
  }

  val app: ZIO[AmpsClient, Any, Unit] = Subscriber
    .subscribe(subscriberBufferSize)(TradeAggregatorExampleMain.ampsTopic)
    .mapZIO(msg => ZIO.fromEither(msg.getData.fromJson[EventMessage]))
    .mapAccum[Map[AggregateKey, Committable[AggregateList]], Option[
      Map[AggregateKey, Committable[AggregateList]]
    ]](
      Map.empty
    )((acc, next) =>
      next match {
        case trade: Trade =>
          val key = AggregateKey(trade)
          val newList = acc
            .getOrElse(
              key,
              UncommittedAggregateList(data = AggregateList.default)
            ) <> UncommittedAggregateList(data = AggregateList(trade))
          (acc + (key -> newList), None)
        case EmitAggregate =>
          (
            acc.map { case (key, value) => (key, value.commit) },
            Some(acc)
          )
        case cancel: CancelTrade =>
          val newAggMap = acc.flatMap { case (key, value) =>
            value.removeData(cancel).map((key, _))
          }
          (newAggMap, None)
      }
    )
    .collectSome
    .mapConcatChunk(aggMap => Chunk(aggMap.toList.map(x => Aggregate(x))))
    .run {
      ZSink.foreach { x =>
        Console.printLine(x)
      }
    }
}
