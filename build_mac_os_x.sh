#!/bin/bash -eE

export JAVA_HOME=`/usr/libexec/java_home -v 1.8`

export LIBS="$(pwd)/lib"

#sbt compile
#CP=/Users/slasch/.ivy2/cache/org.scala-lang/scala-library/jars/scala-library-2.13.3.jar
#javah -cp $CP:target/scala-2.13/classes/ ton.sdk.client.jni.Binding
#mv ton_sdk_client_jni_Binding.h src/main/c/

cd src/main/c

gcc \
  -dynamiclib \
  -shared \
  -I$JAVA_HOME/include \
  -I$JAVA_HOME/include/darwin \
  -I$JAVA_HOME/include/linux \
  *.c \
  -o $LIBS/libTonSdkClientJniBinding.dylib \
  -L$LIBS \
  -lton_client \
  -Wl,-rpath,.

cd -


#gcc \
#  -dynamiclib \
#  -shared \
#  -I$JAVA_HOME/include \
#  -I$JAVA_HOME/include/darwin \
#  -I$JAVA_HOME/include/linux \
#  -o $LIBS/libTonSdkClientJniBinding.dylib \
#  -L$LIBS \
#  -lton_client
#  -Wl,-all_load "$LIBS/libton_client.a" \
#  *.c \
