#!/usr/bin/env bash

set -eu

echo "Generate coverage and test report"
sbt coverage test
sbt coverageReport
echo "Scanning now..."
# SONAR_CLOUD_TOKEN should be added in circleci project setting
sonar-scanner -X -Dsonar.login="$SONAR_CLOUD_TOKEN"
