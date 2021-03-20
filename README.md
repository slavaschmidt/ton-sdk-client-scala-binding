# ton-sdk-client-scala-binding

<img src="https://i.ibb.co/rH7LV8C/Scalaton.png" width="100px" border="0">

TON SDK Client library Scala bindings.

[![Build Status](https://circleci.com/gh/slavaschmidt/ton-sdk-client-scala-binding.svg?style=shield&branch=main)](https://circleci.com/gh/slavaschmidt/ton-sdk-client-scala-binding)
[![codecov](https://codecov.io/gh/slavaschmidt/ton-sdk-client-scala-binding/branch/main/graph/badge.svg?token=MRUA0KJ2BK)](https://codecov.io/gh/slavaschmidt/ton-sdk-client-scala-binding)
[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Maven Central](https://img.shields.io/maven-central/v/com.dancingcode/freeton-sdk-client-scala-binding_2.12)](https://repo.maven.apache.org/maven2/com/dancingcode/freeton-sdk-client-scala-binding_2.12/1.0.0-M2/)


The TON SDK Client library provides bindings for [Freeton SDK client](https://github.com/tonlabs/TON-SDK).
It consists of three major parts:
- JNI bindings for the rust library. Because of the way JVM interoperates with native code it is not possible to call client functions directly.
Instead, a thin C wrapper is implemented. The wrapper translates Java calls into appropriate native calls predefined by the client library.
- Java JNI wrapper. It is a thin layer responsible for the interoperation with the native library. Having this layer written in plain Java should allow to create bindings for other languages by reusing exising implementation.
- Scala wrapper provides idiomatic type safe way to call SDK functions.

## Compatibility

The current version is compatible with JDK 1.8+ and Scala 2.12/2.13

We use CI on Linux Focal Fossa and MacOs X hosts (Intel and M1) to run our tests.

Following systems confirmed to be compatible:
- Arch Linux (manjaro 5.6.16)
- Ubuntu Linux (bionic 18-04)
- Windows 10 x64
- MacOs X (Catalina 10.15)
- macOs (Big Sur 11.2)
- OpenJDK 8
- OpenJDK 11
- OpenJDK 16
- OracleJDK 1.8.0


## Prerequisites

As for any Scala application, JRE is required. This project uses SBT as a convenient build tool. Scala installation is also required.
On Linux and MacOSx, all dependencies can easily be installed by [one-line installer](https://www.scala-lang.org/2020/06/29/one-click-install.html).
For Windows the installation is less convenient and requires manual installation of the JRE and SBT/Scala. 
We recommend to use [OpenJDK](https://adoptopenjdk.net/) to install JRE. 
The [SBT](https://www.scala-sbt.org/download.html) can be used to install other needed components.

To build native libraries the host system should possess working gcc installation for linux/windows and VisualStudio C++ for windows.
It is also mandatory to have JDK (as opposed to the JRE) installed. 


## Installation

Check the prerequisites. Clone the repository. Navigate to the project folder `ton-sdk-client-scala-binding` and start SBT by typing `sbt` in the console.


## Running tests and examples

The library provides a comprehensive set of tests that can be used as a reference to library usage. 
There is also an ongoing effort to provide a 
[standalone set of examples](https://github.com/slavaschmidt/freeton-sdk-client-scala-examples).

Because of the way native loader works some care needs to be taken in Linux and Windows environments.
We're working on improving user experience and making it as seamless as in MacOS X. 

The library JAR contains all needed native binaries and a custom loader. At the moment native library accessed by the JNI subsystem,
the native loader will reuse existing or create appropriate native libs located in the folder defined by the java property `"java.io.freetontmpdir"`. 
If this property is undefined, the `lib` folder in the current project will be created (if needed) and used.

The sbt scripts contain appropriate environment overrides for the default case of placing the native libraries in the "lib" subfolder
but sometimes an additional user actions might be required.

An additional user action is required in Linux and Windows to extend the path the OS uses to locate libraries. This is done by adding the path to the lib folder to it. 
For this the environment variable `LD_LIBRARY_PATH` (linux) and `PATH` (windows) must be extended with the location of the folder 
referenced by `"java.io.freetontmpdir"` (or if it is undefined to the `lib` folder in the project). 
Alternatively, the `"java.io.freetontmpdir"` can be set to point to some folder already included in system path, 
and the libraries well be copied by the loader as needed (given appropriate access rights).

An example for Linux system: 
```shell script
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:"$(pwd/lib)"
```
.
In windows don't forget to restart you command line session after changing `PATH`.

To run tests: navigae to the project folder and type `sbt test`

To create a jar-packaged artefact: `sbt package`. The artefact with the library will be located in `target/scala-2.12` folder of the project.

Building native libraries is different in linux, mac and windows, involves manual relinking on MacOs and should not be required as we provide them pre-build.
Curious users can consult example build scripts for JNI headers and native libraries in `src/main/c` folder.

The windows build script contains suggestions for fixing JDK header files to get them compile with VisualStudio. 
Small adjustments to the `ton_client.h` might also be necessary.

 
## Working with the client

Using client library involves few mandatory steps:


### Load native libraries. 

We provide a helper class `ton.sdk.client.jni.NativeLoader` to make it as easy as possible. Simple call `NativeLoader.apply()` should suffice. Please make sure not to load the library more than once as this will lead to errors.


### Instantiating client

The ton client needs a configuration to be instantiated. Few common configurations that rely on default settings are [provided](https://github.com/slavaschmidt/ton-sdk-client-scala-binding/blob/c2b76dac6ac3e1a28557ea3c1f84df12b7a9074c/src/main/scala/ton/sdk/client/binding/model.scala#L39).
The users of the library can easy create their own configurations by overriding the default one. Currently, the config of the ton client looks like the following:
```
{
    "network": {
        "server_address": "http://localhost",
        "network_retries_count": 5,
        "max_reconnect_timeout": 120,
        "message_retries_count": 5,
        "message_processing_timeout": 40000,
        "wait_for_timeout": 40000,
        "out_of_sync_threshold": 15000,
        "access_key": ""
    },
    "crypto": {
        "mnemonic_dictionary": 1,
        "mnemonic_word_count": 12,
        "hdkey_derivation_path": 'm/44"/396"/0"/0/0',
        "hdkey_compliant": true
    },
    "abi": {
        "workchain": 0,
        "message_expiration_timeout": 40000,
        "message_expiration_timeout_grow_factor": 1.5
    }
}
```


### Creating context

The ton client has a concept of context. The context encapsulates configuration and state data. 
The context can be created by calling the `get` method of the client:
```scala
implicit val ctx = Context.create(ClientConfig.LOCAL).get
``` 
and should be closed as soon as it is not needed anymore by calling 
```scala
ctx.close()
```

The binding library will try its best to auto-close forgotten contexts but because of the unpredictable nature
of the JVMs garbage collection this is not always possible to do timely.
Because of this the library provides managed context that is auto-closed at the moment last operation 
withing the context finishes execution. It can be used like in the following example:
```scala
import ton.sdk.client.binding.Context
import Context._

val result = local { implicit ctx =>
  call(Request.ApiReference)
}
```
The `local` refers to the server configuration. Client calls inside of the curly braces have the corresponding context available to them.
Contexts can be nested where this makes sense by giving the implicits same name. 
In the case of nesting the internal context has higher order of precedence.


### Calling client functions

The approach to calling library functions we use called "trampolining" and in essence it means representing functions as class instances (a quite common approach in scala).
This allows for type-safe calls where types of the both parameter and result are well-defined at compilation time. For example:
```scala
import ton.sdk.client.modules.Utils._

val result = local { implicit ctx =>
  call(Request.ConvertAddress(accountId, AddressStringFormat.hex))
}
assertValue(result)(Address(hexWorkchain))
```

Here we're calling ton client's `convert_address` function by using `ConvertAddress` case class and providing the `accountId` and required format and getting an instance of `Address` class back.
The first line, `import ton.sdk.client.modules.Utils._` demonstrates how different request types reside in modules to reflect 
the naming schema of the ton client. 

For detailed description of the modules and available functions please consult [tests](src/test/scala/ton/sdk/client/modules) 
and documentation for the [ton client](https://github.com/tonlabs/TON-SDK/blob/master/docs).


### Choosing execution style

The ton client provides two modes of execution, sync and async. We decided to abstract this concept into separate "Effect". 
This approach makes it possible to write the application code once and then change the type of effect later without the 
need to touch the code itself.
Of course, this approach works only for functions that support both sync and async calls.

For example, following code executes asynchronously wrapped in a `Future`:
```scala
NativeLoader.apply()

implicit val ef = futureEffect

def run() = devNet { implicit ctx =>
    for {
      version <- call(Client.Request.Version)
      seed    <- call(Crypto.Request.MnemonicFromRandom())
      keys    <- call(Crypto.Request.MnemonicDeriveSignKeys(seed.phrase))
    } yield (version, seed, keys)
  }
```

One can made it run synchronously wrapped in a `Try` by merely changing line

```scala
implicit val ef = futureEffect
```

to

```scala
implicit val ef = tryEffect
```

Most of the tests written in such a way that they don't specify the type of effect and run both 
[synchronously](https://github.com/slavaschmidt/ton-sdk-client-scala-binding/blob/c2b76dac6ac3e1a28557ea3c1f84df12b7a9074c/src/test/scala/ton/sdk/client/modules/utilsSpec.scala#L14) 
and 
[asynchronously](https://github.com/slavaschmidt/ton-sdk-client-scala-binding/blob/c2b76dac6ac3e1a28557ea3c1f84df12b7a9074c/src/test/scala/ton/sdk/client/modules/utilsSpec.scala#L18) 
.


#### Closing over async context

There is a caveat working with asynchronous calls in managed contexts: if one closes over the context and returns async 
computation that runs outside of the context, the context will be closed earlier than operation can complete, for example:
```scala
 implicit val ef = futureEffect
 
 def run() = devNet { implicit ctx =>
     for {
       version <- call(Client.Request.Version)
       seed    <- call(Crypto.Request.MnemonicFromRandom())
     } yield Future(call(Crypto.Request.MnemonicDeriveSignKeys(seed.phrase)))
   }
```

This problem is general to managed resources used in asynchronous manner and not specific to the implementation of the library.


### Error handling

Abstracting over the effect type gives us another advantage - it makes possible to represent failure cases in typical scala way, algebraically.
The result of the `call` function returns either `Success` or `Failure` of the chosen effect type. 
Thus, the logic is better represented in the form of the 
[for-comprehension](https://github.com/slavaschmidt/ton-sdk-client-scala-binding/blob/c2b76dac6ac3e1a28557ea3c1f84df12b7a9074c/src/test/scala/ton/sdk/client/modules/processingSpec.scala#L32).   


### Streaming

Ton client allows calling certain functions "with messages" generating continuous stream of events. 
We represent this in the form of "streaming" `callS` that should be called with appropriate parameters. 
There is a compile-time safety guarantee that the streaming function can't be called with non-streaming parameters and vice-versa.
The result of streaming call is a tuple that extends "normal" result with two 
[blocking iterators](src/main/scala/ton/sdk/client/binding/blockingIterator.scala) 
to communicate arrival of messages and errors to the user. 
Following shows an example of using such streaming call: 

```scala
val result = devNet { implicit ctx =>
  for {
    // Prepare data for deployment message
    keys <- call(Crypto.Request.GenerateRandomSignKeys)
    signer  = Signer.fromKeypair(keys)
    callSet = CallSet("constructor", Option(Map("pubkey" -> keys.public.asJson)), None)
    // Encode deployment message
    encoded <- call(Abi.Request.EncodeMessage(abi, None, Option(deploySet), Option(callSet), signer))
    _       <- sendGrams(encoded.address)
    // Deploy account
    params = MessageEncodeParams(abi, signer, None, Option(deploySet), Option(callSet))
    (data, messages, _) <- callS(Processing.Request.ProcessMessageWithEvents(params))
    // Check that messages are indeed received
    _ = assert(messages.collect(1.minute).nonEmpty)
  } yield data
}
```

Please note how we wait for at most one minute for all messages to arrive. 
Alternatively, one could wait just for, say, one second and retry until there are no messages left.
Please consult the ScalaDoc of the [blocking iterators](src/main/scala/ton/sdk/client/binding/blockingIterator.scala) for further details.
  

## License
```
This software is licensed under the Apache 2 license, quoted below.

Copyright 2020 Slava Schmidt and contributors

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```
