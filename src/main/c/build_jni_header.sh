#!/bin/bash -eE

cd ../../..

sbt compile

CP=~/.ivy2/cache/org.scala-lang/scala-library/jars/scala-library-2.12.12.jar

javah -cp $CP:target/scala-2.12/classes/ton.sdk.client.jni.Binding

mv ton_sdk_client_jni_Binding.h src/main/c/

cd -
