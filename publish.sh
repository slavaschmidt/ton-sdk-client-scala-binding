#!/bin/bash -e

sbt publishSigned && sleep 60 && sbt sonatypeRelease