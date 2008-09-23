This is a plugin that uses the ${plugin.artifacts} expression to extract its own collection of dependency artifacts. It will then cycle through this collection looking for commons-cli, which is also part of Maven's core classpath. If the plugin-artifact collection is unfiltered, this artifact should be present. Otherwise, the plugin will throw a MojoExecutionException when it's run.

So, this test entails first installing this plugin, then executing it; two executions in total, and it'll look like this:

mvn install
mvn tests:test-cli-maven-plugin:1:test

(It should succeed on the first one at all times, and only succeed on the second one if the plugin-artifact collection has NOT had commons-cli filtered out.)