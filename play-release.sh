#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p logs
if ! command -v java >/dev/null 2>&1; then
  echo "Java 17 or newer is required" >&2
  exit 1
fi
mode=--ask
case "${1:-}" in
  --check|--install) mode="$1" ;;
esac
if [ -f updater.jar ]; then
  temporary=$(mktemp)
  trap 'rm -f "$temporary"' EXIT HUP INT TERM
  cp updater.jar "$temporary"
  if ! java -cp "$temporary" veil.DesktopUpdater "$(pwd)" "$mode" >> logs/updater-console.log 2>&1; then
    echo "Updater recovery failed; see logs/updater.log" >&2
    exit 2
  fi
  rm -f "$temporary"
  trap - EXIT HUP INT TERM
fi
if [ "${1:-}" = "--check" ]; then exit 0; fi
echo "Launching The Malefic Veil (logs/game-console.log)"
exec java -jar MaleficVeil.jar >> logs/game-console.log 2>&1
