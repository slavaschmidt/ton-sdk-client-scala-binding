#!/bin/bash -eE

export JAVA_HOME=`/usr/libexec/java_home -v 1.8`


# sbt compile
# CP=/Users/slasch/.ivy2/cache/org.scala-lang/scala-library/jars/scala-library-2.13.3.jar
# cd target/scala-2.13/classes/
# javah -cp $CP:. tonsdkjni.Bridge




# -shared for linux

LIBS="$(pwd)/lib"

echo $LIBS

gcc \
  -dynamiclib \
  -shared \
  -std=c99 \
  -I$JAVA_HOME/include \
  -I$JAVA_HOME/include/darwin \
  -I$JAVA_HOME/include/linux \
  *.c \
  -o lib/libTonSdkJniBridge.dylib \
  -L$LIBS \
  -lton_client \
  -Wl,-rpath,.
