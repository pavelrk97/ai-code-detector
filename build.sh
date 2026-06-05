#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
rm -rf out
javac -d out \
    src/aidetector/*.java \
    src/aidetector/core/*.java \
    src/aidetector/signals/*.java \
    src/aidetector/input/*.java \
    src/aidetector/report/*.java
echo "build ok"
