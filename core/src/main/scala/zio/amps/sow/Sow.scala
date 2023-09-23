package zio.amps.sow

import zio._
import zio.stream._
import com.crankuptheamps.client.{Client => AmpsClient}
import com.crankuptheamps.client.Message
import com.crankuptheamps.client.MessageHandler
import com.crankuptheamps.client.Command

case class SowOptions(
    topic: String,
    sowKeys: Option[String] = None,
    filter: Option[String] = None,
    batchSize: Option[Int] = None
)

object Sow {
  def sowStream(options: SowOptions) = for {
    client <- ZStream.service[AmpsClient]
    stream <- ZStream.async[Any, Throwable, Message](
      { cb =>
        val handler = new MessageHandler {
          override def invoke(msg: Message) = {
            msg.getCommand match {
              case Message.Command.SOW      => cb(ZIO.succeed(Chunk(msg)))
              case Message.Command.GroupEnd => cb.end
            }
          }
        }

        val command = new Command(Message.Command.SOW)

        command.setTopic(options.topic)
        options.filter.foreach(command.setFilter)
        options.sowKeys.foreach(command.setSowKeys)
        options.batchSize.foreach(command.setBatchSize)

        client.executeAsync(command, handler)
      },
      10
    )
  } yield stream

  def sow[V](options: SowOptions) =
    sowStream(options).runCollect
}
