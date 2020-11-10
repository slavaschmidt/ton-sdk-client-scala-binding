#!/bin/bash -eE

export JAVA_HOME=`dirname $(dirname $(readlink -f $(which javac)))`

export LIBS="$(pwd)/lib"

echo "$LIBS"
echo "$JAVA_HOME"

ls "$JAVA_HOME/include/linux"

cd src/main/c

gcc \
  -shared \
  -fPIC \
  -I"$JAVA_HOME/include/linux" \
  -I"$JAVA_HOME/include" \
  -L"$LIBS" \
  *.c \
  -o "$LIBS/libTonSdkClientJniBinding.so" \
  -lton_client

  # -dynamiclib \
cd -



