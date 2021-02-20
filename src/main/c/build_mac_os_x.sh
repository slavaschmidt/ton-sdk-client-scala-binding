#!/bin/bash -eE

export JAVA_HOME=`/usr/libexec/java_home -v 1.8`
export LIBS="../resources"


ARCH=$(echo 'System.getProperty("os.arch")' | scala | grep arch | cut -f 2 -d'=' | sed 's/[[:space:]]//g')
export SUFFIX=$ARCH # "x86_64" # "aarch64"

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
install_name_tool -change "@loader_path/libton_client.dylib" "@loader_path/libton_client_$SUFFIX.dylib" "libTonSdkClientJniBinding_$SUFFIX.dylib"

cd -
