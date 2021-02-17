#!/bin/bash -eE

export JAVA_HOME=`/usr/libexec/java_home -v 1.8`
export LIBS="../resources"
export SUFFIX="x86_64"
# "aarch64"

gcc \
  -dynamiclib \
  -shared \
  -I$JAVA_HOME/include \
  -I$JAVA_HOME/include/darwin \
  -I$JAVA_HOME/include/linux \
  *.c \
  -o "$LIBS/libTonSdkClientJniBinding_$SUFFIX.dylib" \
  -L$LIBS \
  -l"ton_client_$SUFFIX" \
  -Wl,-rpath,.

cd $LIBS

install_name_tool -change "libton_client_$SUFFIX.dylib" "@loader_path/libton_client_$SUFFIX.dylib" "libTonSdkClientJniBinding_$SUFFIX.dylib"

cd -
