package zio.amps.client

import com.crankuptheamps.client.{
  BlockPublishStore,
  BookmarkStore,
  DefaultServerChooser,
  MemoryBookmarkStore,
  MemoryPublishStore,
  PublishStore,
  ServerChooser,
  Store
}

sealed trait ClientConfig {
  def name: String
}

final case class HAClientConfig(
    uris: List[String],
    name: String,
    subscribeLogStore: BookmarkStore = new MemoryBookmarkStore(),
    publishLogStore: Store = new MemoryPublishStore(1024),
    serverChooser: Option[ServerChooser] = None
) extends ClientConfig

final case class SimpleClientConfig(uri: String, name: String)
