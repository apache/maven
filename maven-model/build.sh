#!/bin/sh

rm -rf target

# Generate the sources from the model
modello --model=maven.mdo --version=4.0.0 --mode=java --dir=target/generated-sources
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

# Generate the source for the xpp3 marshaller and unmarshaller
modello --model=maven.mdo --version=4.0.0 --mode=xpp3 --dir=target/generated-sources
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

# Generate the 3.0.0 source from the model
modello --model=maven.mdo --version=3.0.0 --mode=java --dir=target/generated-sources --package-with-version
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

# Generate the 3.0.0 source for the xpp3 marshaller and unmarshaller
modello --model=maven.mdo --version=3.0.0 --mode=xpp3 --dir=target/generated-sources --package-with-version
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

# Generate XML Schema
modello --model=maven.mdo --version=4.0.0 --mode=xsd --dir=.
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

# Generate XDoc
modello --model=maven.mdo --version=4.0.0 --mode=xdoc --dir=target/generated-xdocs
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

# Build the sources
mboot --install
ret=$?; if [ $ret != 0 ]; then exit $ret; fi
