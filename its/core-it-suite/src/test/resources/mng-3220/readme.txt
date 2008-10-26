These tests are designed to test the import scope when used in the dependencyManagement section.

The first, imported-pom-depMgmt, tests whether a dependency in a project's dependencyManagement section of type == pom and scope == import will have its own dependencyManagement section merged with the main project's. If this works, the junit dependency in the project's main dependencies section will have a version provided for it, and the build will succeed. This requires the dm-pom project to be installed first, since there is no other link between the projects than the dependencyManagement specification from project -> dm-pom. To execute:

1. cd dm-pom
2. mvn install
3. cd project
4. mvn package

The second, depMgmt-pom-module-notImported, ensures that POM references in dependencyManagement sections that don't specify scope == import will not have their own dependencyManagement sections merged with those of the referencing POM. The dm-pom POM provides a version for junit in its dependencyManagement section, and the project POM specifies a junit dependency without a version. The top-level POM specifies a reference to the dm-pom POM in its dependencyManagement section, and module entries for both dm-pom and project POMs. When the build is started at the top level, it will fail with an invalid dependency specification in the project POM (the junit version is missing, since the dm-pom's junit specification is not merged). 

NOTE: The dependencyManagement reference to dm-pom in the top-level POM does NOT use scope == import. The scope of dm-pom here is a critical feature of this test.

To execute:

1. mvn install