#!/bin/sh

# Generate the sources from the model
modello --model=maven.mdo --mode=java --dir=src/main/java

# Build the sources
mboot

# Generate XML Schema
modello --model=maven.mdo --mode=xsd --dir=.

# Generate XDoc
modello --model=maven.mdo --mode=xdoc --dir=.
