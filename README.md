# ZIO AMPS

_The use of AMPS Clients in a production environment requires an AMPS Server license._

_WIP_

60east has a fantastic message processing solution, [AMPS](https://www.crankuptheamps.com/amps/),
rich with not just the usual publisher-subscriber functionalities but also features that resemble a real time database
in the form of real-time materialized views with State of World (SOW),
stateless performant content filtering, aggregations amongst many others.

This library is a ZIO wrapper over the 60east Java client, that lifts frequently used
operations and commands into functional effects that allows us to make efficient utilization
of cpu cores while addressing concurrency limitations in a lock-less manner.

## Why AMPS?

One look at the AMPS feature set will likely convince a Kafka user that there are many desirable
features like content filtering, adhoc merging wildcarding topics, aggregations while at the
same not having to worry about deploying a ksqldb or kafka streams applications for
the smallest of use cases.

You can imagine the dynamic capabilities of AMPS can really come in handy to reduce the number
of touches you need to make the configuration or infrastructure, so the focus lies only
on the business logic.

## Why ZIO?

Leaving aside that there will be an fs2 variant of this library coming soon,
ZIO was chosen because it comes with very useful primitives and a single composable container
that is ideal for those new into effect systems.

Effect systems allow us build more utility around concurrency and asynchronous activities
like fetch messages in parallel, transforming them in parallel and updating state stores
in parallel, but because we are investing in immutability and functional composition, we
do not suffer from the same race conditions, lossy updates and non-determinism that
non-functional programming often does.

## Documentation

WIP - Take a look at the examples module for now
