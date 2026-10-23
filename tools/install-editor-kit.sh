#!/usr/bin/env bash
set -euo pipefail
# Pinned, independently published Java 17 editor module from Beckenburg.
# This kit is a separate binary dependency, not copied editor source.
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT="$ROOT/build/editor-kit/LevelEditorKit-Java17.jar"
URL='https://github.com/mrcalzon02/The-Beckenburg-Project/releases/download/beckenburg-v0.1.31/LevelEditorKit-Java17.jar'
HASH='5b75ada82ad71d128150b58f0ab74e0f0e18866968b216968c66940209e1252d'
mkdir -p "$(dirname "$OUTPUT")"
if [ ! -f "$OUTPUT" ] || ! printf '%s  %s\n' "$HASH" "$OUTPUT" | sha256sum -c - --status; then
  curl --fail --location --retry 3 --silent --show-error "$URL" --output "$OUTPUT"
fi
printf '%s  %s\n' "$HASH" "$OUTPUT" | sha256sum -c -
mvn -B -ntp org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file \
  "-Dfile=$OUTPUT" -DgroupId=org.beckenburg -DartifactId=level-editor-kit \
  -Dversion=0.1.31 -Dpackaging=jar -DgeneratePom=true
echo 'Pinned standalone LevelEditorKit-Java17.jar verified and installed to Maven repository'
