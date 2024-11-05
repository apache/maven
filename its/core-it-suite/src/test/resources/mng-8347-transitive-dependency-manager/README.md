# MRESOLVER-614

Facts:
* root POM defines depMgt for level6 1.0.2
* level1 POM defines depMgt for level5=1.0.2 and level6=1.0.1
* level2 POM defines depMgt for level3=1.0.1, level4=1.0.1 and level5=1.0.1
* without transitive manager (Maven3) all we see is root POM depMgt is applied (level6 = 1.0.2)
* with transitive manager (Maven4) we should see level3 unchanged 1.0.0 (as level2 should not apply own depMgt onto its own dependencies), level4 managed to 1.0.1, and level5 managed by root to 1.0.2 (and not 1.0.1)

Command to run:

```
mvn eu.maveniverse.maven.plugins:toolbox:tree -Dmaven.repo.local.tail=local-repo -Dmaven.repo.local.tail.ignoreAvailability
```

Example output with 3.9.9: Maven 3 is not transitive regarding dependency management, and it shows 1.0.0 all way down
except for level5 that has applies depMgt from root.
```
$ mvn -V eu.maveniverse.maven.plugins:toolbox:tree -Dmaven.repo.local.tail=local-repo -Dmaven.repo.local.tail.ignoreAvailability
Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)
Maven home: /home/cstamas/.sdkman/candidates/maven/3.9.9
Java version: 21.0.4, vendor: Eclipse Adoptium, runtime: /home/cstamas/.sdkman/candidates/java/21.0.4-tem
Default locale: en_US, platform encoding: UTF-8
OS name: "linux", version: "6.11.4-201.fc40.x86_64", arch: "amd64", family: "unix"
[INFO] Scanning for projects...
[INFO] 
[INFO] ---------------< org.apache.maven.it.mresolver614:root >----------------
[INFO] Building root 1.0.0
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- toolbox:0.3.5:tree (default-cli) @ root ---
[INFO] org.apache.maven.it.mresolver614:root:jar:1.0.0
[INFO] ╰─org.apache.maven.it.mresolver614:level1:jar:1.0.0 [compile]
[INFO]   ╰─org.apache.maven.it.mresolver614:level2:jar:1.0.0 [compile]
[INFO]     ╰─org.apache.maven.it.mresolver614:level3:jar:1.0.0 [compile]
[INFO]       ╰─org.apache.maven.it.mresolver614:level4:jar:1.0.0 [compile]
[INFO]         ╰─org.apache.maven.it.mresolver614:level5:jar:1.0.0 [compile]
[INFO]           ╰─org.apache.maven.it.mresolver614:level6:jar:1.0.2 [compile] (version managed from 1.0.0)
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  0.192 s
[INFO] Finished at: 2024-10-24T19:20:39+02:00
[INFO] ------------------------------------------------------------------------
$ 
```

Example output with 4.0.0-beta-5: **this version is transitive but broken**, as it applies level2 depMgt onto its own
dependencies.
```
$ mvn -V eu.maveniverse.maven.plugins:toolbox:tree -Dmaven.repo.local.tail=local-repo -Dmaven.repo.local.tail.ignoreAvailability
Apache Maven 4.0.0-beta-5 (6e78fcf6f5e76422c0eb358cd11f0c231ecafbad)
Maven home: /home/cstamas/.sdkman/candidates/maven/4.0.0-beta-5
Java version: 21.0.4, vendor: Eclipse Adoptium, runtime: /home/cstamas/.sdkman/candidates/java/21.0.4-tem
Default locale: en_US, platform encoding: UTF-8
OS name: "linux", version: "6.11.4-201.fc40.x86_64", arch: "amd64", family: "unix"
[WARNING] Unable to find the root directory. Create a .mvn directory in the root directory or add the root="true" attribute on the root project's model to identify it.
[WARNING] Legacy/insecurely encrypted password detected for server my-legacy-server
[WARNING] Legacy/insecurely encrypted password detected for server my-legacy-broken-server
[INFO] Scanning for projects...
[INFO] 
[INFO] ----------------------------------------< org.apache.maven.it.mresolver614:root >-----------------------------------------
[INFO] Building root 1.0.0
[INFO]   from pom.xml
[INFO] ---------------------------------------------------------[ jar ]----------------------------------------------------------
[INFO] 
[INFO] --- toolbox:0.3.5:tree (default-cli) @ root ---
[INFO] org.apache.maven.it.mresolver614:root:jar:1.0.0
[INFO] ╰─org.apache.maven.it.mresolver614:level1:jar:1.0.0 [compile]
[INFO]   ╰─org.apache.maven.it.mresolver614:level2:jar:1.0.0 [compile]
[INFO]     ╰─org.apache.maven.it.mresolver614:level3:jar:1.0.1 [compile] (version managed from 1.0.0)
[INFO]       ╰─org.apache.maven.it.mresolver614:level4:jar:1.0.1 [compile] (version managed from 1.0.0)
[INFO]         ╰─org.apache.maven.it.mresolver614:level5:jar:1.0.2 [compile] (version managed from 1.0.0)
[INFO]           ╰─org.apache.maven.it.mresolver614:level6:jar:1.0.2 [compile] (version managed from 1.0.0)
[INFO] --------------------------------------------------------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] --------------------------------------------------------------------------------------------------------------------------
[INFO] Total time:  0.285 s
[INFO] Finished at: 2024-10-24T19:21:10+02:00
[INFO] --------------------------------------------------------------------------------------------------------------------------
$ 
```

