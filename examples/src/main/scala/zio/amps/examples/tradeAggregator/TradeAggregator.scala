package zio.amps.examples.tradeAggregator

import com.crankuptheamps.client.{Message, Client => AmpsClient}
import com.crankuptheamps.client.Client.Bookmarks
import zio._
import zio.amps.subscriber._
import zio.json._
import zio.stream._

import java.util.UUID
import zio.amps.publisher._

object TradeAggregator {
  private val subscriberBufferSize: Int = 1024 * 1024 * 2

  final val aggregateSowTopic = "/zio/amps/examples/tradeAggregator/aggregates"

  type AggregateListStorage = Map[AggregateKey, Committable[AggregateList]]

  val subscriptionOptions: SubscriptionOptions =
    SubscriptionOptions(
      topic = TradeAggregatorExampleMain.ampsTopic,
      subId = TradeAggregatorExampleMain.subscriptionId,
      bookmark = Some(Bookmarks.EPOCH) // We want to aggregate from bookmark 0
    )

  val app: ZIO[AmpsClient & Publisher, Any, Unit] = Subscriber
    .subscribe(subscriberBufferSize)(
      subscriptionOptions
    )
    .mapAccumZIO[
      AmpsClient & Publisher,
      Throwable,
      AggregateListStorage,
      Option[
        AggregateListStorage
      ]
    ](
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
    .mapConcatChunk(aggMap =>
      Chunk.fromIterable(aggMap.toList.map(Aggregate.apply))
    )
    .run {
      ZSink.foreach(agg =>
        Publisher.publish(
          aggregateSowTopic,
          PublishPayload(data = agg.toJson, sowKey = Some(agg.id))
        )
      )
    }
}
