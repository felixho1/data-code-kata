#!/usr/bin/env bash
set -e

env JAVA_OPTS="-Xss2048k" sbt test clean docker
