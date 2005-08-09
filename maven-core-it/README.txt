-------------------------------------------------------------------------------
it0000: The simplest of builds. We have one application class and one test
        class. There are no resources, no source generation, no resource
        generation and a the super model is employed to provide the build
        information.

it0001: Builds upon it0000: we add an application resource that is packaged
        up in the resultant JAR.

it0002: Builds upon it0001: we add the download of a dependency. We delete
        the JAR from the local repository and make sure it is there post build.
       
it0003: Builds upon it0001: we add a jar installation step. We delete the JAR
        from the local repository to make sure it is there post build.
       
it0004: The simplest of pom installation. We have a pom and we install it in
        local repository.
       
it0005: The simplest of pom installation. We have a snapshot pom and we install
        it in local repository.

it0006: Integration test for the verifier plugin.

it0007: We specify a parent in the POM and make sure that it is downloaded as
        part of the process.        

it0008: Simple goal decoration where a plugin binds to a phase and the plugin must
        be downloaded from a remote repository before it can be executed. This
        test also checks to make sure that mojo parameters are aligned to the 
        project basedir when their type is "java.io.File".
        
it0009: Test plugin configuration and goal configuration that overrides what the
        mojo has specified.
        
it0010: Since the artifact resolution does not use the project builder, we must
        ensure that the full hierarchy of all dependencies is resolved. This
        includes the dependencies of the parent-pom's of dependencies. This test
        will check this, by depending on classworlds, which is a dependency of
        maven-component, which is the parent of maven-plugin, which is an
        explicit dependency of this test.
        # TODO: must correct the assumptions of this test
        
it0011: Test specification of dependency versions via <dependencyManagement/>.

it0012: Test simple POM interpolation

it0013: Test plugin-plugin, which tests maven-plugin-tools-api and 
        maven-plugin-tools-java. This will generate a plugin descriptor from 
        java-based mojo sources, install the plugin, and then use it.

it0014: Test POM configuration by settings the -source and -target for the
        compiler to 1.4

it0015: Test marmalade-driven mojo support. This will compile supporting java
        classes (mmld tag & taglib), generate plugin descriptor from mmld script,
        install the plugin, and finally use the new plugin.

it0016: Test a WAR generation

it0017: Test an EJB generation

it0018: Ensure that managed dependencies for dependency POMs are calculated
        correctly when resolved. Removes commons-logging-1.0.3 and checks it is
        redownloaded.

it0019: Test that a version is managed by pluginManagement in the super POM

it0020: Test beanshell mojo support.

it0021: Test pom-level profile inclusion (this one is activated by system
        property).

it0022: Test profile inclusion from profiles.xml (this one is activated by system
        property).

it0023: Test profile inclusion from settings.xml (this one is activated by an id
        in the activeProfiles section).

it0024: Test usage of <executions/> inside a plugin rather than <goals/>
        that are directly inside th plugin.

it0025: Test multiple goal executions with different execution-level configs.

it0026: Test merging of global- and user-level settings.xml files.

it0027: Test @execute with a custom lifecycle, including configuration

it0028: Test that unused configuration parameters from the POM don't cause the
        mojo to fail...they will show up as warnings in the -X output instead.

it0029: Test for pluginManagement injection of plugin configuration.

it0030: Test for injection of dependencyManagement through parents of 
        dependency poms.

it0031: Test usage of plugins.xml mapping file on the repository to resolve
        plugin artifactId from it's prefix using the pluginGroups in
        the provided settings.xml.

it0032: Tests that a specified Maven version requirement that is lower doesn't cause any problems

it0033: Test an EAR generation

it0034: Test version range junit [3.7,) resolves to 3.8.1

it0035: Test artifact relocation.

it0036: Test building from release-pom.xml when it's available

it0037: Test building with alternate pom file using '-f'

it0038: Test building project from outside the project directory using '-f'
        option

it0039: Test reactor for projects that have release-pom.xml in addition to
        pom.xml. The release-pom.xml file should be chosen above pom.xml for
        these projects in the build.

it0040: Test the use of a packaging from a plugin

it0041: Test the use of a new type from a plugin

it0042: Test that the reactor can establish the artifact location of known projects for dependencies

it0043: Test for repository inheritence - ensure using the same id overrides the defaults

it0044: Test --settings CLI option

it0045: Test non-reactor behavior when plugin declares "@requiresProject false"

it0046: Test fail-never reactor behavior. Forces an exception to be thrown in
        the first module, but checks that the second modules is built.

it0047: Test the use case for having a compile time dependency be transitive: when you extend a class you need its
        dependencies at compile time.

-------------------------------------------------------------------------------

- generated sources
- generated resources from sources
- generated resources from generated sources
- filtered resources
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

-------------------------------------------------------------------------------
These are a set of builds that are more complex than single-project or reactor
invocations. They follow a process external to maven itself, invoking
different lifecycle phases and/or goals on multiple projects within each test
directory in order to accomplish their aims. 

NOTE: Currently, there is no verification process...
-------------------------------------------------------------------------------
it2000: Test resolution of plugin by prefix from the first plugin repository
        in a list. This is accomplished by creating a local "remote" repo, and
        deploying a plugin there. Then, in another project, that plugin's goal
        is invoked directly with the plugin's groupId being included in the
        pluginGroups for the build. This second build should pickup the
        plugins.xml for the aforementioned plugin from the first repository
        and execute. This should resolve MNG-592.
-------------------------------------------------------------------------------
