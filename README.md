# ton-sdk-client-scala-binding

<img src="https://i.ibb.co/rH7LV8C/Scalaton.png" width="100px" border="0">

TON SDK Client library Scala bindings.

[![Build Status](https://travis-ci.com/slavaschmidt/ton-sdk-client-scala-binding.svg?branch=main&env=BADGE=osx)](https://travis-ci.com/slavaschmidt/ton-sdk-client-scala-binding)
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

The current version is compatible with Freeton client v1.0.0, JDK 1.8+ and Scala 2.12.

We use CI on Linux Focal Fossa and MacOs X hosts to run our tests.

Following systems confirmed to be compatible:
- Arch Linux (manjaro 5.6.16)
- Ubuntu Linux (bionic 18-04)
- Windows 10 x64 (US-EN)
- Windows 7 x64 (DE)
- MacOs X (Catalina 10.15)
- OpenJDK 8
- OpenJDK 11
- OracleJDK 1.8.0

We're currently working on adding Windows x86, Scala 2.13 cross-compilation and Freeton client v1.1.0 support.


## Prerequisites

As for any Scala application, JRE is required. This project uses SBT as a convenient build tool. Scala installation is also required.
On Linux and MacOSx, SBT, Scala and JDK can easily be installed by [one-line installer](https://www.scala-lang.org/2020/06/29/one-click-install.html).
For Windows the installation is less convenient and requires manual installation of the JRE and SBT/Scala. 
We recommend to use [OpenJDK](https://adoptopenjdk.net/) to install JRE. The [SBT](https://www.scala-sbt.org/download.html) can be used to install other needed dependencies.

To build native libraries the host system should possess working gcc installation for linux/windows and VisualStudio C++ for windows.
It is also mandatory to have JDK (as opposed to the JRE) installed. 



## Installation

Check the prerequisites. Clone the repository. Navigate to the project folder and start SBT by typing `sbt` in the console.

## Running tests and examples

The library provides a comprehensive set of tests that can be used as a reference to using library. 
There is also an ongoing effort to provide a [standalone set of examples](https://github.com/slavaschmidt/freeton-sdk-client-scala-examples)

Because of the way native loader looks for the libraries some care needs to be taken in Linux and Windows environments.
We're working on improving user experience and making it as seamless as in MacOS X. 

The library JAR contains all needed native code and a special loader. At the moment native library is loaded by the JNI subsystem,
the native loader will reuse existing or create appropriate native libs located in the folder defined by the java property `"java.io.freetontmpdir"`. 
If this property is undefined, the `lib` folder in the current project will be created (if needed) and used.

An additional user action is required in Linux and Windows to extend the path used to locate libraries with the path to this folder. 
For this the environment variable `LD_LIBRARY_PATH` (linux) and `PATH` (windows) must be extended with the location of the folder 
referenced by `"java.io.freetontmpdir"` or if it is undefined to the `lib` folder in the project. 
Alternatively, the `"java.io.freetontmpdir"` can be set to point to some folder already included in path and the libraries can be put there manually.

An example for Linux system: `export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:"$(pwd/lib)"; sbt test`.
In windows don't forget to restart you command line session after the change.

To run tests: `sbt test`

To create a library jar-packaged artefact: `sbt package`. The artefact is located in target/scala-2.12 .

Building native libraries is different in linux, mac and windows, involves manual relinking on MacOs and should not be required as we provide them pre-build.
Curious users can consult example build scripts for JNI headers and native libraries in `src/main/c` folder.

The windows build script contains suggestions for fixing JDK header files to get them compile with VisualStudio. 
Small adjustments to the `ton_client.h` might also be necessary.

 
## Working with the client

Using client library involves few mandatory steps:

### Load native libraries. 

To easy this task we provide a helper class `ton.sdk.client.jni.NativeLoader`. Simple call `NativeLoader.apply()` should suffice. Please make sure not to load the library more than once as this will lead to errors.

### Instantiating client

The ton client needs a configuration to be instantiated. Few common configurations using default settings are [provided](https://github.com/slavaschmidt/ton-sdk-client-scala-binding/blob/c2b76dac6ac3e1a28557ea3c1f84df12b7a9074c/src/main/scala/ton/sdk/client/binding/model.scala#L39)
The users of the library can easy create their own configurations by overriding the default one. The default config of the freeton client looks like the following:
```
{
    'network': {
        'server_address': 'http://localhost',
        'network_retries_count': 5,
        'message_retries_count': 5,
        'message_processing_timeout': 40000,
        'wait_for_timeout': 40000,
        'out_of_sync_threshold': 15000,
        'access_key': ''
    },
    'crypto': {
        'mnemonic_dictionary': 1,
        'mnemonic_word_count': 12,
        'hdkey_derivation_path': "m/44'/396'/0'/0/0",
        'hdkey_compliant': True
    },
    'abi': {
        'workchain': 0,
        'message_expiration_timeout': 40000,
        'message_expiration_timeout_grow_factor': 1.5
    }
}
```

### Creating context

The ton client has a concept of context in which actions are performed. The context is created by calling the get method of the client:
`implicit val ctx = Context.create(ClientConfig.LOCAL).get`  and should be closed after it is not needed anymore: `ctx.close()`.
The binding library will try its best to auto-close the context if it is not done manually but because of the unpredictable nature
of the JVMs garbage collection this is not always possible to do in timely manner.

Because of this the library provides managed context that will be auto-closed in the moment it is not needed. The use pattern is like following:
```
import ton.sdk.client.binding.Context
import ton.sdk.client.binding.Context._

val result = local { implicit ctx =>
  call(Request.ApiReference)
}
```
The `local` refers to the server configuration and client calls inside of the curly braces have the context available to them.

### Calling client functions

The approach to calling library functions we use called "Trampolining" and its main point in representing functions as class instances, quite common in Scala.
This allows for type-safe calls where the parameter type and the result type are well-defined at compilation time, for example:
```
import ton.sdk.client.modules.Utils._

val result = local { implicit ctx =>
  call(Request.ConvertAddress(accountId, AddressStringFormat.hex))
}
assertValue(result)(Address(hexWorkchain))
```

Here we're calling `ConvertAddress` function providing the accountId and required format and getting an instance of `Address` class back.
The first line, `import ton.sdk.client.modules.Utils._` demonstrates how different request types grouped in modules reflecting 
the approach of the ton client. 

For detailed description of the modules and available functions please consult tests and documentation for the [ton client](https://github.com/tonlabs/TON-SDK/blob/master/docs).

### Choosing execution style

The ton client provides two modes of execution, sync and async. We decided to abstract this concept into separate "Effect". 
Using this approach it is possible to write the application code once and then change the type of effect later without the need to touch the code itself.
Of course, this approach works only for functions that support both sync and async calls.

For example, following code executes asynchronously wrapped in `Future`:
```
NativeLoader.apply()

implicit val executionContext: ExecutionContext       = ExecutionContext.Implicits.global
implicit val ef:               Context.Effect[Future] = futureEffect

def run() = devNet { implicit ctx =>
    for {
      version <- call(Client.Request.Version)
      seed    <- call(Crypto.Request.MnemonicFromRandom())
      keys    <- call(Crypto.Request.MnemonicDeriveSignKeys(seed.phrase))
    } yield (version, seed, keys)
  }
```
One can made it run synchronously wrapped in `Try` by changing lines
```
implicit val executionContext: ExecutionContext       = ExecutionContext.Implicits.global
implicit val ef:               Context.Effect[Future] = futureEffect
```
to
```
implicit override val ef: Context.Effect[Try] = tryEffect
```

Most of the tests for the library are written in such a way that they don't rely on the effect type and run both 
[sync](https://github.com/slavaschmidt/ton-sdk-client-scala-binding/blob/c2b76dac6ac3e1a28557ea3c1f84df12b7a9074c/src/test/scala/ton/sdk/client/modules/utilsSpec.scala#L14) 
and 
[async](https://github.com/slavaschmidt/ton-sdk-client-scala-binding/blob/c2b76dac6ac3e1a28557ea3c1f84df12b7a9074c/src/test/scala/ton/sdk/client/modules/utilsSpec.scala#L18) ways:

#### Closing over async context
There is a caveat working with asyncronous calls in managed contexts: if one closes over the context and returns async 
computation that runs outside of the context, the context will be closed earlier than operation can complete, for example:
```
 implicit val ef:               Context.Effect[Future] = futureEffect
 
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
Thus, the logic is better represented in the form of [for-comprehension])(https://github.com/slavaschmidt/ton-sdk-client-scala-binding/blob/c2b76dac6ac3e1a28557ea3c1f84df12b7a9074c/src/test/scala/ton/sdk/client/modules/processingSpec.scala#L32).   

### Streaming

Ton client allows calling certain functions "with messages" generating continous stream of messages to the handler. 
We represent this in the form of "streaming" `callS` with appropriate parameters. 
There is a compile-time safety the streaming function aren't called with non-streaming parameters and vice-versa.
The result of streaming call is a tuple that extends "normal" result with two [blocking iterators](https://github.com/slavaschmidt/ton-sdk-client-scala-binding/blob/c2b76dac6ac3e1a28557ea3c1f84df12b7a9074c/src/main/scala/ton/sdk/client/binding/blockingIterator.scala#L10) 
to communicate arrival of messages and errors. The streaming call can be used for example as following:
```
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
Please note how we wait for at most one minute for all messages to arrive. Alternatively, one could wait just for a second and then retry until there are no messages.
Please consult the ScalaDoc of the BlockingIterator for further details.

## Further examples

Please take a look at the test specifications for [individual modules](src/test/scala/ton/sdk/client/modules) to get more understanding of how all pieces fit together.

There is also an ongoing effort to provide [standalone examples](https://github.com/slavaschmidt/freeton-sdk-client-scala-examples)

  

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
