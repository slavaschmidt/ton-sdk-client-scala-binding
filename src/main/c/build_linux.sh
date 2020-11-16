#!/bin/bash -eE

export JAVA_HOME=`dirname $(dirname $(readlink -f $(which javac)))`

gcc \
  -shared \
  -fPIC \
  -I"$JAVA_HOME/include/linux" \
  -I"$JAVA_HOME/include" \
  -L"$LIBS" \
  *.c \
  -o "libTonSdkClientJniBinding.so" \
  -lton_client




