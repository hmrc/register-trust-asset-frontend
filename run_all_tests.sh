#!/usr/bin/env bash

sbt clean scalafmtAll scalastyleAll compile coverage Test/test coverageOff dependencyUpdates coverageReport
