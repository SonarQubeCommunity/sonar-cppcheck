#! /bin/bash

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(readlink -f "$SCRIPT_DIR/../../..")"
TARGET_DIR="$PROJECT_DIR/target"

function check_project_dir_and_create_target_dir {
  if [ ! -f "$PROJECT_DIR/pom.xml" ]; then
    echo "ERROR, invalid project folder '$PROJECT_DIR' missing 'pom.xml'" 1>&2
    return 1
  fi
  mkdir -p "$TARGET_DIR"
}

function prepare_working_dir {
  local WORKING_DIR="$1"
  echo "Working dir : $WORKING_DIR"
  if [ -d "$WORKING_DIR" ]; then
    rm -r "$WORKING_DIR"
  fi
  mkdir "$WORKING_DIR"
}

function download_source {
  local SRC_URL="$1"
  local TARGET_FILE="$2"
  echo "Downloading : $SRC_URL"
  echo "Into        : $TARGET_FILE"
  curl --location --max-redirs 10 -o "$TARGET_FILE" "$SRC_URL"
}

function unzip_source {
  local WORKING_DIR="$1"
  local SRC_FILE="$2"
  tar -zxf "$SRC_FILE" --directory "$WORKING_DIR"
}

function compile_source {
  local WORKING_DIR="$1"
  echo "Compiling   : $WORKING_DIR"
  (
    cd "$WORKING_DIR"
    make
  )
}

function run_cppcheck_to_export_rules {
  local CPPCHECK_DIR="$1"
  local OUTPUT_FILE="$2"
  echo "Exporting   : $OUTPUT_FILE"
  (
    cd "$CPPCHECK_DIR"
    ./cppcheck --xml --xml-version=2 --errorlist > "$OUTPUT_FILE"
  )
}

function export_rules {
  local VERSION="$1"
  echo "___________________________________________________________"
  echo "Exporting cppcheck version $VERSION"
  local WORKING_DIR="$TARGET_DIR/cppcheck-export-$VERSION"
  local SRC_URL="http://sourceforge.net/projects/cppcheck/files/cppcheck/$VERSION/cppcheck-$VERSION.tar.gz/download"
  local SRC_FILE="cppcheck-$VERSION.tar.gz"
  local CPPCHECK_DIR="$WORKING_DIR/cppcheck-$VERSION"

  prepare_working_dir "$WORKING_DIR"
  download_source "$SRC_URL" "$WORKING_DIR/$SRC_FILE"
  unzip_source "$WORKING_DIR" "$WORKING_DIR/$SRC_FILE"
  compile_source "$CPPCHECK_DIR"
  run_cppcheck_to_export_rules "$CPPCHECK_DIR" "$SCRIPT_DIR/cppcheck-$VERSION.xml"
  echo "-- success --"
  echo
}

check_project_dir_and_create_target_dir

if [ -z "$1" ]; then
  echo "ERROR, please provide a list of cppcheck version" 1>&2
  echo "Example: ./export-rules.sh 1.68 1.69 1.70" 1>&2
else
  for VERSION in "$@"; do
    export_rules "$VERSION"
  done
fi
