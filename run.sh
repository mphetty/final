#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAVAFX_SDK="${JAVAFX_SDK:-$HOME/Downloads/javafx-sdk-21.0.2}"
JAVAFX_LIB="${JAVAFX_LIB:-$JAVAFX_SDK/lib}"
BUILD_DIR="$ROOT_DIR/.build"

if [[ ! -f "$JAVAFX_LIB/javafx.controls.jar" ]]; then
    echo "JavaFX SDK not found at: $JAVAFX_LIB" >&2
    echo "Set JAVAFX_LIB to the SDK's lib directory and run again." >&2
    exit 1
fi

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

javac \
    --module-path "$JAVAFX_LIB" \
    --add-modules javafx.controls \
    -d "$BUILD_DIR" \
    "$ROOT_DIR/Game.java" \
    "$ROOT_DIR/Gui.java" \
    "$ROOT_DIR/Move.java" \
    "$ROOT_DIR/Player.java" \
    "$ROOT_DIR/Setup.java" \
    "$ROOT_DIR/Stratego.java"

exec java \
    --module-path "$JAVAFX_LIB" \
    --add-modules javafx.controls \
    -cp "$BUILD_DIR" \
    Stratego
