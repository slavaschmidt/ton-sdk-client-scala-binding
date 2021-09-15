#!/bin/bash -e

# wget http://binaries.tonlabs.io/tonclient_1_win32_lib.gz
# wget http://binaries.tonlabs.io/tonclient_1_win32_dll.gz
# wget http://binaries.tonlabs.io/tonclient_1_darwin.gz
# wget http://binaries.tonlabs.io/tonclient_1_darwin_arm64.gz
# wget http://binaries.tonlabs.io/tonclient_1_linux.gz
# 
# gunzip tonclient_1_win32_lib.gz
# gunzip tonclient_1_win32_dll.gz
# gunzip tonclient_1_darwin.gz
# gunzip tonclient_1_darwin_arm64.gz
# gunzip tonclient_1_linux.gz
# 
# mv tonclient_1_darwin src/main/resources/libton_client_x86_64.dylib
# mv tonclient_1_darwin_arm64 src/main/resources/libton_client_aarch64.dylib
# mv tonclient_1_linux src/main/resources/libton_client.so
# mv tonclient_1_win32_dll src/main/resources/ton_client.dll
# mv tonclient_1_win32_lib src/main/resources/ton_client.lib
# 
# exit 0

cd ~/work/workspaces/ton-sdk/TON-SDK
git checkout master
git pull
cargo update
cargo build --release
cd -
mv ~/work/workspaces/ton-sdk/TON-SDK/target/release/libton_client.dylib src/main/resources/libton_client_aarch64.dylib
