package zio.amps.examples.tradeAggregator

import com.crankuptheamps.client.{Message, Client => AmpsClient}
import com.crankuptheamps.client.Client.Bookmarks
import zio._
import zio.amps.subscriber._
import zio.json._
import zio.stream._

import java.util.UUID

object TradeAggregator {
  private val subscriberBufferSize: Int = 1024 * 1024 * 2

  type AggregateListStorage = Map[AggregateKey, Committable[AggregateList]]

  val subscriptionOptions: SubscriptionOptions =
    SubscriptionOptions(
      topic = TradeAggregatorExampleMain.ampsTopic,
      subId = TradeAggregatorExampleMain.subscriptionId,
      bookmark = Some(Bookmarks.EPOCH) // We want to aggregate from bookmark 0
    )

  val app: ZIO[AmpsClient, Any, Unit] = Subscriber
    .subscribe(subscriberBufferSize)(
      subscriptionOptions
    )
    .mapAccumZIO[AmpsClient, Throwable, AggregateListStorage, Option[
      AggregateListStorage
    ]](
      Map.empty
    )((acc, msg) =>
      for {
        next <- ZIO
          .fromEither(msg.getData.fromJson[EventMessage])
          .mapError(new Exception(_))
        result =
          next match {
            case trade: Trade =>
              val key = AggregateKey(trade)
              val newList = acc
                .getOrElse(
                  key,
                  UncommittedAggregateList(data = AggregateList.default)
                ) <> UncommittedAggregateList(data = AggregateList(trade))
              (acc + (key -> newList), Option.empty[AggregateListStorage])
            case EmitAggregate =>
              (
                acc.flatMap { case (key, value) =>
                  value.commit.discardOption.map((key, _))
                }.toMap,
                Some(acc)
              )
            case cancel: CancelTrade =>
              val newAggMap = acc.flatMap { case (key, value) =>
                value.removeData(cancel).map((key, _))
              }
              (newAggMap, Option.empty[AggregateListStorage])
          }
        _ <- ZIO.attempt {
          msg.ack()
        }
      } yield result
    )
    .collectSome
    .mapConcatChunk(aggMap => Chunk(aggMap.toList.map(x => Aggregate(x))))
    .run {
      ZSink.foreach(agg => Console.printLine(agg.toJson))
    }
}
