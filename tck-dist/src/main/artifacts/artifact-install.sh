#!/usr/bin/env bash
# Installs the TCK artifacts in this directory into the local Maven repository.
set -euo pipefail

cd "$(dirname "$0")"

POM=$(ls jakarta.agentic-ai-parent-*.pom)
TCK=$(ls jakarta.agentic-ai-tck-*.jar | grep -v -- '-sources')
SOURCES=$(ls jakarta.agentic-ai-tck-*-sources.jar)

VERSION=${POM#jakarta.agentic-ai-parent-}
VERSION=${VERSION%.pom}

echo "Installing $POM"
mvn -q org.apache.maven.plugins:maven-install-plugin:3.1.3:install-file \
  -Dfile="$POM" \
  -DgroupId=jakarta.agentic-ai \
  -DartifactId=jakarta.agentic-ai-parent \
  -Dversion="$VERSION" \
  -Dpackaging=pom

echo "Installing $TCK with sources $SOURCES"
mvn -q org.apache.maven.plugins:maven-install-plugin:3.1.3:install-file \
  -Dfile="$TCK" \
  -Dsources="$SOURCES"

echo "Installed jakarta.agentic-ai:jakarta.agentic-ai-tck:$VERSION"
