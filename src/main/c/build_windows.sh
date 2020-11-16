@ECHO OFF

# expects JAVA_HOME to be properly set on system level and point to JDK
# for example JAVA_HOME="c:\work\jdk1.8.0_271"

## don't forget to add long long definition to jni_md.h as in following:

# # ifdef __GNUC__
# typedef long long jlong;
# #else
# typedef _int64 jlong;
# #endif

cl *.c -I"!JAVA_HOME!\include\win32" -I"!JAVA_HOME!\include" -MD -LD -link "..\..\..\lin\ton_client.dll.lib"