The **expected** output is:
```
[INFO] --- toolbox:0.3.5:tree (default-cli) @ root ---
[INFO] org.apache.maven.it.mresolver614:root:jar:1.0.0
[INFO] ╰─org.apache.maven.it.mresolver614:level1:jar:1.0.0 [compile]
[INFO]   ╰─org.apache.maven.it.mresolver614:level2:jar:1.0.0 [compile]
[INFO]     ╰─org.apache.maven.it.mresolver614:level3:jar:1.0.0 [compile] == unmanaged, level2 depends on level3:1.0.0
[INFO]       ╰─org.apache.maven.it.mresolver614:level4:jar:1.0.1 [compile] (version managed from 1.0.0) == by level2
[INFO]         ╰─org.apache.maven.it.mresolver614:level5:jar:1.0.2 [compile] (version managed from 1.0.0) == by level1 to 1.0.2 and level2 to 1.0.1 but level1 wins (closer to root)
[INFO]           ╰─org.apache.maven.it.mresolver614:level6:jar:1.0.2 [compile] (version managed from 1.0.0) == by root to 1.0.2 and level1 to 1.0.1 but root wins (closer to root)
```

Maven 4.0.0-SNAPSHOT + Resolver [2.0.3-SNAPSHOT](https://github.com/apache/maven-resolver/pull/588) output **is expected output**:
```
$ ~/Tools/maven/apache-maven-4.0.0-beta-6-SNAPSHOT/bin/mvn -V eu.maveniverse.maven.plugins:toolbox:tree -Dmaven.repo.local.tail=local-repo -Dmaven.repo.local.tail.ignoreAvailability
Apache Maven 4.0.0-beta-6-SNAPSHOT (cf94fba0151ff403763bdf23eb73fe74b3d0874d)
Maven home: /home/cstamas/Tools/maven/apache-maven-4.0.0-beta-6-SNAPSHOT
Java version: 21.0.4, vendor: Eclipse Adoptium, runtime: /home/cstamas/.sdkman/candidates/java/21.0.4-tem
Default locale: en_US, platform encoding: UTF-8
OS name: "linux", version: "6.11.4-201.fc40.x86_64", arch: "amd64", family: "unix"
[WARNING] Unable to find the root directory. Create a .mvn directory in the root directory or add the root="true" attribute on the root project's model to identify it.
[WARNING] Legacy/insecurely encrypted password detected for server my-legacy-server
[WARNING] Legacy/insecurely encrypted password detected for server my-legacy-broken-server
[INFO] Scanning for projects...
[INFO] 
[INFO] ----------------------------------------< org.apache.maven.it.mresolver614:root >-----------------------------------------
[INFO] Building root 1.0.0
[INFO]   from pom.xml
[INFO] ---------------------------------------------------------[ jar ]----------------------------------------------------------
[INFO] 
[INFO] --- toolbox:0.3.5:tree (default-cli) @ root ---
[INFO] org.apache.maven.it.mresolver614:root:jar:1.0.0
[INFO] ╰─org.apache.maven.it.mresolver614:level1:jar:1.0.0 [compile]
[INFO]   ╰─org.apache.maven.it.mresolver614:level2:jar:1.0.0 [compile]
[INFO]     ╰─org.apache.maven.it.mresolver614:level3:jar:1.0.0 [compile]
[INFO]       ╰─org.apache.maven.it.mresolver614:level4:jar:1.0.1 [compile] (version managed from 1.0.0)
[INFO]         ╰─org.apache.maven.it.mresolver614:level5:jar:1.0.2 [compile] (version managed from 1.0.0)
[INFO]           ╰─org.apache.maven.it.mresolver614:level6:jar:1.0.2 [compile] (version managed from 1.0.0)
[INFO] --------------------------------------------------------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] --------------------------------------------------------------------------------------------------------------------------
[INFO] Total time:  0.312 s
[INFO] Finished at: 2024-10-24T21:11:21+02:00
[INFO] --------------------------------------------------------------------------------------------------------------------------
$ 
```