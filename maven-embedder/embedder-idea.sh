#!/bin/sh

# At this point I've already tested the embedder. This is just an easy way for the JetBrains folks
# to create the embedder they need for integration.

mvn -Dmaven.test.skip=true -Pidea clean package
