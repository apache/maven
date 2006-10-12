Notes:
- today, 3 sets of integration tests, categorized by their ids (it0xxx, it1xxx, it2xxx). 
  see below for what these groups represent
- creating a new test:
 - you can add mojos to the integration-tests plugins/maven-core-it-plugin
 - add log.txt and target to your it test svn ignore list


Format of tests:
-------------------------------------------------------------------------------

Any Maven project plus the following optional files

- goals.txt goals to run

- expected-results.txt path of files expected after build, use "!" as first char to mark it as not expected

Examples:
target/maven-core-it0003-1.0.jar
${artifact:org.apache.maven:maven-core-it0003:1.0:jar}
!target/maven-core-it0016-1.0/WEB-INF/lib/servletapi-2.4-20040521.jar
target/maven-core-it0057-1.0.jar!/it0001.properties

This means that
we expect target/maven-core-it0003-1.0.jar
we expect an artifact in the local repo under org.apache.maven groupId, maven-core-it0003 artifactId, 1.0 version and type jar
we don't expect target/maven-core-it0016-1.0/WEB-INF/lib/servletapi-2.4-20040521.jar
we don't expect it0001.properties inside target/maven-core-it0057-1.0.jar


- prebuild-hook.txt comands to run before the invocation of mvn

Examples:
rm ${artifact:org.apache.maven.plugins:maven-core-it-plugin:1.0:maven-plugin}
rmdir ${basedir}/test project

- cli-options.txt options used in mvn command line

-------------------------------------------------------------------------------

- generated sources
- generated resources from sources
- generated resources from generated sources
- build that requires a plugin download
- transitive dependencies
- goal attainment not requiring depedency resolution
- goal attainment where a POM is not required: this is a case where
  we are using mgen to create new applications and project structures
  which is used by the m2 geronimo plugin and tools like the "setup"
  goal which brings a project to life from scratch using something like:
  m2 --setup xstream --version 1.0

- write a small program to generate a massively nested build
  which which use the reactor and inheritence. we need to have
  integration tests that go far beyond what the average user
  would ever setup.
  
- project with a cyclic dependency

-------------------------------------------------------------------------------
These are a set of builds that contain known errors. The errors should be
captured and reported in a useful manner to the user. We will start at it1000
for intentially flawed builds.
-------------------------------------------------------------------------------
it1000: A build which contains a malformed pom.xml. We have intentionally 
        created a mismatch in the first element. We have:
        <projectX>...</project>
-------------------------------------------------------------------------------
it1001: A build whose pom.xml does not contain a <groupId/> element.
-------------------------------------------------------------------------------
it1002: A build with a syntax error in the first field declaration.
-------------------------------------------------------------------------------
it1003: A build with a simple test failure.
-------------------------------------------------------------------------------

- checksum mismatch
-------------------------------------------------------------------------------
it1005: A build with two mojo java sources that declare the same goal.
-------------------------------------------------------------------------------
it1006: Tests collision on default execution id. Should throw an 
        IllegalStateException, since the model is incorrect.
-------------------------------------------------------------------------------
it1007: Should fail due to requiring a future version of Maven.
-------------------------------------------------------------------------------
it1008: Should fail due to requiring a version range for junit that doesn't exist
        in the repository. Currently succeeds (MNG-614)
-------------------------------------------------------------------------------
it1009: Tests packaging from a plugin fails when extensions is not true.
-------------------------------------------------------------------------------
it1010: Tests a type from a plugin fails when extensions is not true.
-------------------------------------------------------------------------------
it1011: Tests the fail-at-end reactor behavior. First module fails, and second
        should also run but not fail. End result should be failure of the build.
-------------------------------------------------------------------------------
it1012: Test that the DefaultLifecycleExecutor will throw an exception when
        encountering an unknown packaging.



-------------------------------------------------------------------------------
it1015: Test that expressions that self-reference within the POM result in an
        error during POM interpolation.
-------------------------------------------------------------------------------

-------------------------------------------------------------------------------
it1018: Test formatting of error caused by invalid profiles.xml syntax.
-------------------------------------------------------------------------------
it1019: A test that ensures that an exception is thrown when two artifacts
        with the same id are present in the reactor.
-------------------------------------------------------------------------------


-------------------------------------------------------------------------------
These are a set of builds that are more complex than single-project or reactor
invocations. They follow a process external to maven itself, invoking
different lifecycle phases and/or goals on multiple projects within each test
directory in order to accomplish their aims. 

NOTE: Currently, there is no automatic verification process for these...
-------------------------------------------------------------------------------
it2000: Test resolution of plugin by prefix from the first plugin repository
        in a list. This is accomplished by creating a local "remote" repo, and
        deploying a plugin there. Then, in another project, that plugin's goal
        is invoked directly with the plugin's groupId being included in the
        pluginGroups for the build. This second build should pickup the
        plugins.xml for the aforementioned plugin from the first repository
        and execute. This should resolve MNG-592.

it2001: Test that repositories are accumulated as the artifact resolution
        process traverses successive layers of transitive dependencies, such
        that transitive dependencies can be resolved from repositories defined
        in the top-level pom.xml. See MNG-757.


it2002: Test the release plugin.

it2003: Test that versions specified in pluginManagement are used when plugins
        are resolved as direct command-line goals, or as implied lifecycle
        bindings.

-------------------------------------------------------------------------------

