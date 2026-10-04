#!/usr/bin/env zsh
#
# macOS port of updateBlueJdeps.ps1.
# Installs BlueJ's bundled dependency jars (e.g. Bluej.jar) into this
# project's local Maven repository under lib/, so pom.xml's
# <repositories><repository id="local_repository">...</repository> can resolve them.
#
# Usage:
#   ./updateBlueJdeps.sh <version> [installDir]
#
#   <version>    Required. Version to install the jar(s) under, e.g. 6.0.0
#                (should match the version used in pom.xml's bluej dependency).
#   [installDir] Optional. Path to the BlueJ.app bundle's lib directory containing
#                the jar(s) to install. If omitted, a few common locations for a
#                BlueJ.app installed under /Applications are tried automatically.

set -euo pipefail

SCRIPT_NAME="${0:t}"
SCRIPT_DIR="${0:A:h}"

usage() {
  echo "Usage: $SCRIPT_NAME <version> [installDir]"
  echo
  echo "  <version>    Required. e.g. 6.0.0"
  echo "  [installDir] Optional. Directory containing the BlueJ jar(s) to install."
  echo "               Defaults to searching common BlueJ.app bundle locations."
}

if [[ $# -lt 1 || -z "${1:-}" ]]; then
  echo "Error: <version> is required." >&2
  usage
  exit 1
fi

VERSION="$1"

# List of jar files to update (matches updateBlueJdeps.ps1).
JAR_DEPENDENCIES=("bluej")

# Candidate locations for the jars inside a BlueJ.app bundle on macOS.
# BlueJ.app's exact internal layout can vary between releases (it's packaged
# with jpackage), so we try the known layouts in order rather than assuming one.
DEFAULT_INSTALL_DIRS=(
  "/Applications/BlueJ.app/Contents/Java"
  "$HOME/Applications/BlueJ.app/Contents/Java"
)

if [[ $# -ge 2 && -n "${2:-}" ]]; then
  INSTALL_DIRS=("$2")
else
  INSTALL_DIRS=("${DEFAULT_INSTALL_DIRS[@]}")
fi

for JAR_NAME in "${JAR_DEPENDENCIES[@]}"; do
  EXTENSION_JAR=""

  for CANDIDATE_DIR in "${INSTALL_DIRS[@]}"; do
    CANDIDATE_JAR="$CANDIDATE_DIR/$JAR_NAME.jar"
    if [[ -f "$CANDIDATE_JAR" ]]; then
      EXTENSION_JAR="$CANDIDATE_JAR"
      break
    fi
  done

  if [[ -z "$EXTENSION_JAR" ]]; then
    echo "Error: could not find $JAR_NAME.jar in any of:" >&2
    printf '  %s\n' "${INSTALL_DIRS[@]/%//$JAR_NAME.jar}" >&2
    echo "Pass the correct directory as the second argument, e.g.:" >&2
    echo "  ./$SCRIPT_NAME $VERSION \"/Applications/BlueJ.app/Contents/Resources/lib\"" >&2
    exit 1
  fi

  echo "Installing $EXTENSION_JAR as $JAR_NAME:$VERSION"

  mvn install:install-file \
    -Dfile="$EXTENSION_JAR" \
    -DgroupId="bluej" \
    -DartifactId="$JAR_NAME" \
    -Dversion="$VERSION" \
    -Dpackaging="jar" \
    -DgeneratePom="true" \
    -DlocalRepositoryPath="$SCRIPT_DIR/../lib/"
done
