#!/bin/sh

# Generate the sources from the model
modello --model=maven.mdo --mode=java --dir=src/main/java

# Generate the source for the xpp3 marshaller and unmarshaller
modello --model=maven.mdo --mode=xpp3 --dir=src/main/java

# Build the sources
mboot

# Generate XML Schema
modello --model=maven.mdo --mode=xsd --dir=.

# Generate XDoc
modello --model=maven.mdo --mode=xdoc --dir=./xdocs
