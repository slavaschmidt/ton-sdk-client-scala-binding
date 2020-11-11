#!/bin/bash -eE

# export JAVA_HOME='C:\work\jdk1.8.0_271\'

# export LIBS="$(pwd)/lib"

cd src/main/c

# cl.exe main.cpp /EHsc /link example_dll.lib

cl *.c -I"c:\work\jdk1.8.0_271\include\win32" -I"c:\work\jdk1.8.0_271\include" -MD -LD -link "e:\TON-SDK\target\release\ton_client.dll.lib"  # dir-F "libTonSdkClientJniBinding.dll"

## don't forget to add long long to jni_md.h
## ifdef __GNUC__
#typedef long long jlong;
##else
#typedef _int64 jlong;
##endif
