#!/bin/bash -eE

export JAVA_HOME=`/usr/libexec/java_home -v 1.8`
export LIBS="../resources"
gcc \
  -dynamiclib \
  -shared \
  -I$JAVA_HOME/include \
  -I$JAVA_HOME/include/darwin \
  -I$JAVA_HOME/include/linux \
  *.c \
  -o $LIBS/libTonSdkClientJniBinding_aarch64.dylib \
  -L$LIBS \
  -lton_client_aarch64 \
  -Wl,-rpath,.

cd $LIBS

install_name_tool -change "libton_client_aarch64.dylib" @loader_path/libton_client_aarch64.dylib "libTonSdkClientJniBinding_aarch64.dylib"

cd -
