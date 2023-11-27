# Network model

## Overview

The whole tenet networking part is quite simple, the main components are :

1. Encoder, Encoder determines how your POJO should be encoded into a WriteBuffer for transferring
2. Decoder, Decoder determines how to decode a POJO from the target ReadBuffer
3. Handler, Handler is where you put your specific business logic, to process the data received
4. Provider, Provider determines how a Sentry object could be created, and what to be done when the application exit

About concrete networking protocol, they are divided into two phases:

1. Sentry phase, where the read-write model are fully controlled by developers only, all the stuff like Encoder, Decoder, Handler are not functional yet, during sentry phase, the connection must have been well established, and ready for further exchange of data
2. Protocol phase, where Encoder, Decoder, Handler would take over the control, reading from channel and writing data to channel are well organized by tenet

## Threading model

 TODO
