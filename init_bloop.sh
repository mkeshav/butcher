#!/usr/bin/env bash

set -eu

bloop server &
sbt bloopInstall

echo "Waiting for bloop to start...."
sleep 5

echo "Compiling...."
bloop compile app
bloop compile app-test
echo "Ready.... now you can run bloop test app"