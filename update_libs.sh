#!/bin/bash -e

wget http://sdkbinaries-ws.tonlabs.io/tonclient_1_win32_lib.gz
wget http://sdkbinaries-ws.tonlabs.io/tonclient_1_win32_dll.gz
wget http://sdkbinaries-ws.tonlabs.io/tonclient_1_darwin.gz
wget http://sdkbinaries-ws.tonlabs.io/tonclient_1_linux.gz

gunzip tonclient_1_win32_lib.gz
gunzip tonclient_1_win32_dll.gz
gunzip tonclient_1_darwin.gz
gunzip tonclient_1_linux.gz

mv tonclient_1_darwin src/main/resources/libton_client.dylib
mv tonclient_1_linux src/main/resources/libton_client.so
mv tonclient_1_win32_dll src/main/resources/ton_client.dll
mv tonclient_1_win32_lib src/main/resources/ton_client.lib
