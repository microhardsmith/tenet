# Network model

## Overview

The whole tenet networking part is quite simple, the main components are :

1. `Encoder`, Encoder determines how your POJO should be encoded into a WriteBuffer for transferring
2. `Decoder`, Decoder determines how to decode a POJO from the target ReadBuffer
3. `Handler`, Handler is where you put your specific business logic, to process the data received
4. `Provider`, Provider determines how a Sentry object could be created, and what to be done when the application exit

About concrete networking protocol, they are divided into two phases:

1. `Sentry` phase, where the read-write model are fully controlled by developers only, all the stuff like Encoder, Decoder, Handler are not functional yet, during sentry phase, the connection must have been well established, and ready for further exchange of data
2. `Protocol` phase, where Encoder, Decoder, Handler would take over the control, reading from channel and writing data to channel are well organized by tenet

## Threading model

Each TCP connection will be represented as an `Channel`, the `Channel` was first mounted on a `Poller` thread in `Sentry` phase, if it successfully upgraded to `Protocol` phase, it will be also mounted on `Writer` thread.
All the server socket were registered to a single `Net` thread, and the default `Poller` and `Writer` threads are the number of your CPU cores.
`Poller` thread will handle the reading part of the socket, `Writer` thread will handle the writing part of the socket, the access to the critical section will be managed through a locking mechanism.

## Example

A small `echo` example could be found in [echo example](https://github.com/microhardsmith/tenet/blob/master/common/src/test/java/cn/zorcc/common/network/EchoTest.java)
A simple `http` example could be found in [http example](https://github.com/microhardsmith/tenet/blob/master/common/src/test/java/cn/zorcc/common/network/HttpTest.java)

Note that in order to run tenet application, you need to specify some arguments in system properties:

```shell
--enable-preview
--enable-native-access=ALL-UNNAMED
-DTENET_LIBRARY_PATH=/path/to/lib
```

