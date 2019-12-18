#!/usr/bin/env bash

set -eu

echo "Release to Github package repository"
git config --global user.email "mkeshav@gmail.com"
git config --global user.name "Keshav Murthy"
sbt 'release with-defaults skip-tests'