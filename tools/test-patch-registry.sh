#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT INT TERM

MAIN_SOURCES=$(find "$ROOT/src/main/java/dev/reny/optimization/patch" -name '*.java' -print)
TEST_SOURCE="$ROOT/src/test/java/dev/reny/optimization/patch/PatchRegistrySelfTest.java"

javac -source 8 -target 8 -d "$OUT" $MAIN_SOURCES "$TEST_SOURCE"
java -cp "$OUT" dev.reny.optimization.patch.PatchRegistrySelfTest
