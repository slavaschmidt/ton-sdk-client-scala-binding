#!/bin/bash -eE

export JAVA_HOME='C:\work\jdk1.8.0_271\'

export LIBS="$(pwd)/lib"

echo "$LIBS"
echo "$JAVA_HOME"

ls "$JAVA_HOME/include/win32"

cd src/main/c

gcc \
  -shared \
  -fPIC \
  -I"$JAVA_HOME/include/win32" \
  -I"$JAVA_HOME/include" \
  -L"$LIBS" \
  *.c \
  -o "$LIBS/libTonSdkClientJniBinding.so" \
  -lton_client

  # -dynamiclib \
cd -



