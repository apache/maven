This test should verify that the site plugin version used is 2.0-beta-5, not the latest one (which is currently 2.0-beta-6). It should get this version from the POM's pluginManagement section, even though the site-plugin's mojo call is coming from the default lifecycle bindings, and the site plugin is not specified in the POM's build/plugins section.

To test, run `mvn -X install | tee build.log` or similar, to capture output to a separate file. Then, search the debug output for the string:

org.apache.maven.plugins:maven-site-plugin:2.0-beta-5
