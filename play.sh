#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p out
javac --release 17 -d out src/main/java/veil/*.java
java -cp out veil.WorldSelfTest
java -cp out veil.LanSmokeTest
echo "Tests passed. Launching The Malefic Veil..."
java -cp out veil.MaleficVeil
