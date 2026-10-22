#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
if ! command -v java >/dev/null 2>&1; then
  echo "Java 17 or newer is required" >&2
  exit 1
fi
if [ -f updater.jar ]; then java -cp updater.jar veil.DesktopUpdater "$(pwd)"; fi
exec java -jar MaleficVeil.jar
