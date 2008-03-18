In some scenarios, usage of the import scope can result in a StackOverflowException or OutOfMemoryException. The tests in this group are designed to define the edges of this problem.

The first build, depMgmt-importPom-noParentCycle, has a module POM that is also referenced from the top level as a dependencyManagement entry with type == pom and scope == import. The dm-pom project (the module in question) DOES NOT specify the top-level POM as its parent. This build should fail on the first execution, but succeed as soon as the dm-pom POM is installed into the local repository. To execute:

1. mvn install
2. Observe the ArtifactNotFoundException listing org.apache.maven.its.mng3391.2:dm-pom:pom:1 as missing. This is because the dm-pom module hasn't been loaded at the time that the dm-pom is required for merging dependencyManagement.
3. cd dm-pom
4. mvn install
5. mvn install (from the top level again)
6. Observe that the build succeeds this time.

The second build, depMgmt-importPom-parentCycle, is designed to show the two error conditions that result from the the dm-pom POM listing the top-level POM as its parent, while the top-level POM simultaneously lists the dm-pom as a module AND as a dependencyManagement entry with scope == import and type == pom. This build will fail with an ArtifactNotFoundException on the first run. On the second run, the dm-pom is first installed manually into the local repository, and will result in either a StackOverflowException or an OutOfMemoryException.

1. mvn install
2. Observe the ArtifactNotFoundException listing org.apache.maven.its.mng3391.2:dm-pom:pom:1 as missing. This is because the dm-pom module hasn't been loaded at the time that the dm-pom is required for merging dependencyManagement.
3. cd dm-pom
4. copy pom.xml to <local-repository>/org/apache/maven/its/mng3391/1/dm-pom/1/dm-pom-1.pom
5. mvn install (from the top level again)
6. Observe that this build brings Maven to its knees.

NOTE: We can't really run this last build (yet) because it will wreck the entire IT run.