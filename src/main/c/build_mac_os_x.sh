#!/bin/bash -eE

export JAVA_HOME=`/usr/libexec/java_home -v 1.8`

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

cd $LIBS

install_name_tool -change "libton_client.dylib" @loader_path/libton_client.dylib "libTonSdkClientJniBinding.dylib"

cd -
