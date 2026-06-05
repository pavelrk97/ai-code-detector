#!/usr/bin/env bash
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
if [ ! -d "$here/out" ]; then
    echo "not built yet, run ./build.sh first" >&2
    exit 1
fi
exec java -cp "$here/out" aidetector.Main "$@"
