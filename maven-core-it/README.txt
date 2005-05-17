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
