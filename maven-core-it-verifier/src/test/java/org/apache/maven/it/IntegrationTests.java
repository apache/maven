package org.apache.maven.it;                                                                                                                                                                                                                
                                                                                                                                                                                                                                            
import java.io.*;                                                                                                                                                                                                                           
import java.util.*;                                                                                                                                                                                                                         
                                                                                                                                                                                                                                            
import junit.framework.*;                                                                                                                                                                                                                   
                                                                                                                                                                                                                                            
import org.apache.maven.it.*;                                                                                                                                                                                                               
import org.codehaus.plexus.util.*;

public class IntegrationTests extends TestCase 
{
    private static final String rootdir = System.getProperty("maven.it.dir", "maven-core-it");

    private Verifier verifier;                                                                                                                                                                                                                                                
        
    public IntegrationTests(String name) 
    {
        super(name);                                                                                                                                                                                                                        
    }                                                                                                                                                                                                                                       
                                                                                                                                                                                                                                                        
    public static Test suite() 
    {
        String[] tests = new String[] 
        {
//"#it0107 requires a snapshot version of maven-plugin-plugin, which indicates it doesn't belong here",
//"#it0106 MNG-2318 not yet fixed",
"it0105",
//"#it0104 Commenting out, not fixed until post-2.0.4, due to dependency on new plexus-container-default version.",
"it0103",
"it0102",
"it0101",
"it0100",
"it0099",
//"# it0098 - something started failing here, not yet identified. MNG-2322",
//"#it0097 MNG-870",
//"#it0096 MNG-870",
"it0095",
"it0094",
"it0092",
//"# it0091 currrently fails. Not sure if there is an associated JIRA.",
"it0090",
"it0089",
"it0088",
"it0087",
"it0086",
"it0085",
"it0084",
"it0083",
"it0082",
//"#it0081 MNG-2603",
"it0080",
"it0079",
"it0078",
"it0077",
"it0076",
"it0075",
"it0074",
"it0073",
"it0072",
"it0071",
"it0070",
"it0069",
"it0068",
"it0067",
"it0066",
"it0065",
"it0064",
"it0063",
"it0062",
"it0061",
"it0060",
"it0059",
"it0058",
"it0057",
"it0056",
"it0055",
"it0054",
"it0053",
"it0052",
"it0051",
"it0050",
"it0049",
"it0048",
"it0047",
"it0046",
"it0045",
"it0044",
"it0043",
//"#it0042 MNG-870",
"it0041",
"it0040",
"it0039",
"it0038",
"it0037",
"it0036",
"it0035",
"it0034",
"it0033",
"it0032",
"it0031",
"it0030",
"it0029",
"it0028",
"it0027",
"it0026",
"it0025",
"it0024",
"it0023",
"it0022",
"it0021",
"it0020",
"it0019",
"it0018",
"it0017",
"it0016",
//"# it0015 reserved in rememberance of Marmalade. We knew you so little.",
"it0014",
"it0013",
"it0012",
"it0011",
"it0010",
"it0009",
"it0008",
"it0007",
"it0006",
"it0005",
"it0004",
"it0003",
"it0002",
"it0001",
"it0000",
};
/** The simplest of builds. We have one application class and one test
        class. There are no resources, no source generation, no resource
        generation and a the super model is employed to provide the build
        information.
 */
public void test_it0000() throws Exception {
File basedir = new File(rootdir, "it0000");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/classes/org/apache/maven/it0000/Person.class");
verifier.assertFilePresent("target/test-classes/org/apache/maven/it0000/PersonTest.class");
verifier.assertFilePresent("target/maven-core-it0000-1.0.jar");
verifier.assertFilePresent("target/surefire-reports/org.apache.maven.it0000.PersonTest.txt");
verifier.verifyErrorFreeLog();
}

/** Builds upon it0000: we add an application resource that is packaged
        up in the resultant JAR.
 */
public void test_it0001() throws Exception {
File basedir = new File(rootdir, "it0001");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/classes/org/apache/maven/it0001/Person.class");
verifier.assertFilePresent("target/test-classes/org/apache/maven/it0001/PersonTest.class");
verifier.assertFilePresent("target/maven-core-it0001-1.0.jar");
verifier.assertFilePresent("target/maven-core-it0001-1.0.jar!/it0001.properties");
verifier.verifyErrorFreeLog();
}

/** Builds upon it0001: we add the download of a dependency. We delete
        the JAR from the local repository and make sure it is there post build.
        */
public void test_it0002() throws Exception {
File basedir = new File(rootdir, "it0002");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven", "maven-core-it-support", "1.0", "jar");
verifier.executeGoal("package");
verifier.assertFilePresent("target/classes/org/apache/maven/it0002/Person.class");
verifier.assertFilePresent("target/test-classes/org/apache/maven/it0002/PersonTest.class");
verifier.assertFilePresent("target/maven-core-it0002-1.0.jar");
verifier.assertFilePresent("target/maven-core-it0002-1.0.jar!/it0002.properties");
verifier.assertArtifactPresent("org.apache.maven", "maven-core-it-support", "1.0", "jar");
verifier.verifyErrorFreeLog();
}

/** Builds upon it0001: we add a jar installation step. We delete the JAR
        from the local repository to make sure it is there post build.
        */
public void test_it0003() throws Exception {
File basedir = new File(rootdir, "it0003");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven", "maven-core-it0003", "1.0", "jar");
verifier.executeGoal("install");
verifier.assertFilePresent("target/classes/org/apache/maven/it0003/Person.class");
verifier.assertFilePresent("target/test-classes/org/apache/maven/it0003/PersonTest.class");
verifier.assertFilePresent("target/maven-core-it0003-1.0.jar");
verifier.assertFilePresent("target/maven-core-it0003-1.0.jar!/it0003.properties");
verifier.assertArtifactPresent("org.apache.maven", "maven-core-it0003", "1.0", "jar");
verifier.verifyErrorFreeLog();
}

/** The simplest of pom installation. We have a pom and we install it in
        local repository.
        */
public void test_it0004() throws Exception {
File basedir = new File(rootdir, "it0004");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven", "maven-core-it0004", "1.0", "pom");
verifier.executeGoal("install:install");
verifier.assertArtifactPresent("org.apache.maven", "maven-core-it0004", "1.0", "pom");
verifier.verifyErrorFreeLog();
}

/** The simplest of pom installation. We have a snapshot pom and we install
        it in local repository.
 */
public void test_it0005() throws Exception {
File basedir = new File(rootdir, "it0005");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven", "maven-core-it0005", "1.0-SNAPSHOT", "pom");
verifier.executeGoal("install:install");
verifier.assertArtifactPresent("org.apache.maven", "maven-core-it0005", "1.0-SNAPSHOT", "pom");
verifier.verifyErrorFreeLog();
}

/** Integration test for the verifier plugin.
 */
public void test_it0006() throws Exception {
File basedir = new File(rootdir, "it0006");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("integration-test");
verifier.verifyErrorFreeLog();
}

/** We specify a parent in the POM and make sure that it is downloaded as
        part of the process.        
 */
public void test_it0007() throws Exception {
File basedir = new File(rootdir, "it0007");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.plugins", "maven-plugin-parent", "2.0", "pom");
verifier.executeGoal("package");
verifier.assertFilePresent("target/classes/org/apache/maven/it0007/Person.class");
verifier.assertFilePresent("target/test-classes/org/apache/maven/it0007/PersonTest.class");
verifier.assertFilePresent("target/maven-core-it0007-1.0.jar");
verifier.assertFilePresent("target/maven-core-it0007-1.0.jar!/it0007.properties");
verifier.assertArtifactPresent("org.apache.maven.plugins", "maven-plugin-parent", "2.0", "pom");
verifier.verifyErrorFreeLog();
}

/** Simple goal decoration where a plugin binds to a phase and the plugin must
        be downloaded from a remote repository before it can be executed. This
        test also checks to make sure that mojo parameters are aligned to the 
        project basedir when their type is "java.io.File".
         */
public void test_it0008() throws Exception {
File basedir = new File(rootdir, "it0008");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.plugins", "maven-core-it-plugin", "1.0", "maven-plugin");
verifier.executeGoal("compile");
verifier.assertFilePresent("target/touch.txt");
verifier.assertFilePresent("target/test-basedir-alignment/touch.txt");
verifier.verifyErrorFreeLog();
}

/** Test plugin configuration and goal configuration that overrides what the
        mojo has specified.
         */
public void test_it0009() throws Exception {
File basedir = new File(rootdir, "it0009");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.plugins", "maven-core-it-plugin", "1.0", "maven-plugin");
verifier.executeGoal("generate-resources");
verifier.assertFilePresent("target/pluginItem");
verifier.assertFilePresent("target/goalItem");
verifier.assertFileNotPresent("target/bad-item");
verifier.verifyErrorFreeLog();
}

/** Since the artifact resolution does not use the project builder, we must
        ensure that the full hierarchy of all dependencies is resolved. This
        includes the dependencies of the parent-pom's of dependencies. This test
        will check this, by depending on classworlds, which is a dependency of
        maven-component, which is the parent of maven-plugin, which is an
        explicit dependency of this test.
        # TODO: must correct the assumptions of this test
         */
public void test_it0010() throws Exception {
File basedir = new File(rootdir, "it0010");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("compile");
verifier.assertFilePresent("target/classes/org/apache/maven/it0010/PersonFinder.class");
verifier.verifyErrorFreeLog();
}

/** Test specification of dependency versions via <dependencyManagement/>.
 */
public void test_it0011() throws Exception {
File basedir = new File(rootdir, "it0011");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("compile");
verifier.assertFilePresent("target/classes/org/apache/maven/it0011/PersonFinder.class");
verifier.verifyErrorFreeLog();
}

/** Test simple POM interpolation
 */
public void test_it0012() throws Exception {
File basedir = new File(rootdir, "it0012");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("core-it:touch");
verifier.assertFilePresent("target/touch-3.8.1.txt");
verifier.assertFilePresent("child-project/target/child-touch-3.0.3.txt");
verifier.verifyErrorFreeLog();
}

/** Test plugin-plugin, which tests maven-plugin-tools-api and 
        maven-plugin-tools-java. This will generate a plugin descriptor from 
        java-based mojo sources, install the plugin, and then use it.
 */
public void test_it0013() throws Exception {
File basedir = new File(rootdir, "it0013");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.plugins", "maven-it0013-plugin", "1.0-SNAPSHOT", "maven-plugin");
List goals = Arrays.asList(new String[] {"install", "it0013:it0013"});
verifier.executeGoals(goals);
verifier.assertFilePresent("target/maven-it0013-plugin-1.0-SNAPSHOT.jar");
verifier.assertFilePresent("target/it0013-verify");
verifier.verifyErrorFreeLog();
}

/** Test POM configuration by settings the -source and -target for the
        compiler to 1.4
 */
public void test_it0014() throws Exception {
File basedir = new File(rootdir, "it0014");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("test");
verifier.assertFilePresent("target/classes/org/apache/maven/it0001/Person.class");
verifier.verifyErrorFreeLog();
}

/** Test a WAR generation
 */
public void test_it0016() throws Exception {
File basedir = new File(rootdir, "it0016");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/classes/org/apache/maven/it0016/Person.class");
verifier.assertFilePresent("target/maven-core-it0016-1.0/index.html");
verifier.assertFilePresent("target/maven-core-it0016-1.0/WEB-INF/classes/org/apache/maven/it0016/Person.class");
verifier.assertFilePresent("target/maven-core-it0016-1.0/WEB-INF/lib/commons-logging-1.0.3.jar");
verifier.assertFileNotPresent("target/maven-core-it0016-1.0/WEB-INF/lib/servletapi-2.4-20040521.jar");
verifier.assertFilePresent("target/maven-core-it0016-1.0.war");
verifier.assertFilePresent("target/maven-core-it0016-1.0.war!/index.html");
verifier.assertFilePresent("target/maven-core-it0016-1.0.war!/WEB-INF/classes/org/apache/maven/it0016/Person.class");
verifier.assertFilePresent("target/maven-core-it0016-1.0.war!/WEB-INF/lib/commons-logging-1.0.3.jar");
verifier.verifyErrorFreeLog();
}

/** Test an EJB generation
 */
public void test_it0017() throws Exception {
File basedir = new File(rootdir, "it0017");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/classes/org/apache/maven/it0017/Person.class");
verifier.assertFilePresent("target/maven-core-it0017-1.0.jar");
verifier.assertFilePresent("target/maven-core-it0017-1.0.jar!/META-INF/ejb-jar.xml");
verifier.assertFilePresent("target/maven-core-it0017-1.0.jar!/org/apache/maven/it0017/Person.class");
verifier.verifyErrorFreeLog();
}

/** Ensure that managed dependencies for dependency POMs are calculated
        correctly when resolved. Removes commons-logging-1.0.3 and checks it is
        redownloaded.
 */
public void test_it0018() throws Exception {
File basedir = new File(rootdir, "it0018");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("commons-logging", "commons-logging", "1.0.3", "jar");
verifier.executeGoal("package");
// TODO: I would like to build some small core-it artifacts for this purpose instead
verifier.assertArtifactPresent("commons-logging", "commons-logging", "1.0.3", "jar");
verifier.verifyErrorFreeLog();
}

/** Test that a version is managed by pluginManagement in the super POM
 */
public void test_it0019() throws Exception {
File basedir = new File(rootdir, "it0019");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("compile");
verifier.assertFilePresent("target/classes/org/apache/maven/it0019/Person.class");
verifier.verifyErrorFreeLog();
}

/** Test beanshell mojo support.
 */
public void test_it0020() throws Exception {
File basedir = new File(rootdir, "it0020");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.plugins", "maven-it0020-plugin", "1.0-SNAPSHOT", "maven-plugin");
FileUtils.deleteFile(new File(basedir, "target/out.txt"));
List goals = Arrays.asList(new String[] {"install", "it0020:it0020"});
verifier.executeGoals(goals);
verifier.assertFilePresent("target/out.txt");
verifier.verifyErrorFreeLog();
}

/** Test pom-level profile inclusion (this one is activated by system
        property).
 */
public void test_it0021() throws Exception {
File basedir = new File(rootdir, "it0021");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven", "maven-core-it-support", "1.0", "jar");
Properties systemProperties = new Properties();
systemProperties.put("includeProfile", "true");
verifier.setSystemProperties(systemProperties);
verifier.executeGoal("compile");
verifier.assertArtifactPresent("org.apache.maven", "maven-core-it-support", "1.0", "jar");
verifier.verifyErrorFreeLog();
}

/** Test profile inclusion from profiles.xml (this one is activated by system
        property).
 */
public void test_it0022() throws Exception {
File basedir = new File(rootdir, "it0022");
verifier = new Verifier(basedir.getAbsolutePath());
Properties systemProperties = new Properties();
systemProperties.put("includeProfile", "true");
verifier.setSystemProperties(systemProperties);
verifier.executeGoal("core-it:touch");
verifier.assertFilePresent("target/test.txt");
verifier.verifyErrorFreeLog();
}

/** Test profile inclusion from settings.xml (this one is activated by an id
        in the activeProfiles section).
 */
public void test_it0023() throws Exception {
File basedir = new File(rootdir, "it0023");
verifier = new Verifier(basedir.getAbsolutePath());
Properties systemProperties = new Properties();
systemProperties.put("org.apache.maven.user-settings", "settings.xml");
verifier.setSystemProperties(systemProperties);
verifier.executeGoal("core-it:touch");
verifier.assertFilePresent("target/test.txt");
verifier.verifyErrorFreeLog();
}

/** Test usage of <executions/> inside a plugin rather than <goals/>
        that are directly inside th plugin.
 */
public void test_it0024() throws Exception {
File basedir = new File(rootdir, "it0024");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("generate-sources");
verifier.assertFilePresent("target/classes/org/apache/maven/it0001/Person.class");
verifier.verifyErrorFreeLog();
}

/** Test multiple goal executions with different execution-level configs.
 */
public void test_it0025() throws Exception {
File basedir = new File(rootdir, "it0025");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("process-sources");
verifier.assertFilePresent("target/test.txt");
verifier.assertFilePresent("target/test2.txt");
verifier.verifyErrorFreeLog();
}

/** Test merging of global- and user-level settings.xml files.
 */
public void test_it0026() throws Exception {
File basedir = new File(rootdir, "it0026");
verifier = new Verifier(basedir.getAbsolutePath());
Properties systemProperties = new Properties();
systemProperties.put("org.apache.maven.user-settings", "user-settings.xml");
systemProperties.put("org.apache.maven.global-settings", "global-settings.xml");
verifier.setSystemProperties(systemProperties);
verifier.executeGoal("core-it:touch");
verifier.assertFilePresent("target/test.txt");
verifier.verifyErrorFreeLog();
}

/** Test @execute with a custom lifecycle, including configuration
 */
public void test_it0027() throws Exception {
File basedir = new File(rootdir, "it0027");
verifier = new Verifier(basedir.getAbsolutePath());
List goals = Arrays.asList(new String[] {"core-it:fork", "core-it:fork-goal"});
verifier.executeGoals(goals);
verifier.assertFilePresent("target/forked/touch.txt");
verifier.assertFilePresent("target/forked2/touch.txt");
verifier.verifyErrorFreeLog();
}

/** Test that unused configuration parameters from the POM don't cause the
        mojo to fail...they will show up as warnings in the -X output instead.
 */
public void test_it0028() throws Exception {
File basedir = new File(rootdir, "it0028");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("test");
verifier.assertFilePresent("target/classes/org/apache/maven/it0001/Person.class");
verifier.verifyErrorFreeLog();
}

/** Test for pluginManagement injection of plugin configuration.
 */
public void test_it0029() throws Exception {
File basedir = new File(rootdir, "it0029");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.it", "maven-core-it0029", "1.0-SNAPSHOT", "jar");
verifier.deleteArtifact("org.apache.maven.it", "maven-core-it0029-child", "1.0-SNAPSHOT", "jar");
verifier.executeGoal("install");
verifier.assertFilePresent("child-project/target/classes/org/apache/maven/it0001/Person.class");
verifier.verifyErrorFreeLog();
}

/** Test for injection of dependencyManagement through parents of 
        dependency poms.
 */
public void test_it0030() throws Exception {
File basedir = new File(rootdir, "it0030");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.it", "maven-core-it0030", "1.0-SNAPSHOT", "jar");
verifier.deleteArtifact("org.apache.maven.it", "maven-core-it0030-child-hierarchy", "1.0-SNAPSHOT", "jar");
verifier.deleteArtifact("org.apache.maven.it", "maven-core-it0030-child-project1", "1.0-SNAPSHOT", "jar");
verifier.deleteArtifact("org.apache.maven.it", "maven-core-it0030-child-project2", "1.0-SNAPSHOT", "jar");
verifier.executeGoal("install");
verifier.assertFilePresent("child-hierarchy/project2/target/classes/org/apache/maven/it0001/Person.class");
verifier.verifyErrorFreeLog();
}

/** Test usage of plugins.xml mapping file on the repository to resolve
        plugin artifactId from it's prefix using the pluginGroups in
        the provided settings.xml.
 */
public void test_it0031() throws Exception {
File basedir = new File(rootdir, "it0031");
verifier = new Verifier(basedir.getAbsolutePath());
Properties systemProperties = new Properties();
systemProperties.put("org.apache.maven.user-settings", "settings.xml");
systemProperties.put("model", "src/main/mdo/test.mdo");
systemProperties.put("version", "1.0.0");
verifier.setSystemProperties(systemProperties);
Properties verifierProperties = new Properties();
verifierProperties.put("failOnErrorOutput", "false");
verifier.setVerifierProperties(verifierProperties);
verifier.executeGoal("modello:java");
verifier.assertFilePresent("target/generated-sources/modello/org/apache/maven/it/it0031/Root.java");
// don't verify error free log
}

/** Tests that a specified Maven version requirement that is lower doesn't cause any problems
 */
public void test_it0032() throws Exception {
File basedir = new File(rootdir, "it0032");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/classes/org/apache/maven/it0032/Person.class");
verifier.assertFilePresent("target/test-classes/org/apache/maven/it0032/PersonTest.class");
verifier.assertFilePresent("target/maven-core-it0032-1.0.jar");
verifier.assertFilePresent("target/maven-core-it0032-1.0.jar!/it0032.properties");
verifier.verifyErrorFreeLog();
}

/** Test an EAR generation
 */
public void test_it0033() throws Exception {
File basedir = new File(rootdir, "it0033");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/maven-core-it00xx-1.0.ear");
verifier.assertFilePresent("target/maven-core-it00xx-1.0.ear!/META-INF/application.xml");
verifier.assertFilePresent("target/maven-core-it00xx-1.0.ear!/META-INF/appserver-application.xml");
verifier.verifyErrorFreeLog();
}

/** Test version range junit [3.7,) resolves to 3.8.1
 */
public void test_it0034() throws Exception {
File basedir = new File(rootdir, "it0034");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven", "maven-core-it-support", "1.4", "jar");
verifier.deleteArtifact("junit", "junit", "3.8", "jar");
verifier.executeGoal("package");
verifier.assertArtifactPresent("junit", "junit", "3.8", "jar");
verifier.assertArtifactPresent("org.apache.maven", "maven-core-it-support", "1.4", "jar");
verifier.verifyErrorFreeLog();
}

/** Test artifact relocation.
 */
public void test_it0035() throws Exception {
File basedir = new File(rootdir, "it0035");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven", "maven-core-it-support", "1.1", "jar");
verifier.deleteArtifact("org.apache.maven", "maven-core-it-support", "1.1", "pom");
verifier.deleteArtifact("org.apache.maven", "maven-core-it-support-old-location", "1.1", "pom");
verifier.executeGoal("package");
verifier.assertArtifactPresent("org.apache.maven", "maven-core-it-support", "1.1", "jar");
verifier.assertArtifactPresent("org.apache.maven", "maven-core-it-support", "1.1", "pom");
verifier.assertArtifactPresent("org.apache.maven", "maven-core-it-support-old-location", "1.1", "pom");
verifier.verifyErrorFreeLog();
}

/** Test building from release-pom.xml when it's available
 */
public void test_it0036() throws Exception {
File basedir = new File(rootdir, "it0036");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/maven-core-it0036-1.0.jar");
verifier.verifyErrorFreeLog();
}

/** Test building with alternate pom file using '-f'
 */
public void test_it0037() throws Exception {
File basedir = new File(rootdir, "it0037");
verifier = new Verifier(basedir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("-f pom2.xml");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("package");
verifier.assertFilePresent("target/maven-core-it0037-1.0-build2.jar");
verifier.verifyErrorFreeLog();
}

/** Test building project from outside the project directory using '-f'
        option
 */
public void test_it0038() throws Exception {
File basedir = new File(rootdir, "it0038");
verifier = new Verifier(basedir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("-f project/pom2.xml");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("package");
verifier.assertFilePresent("project/target/maven-core-it0037-1.0-build2.jar");
verifier.verifyErrorFreeLog();
}

/** Test reactor for projects that have release-pom.xml in addition to
        pom.xml. The release-pom.xml file should be chosen above pom.xml for
        these projects in the build.
 */
public void test_it0039() throws Exception {
File basedir = new File(rootdir, "it0039");
verifier = new Verifier(basedir.getAbsolutePath());
FileUtils.deleteFile(new File(basedir, "project/target/maven-core-it0039-p1-1.0.jar"));
FileUtils.deleteFile(new File(basedir, "project2/target/maven-core-it0039-p2-1.0.jar"));
List cliOptions = new ArrayList();
cliOptions.add("-r");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("package");
verifier.assertFilePresent("project/target/maven-core-it0039-p1-1.0.jar");
verifier.assertFilePresent("project2/target/maven-core-it0039-p2-1.0.jar");
verifier.verifyErrorFreeLog();
}

/** Test the use of a packaging from a plugin
 */
public void test_it0040() throws Exception {
File basedir = new File(rootdir, "it0040");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/maven-core-it0040-1.0-it.jar");
verifier.verifyErrorFreeLog();
}

/** Test the use of a new type from a plugin
 */
public void test_it0041() throws Exception {
File basedir = new File(rootdir, "it0041");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven", "maven-core-it-support", "1.2", "coreit-artifact");
verifier.executeGoal("package");
verifier.assertFilePresent("target/maven-core-it0041-1.0-SNAPSHOT.jar");
verifier.assertArtifactPresent("org.apache.maven", "maven-core-it-support", "1.2", "coreit-artifact");
verifier.assertArtifactPresent("org.apache.maven", "maven-core-it-support", "1.2", "pom");
verifier.verifyErrorFreeLog();
}

/** Test that the reactor can establish the artifact location of known projects for dependencies
 */
public void test_it0042() throws Exception {
File basedir = new File(rootdir, "it0042");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("test-component-a/target/test-component-a-0.1.jar");
verifier.assertFilePresent("test-component-b/target/test-component-b-0.1.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.war");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.war!/WEB-INF/lib/test-component-a-0.1.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.war!/WEB-INF/lib/test-component-b-0.1.jar");
verifier.assertFilePresent("test-component-c/target/my-test");
verifier.verifyErrorFreeLog();
}

/** Test for repository inheritence - ensure using the same id overrides the defaults
 */
public void test_it0043() throws Exception {
File basedir = new File(rootdir, "it0043");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/maven-it0043-1.0-SNAPSHOT.jar");
verifier.verifyErrorFreeLog();
}

/** Test --settings CLI option
 */
public void test_it0044() throws Exception {
File basedir = new File(rootdir, "it0044");
verifier = new Verifier(basedir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("--settings settings.xml");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("core-it:touch");
verifier.assertFilePresent("target/test.txt");
verifier.verifyErrorFreeLog();
}

/** Test non-reactor behavior when plugin declares "@requiresProject false"
 */
public void test_it0045() throws Exception {
File basedir = new File(rootdir, "it0045");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.plugins", "maven-core-it-plugin", "1.0", "maven-plugin");
List cliOptions = new ArrayList();
cliOptions.add("--no-plugin-registry");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("core-it:light-touch");
verifier.assertFilePresent("target/touch.txt");
verifier.assertFileNotPresent("subproject/target/touch.txt");
verifier.verifyErrorFreeLog();
}

/** Test fail-never reactor behavior. Forces an exception to be thrown in
        the first module, but checks that the second modules is built.
 */
public void test_it0046() throws Exception {
File basedir = new File(rootdir, "it0046");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.plugins", "maven-core-it-plugin", "1.0", "maven-plugin");
List cliOptions = new ArrayList();
cliOptions.add("--no-plugin-registry --fail-never");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("core-it:touch");
verifier.assertFilePresent("target/touch.txt");
verifier.assertFileNotPresent("subproject/target/touch.txt");
verifier.assertFilePresent("subproject2/target/touch.txt");
verifier.verifyErrorFreeLog();
}

/** Test the use case for having a compile time dependency be transitive: 
        when you extend a class you need its dependencies at compile time.
 */
public void test_it0047() throws Exception {
File basedir = new File(rootdir, "it0047");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("compile");
verifier.assertFilePresent("target/classes/org/apache/maven/it0047/Person.class");
verifier.verifyErrorFreeLog();
}

/** Verify that default values for mojo parameters are working (indirectly, 
        by verifying that the Surefire mojo is functioning correctly).
 */
public void test_it0048() throws Exception {
File basedir = new File(rootdir, "it0048");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("test");
verifier.assertFilePresent("target/testFileOutput.txt");
verifier.verifyErrorFreeLog();
}

/** Test parameter alias usage.
 */
public void test_it0049() throws Exception {
File basedir = new File(rootdir, "it0049");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.plugins", "maven-core-it-plugin", "1.0", "maven-plugin");
verifier.executeGoal("core-it:touch");
verifier.assertFilePresent("target/touchFile.txt");
verifier.verifyErrorFreeLog();
}

/** Test surefire inclusion/exclusions
 */
public void test_it0050() throws Exception {
File basedir = new File(rootdir, "it0050");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/testTouchFile.txt");
verifier.assertFilePresent("target/defaultTestTouchFile.txt");
verifier.verifyErrorFreeLog();
}

/** Test source attachment when -DperformRelease=true is specified.
 */
public void test_it0051() throws Exception {
File basedir = new File(rootdir, "it0051");
verifier = new Verifier(basedir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("--no-plugin-registry -DperformRelease=true");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("package");
verifier.assertFilePresent("target/maven-core-it0051-1.0.jar");
verifier.assertFilePresent("target/maven-core-it0051-1.0-sources.jar");
verifier.verifyErrorFreeLog();
}

/** Test that source attachment doesn't take place when
        -DperformRelease=true is missing.
 */
public void test_it0052() throws Exception {
File basedir = new File(rootdir, "it0052");
verifier = new Verifier(basedir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("--no-plugin-registry");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("package");
verifier.assertFilePresent("target/maven-core-it0051-1.0.jar");
verifier.assertFileNotPresent("target/maven-core-it0051-1.0-sources.jar");
verifier.verifyErrorFreeLog();
}

/** Test that attached artifacts have the same buildnumber and timestamp
        as the main artifact. This will not correctly verify until we have
        some way to pattern-match the buildnumber/timestamp...
 */
public void test_it0053() throws Exception {
File basedir = new File(rootdir, "it0053");
verifier = new Verifier(basedir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("--no-plugin-registry");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("package");
verifier.assertFilePresent("target/maven-core-it0053-1.0-SNAPSHOT.jar");
verifier.assertFileNotPresent("target/maven-core-it0053-1.0-SNAPSHOT-sources.jar");
verifier.verifyErrorFreeLog();
}

/** Test resource filtering.
 */
public void test_it0054() throws Exception {
File basedir = new File(rootdir, "it0054");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/classes/org/apache/maven/it0054/Person.class");
verifier.assertFilePresent("target/test-classes/org/apache/maven/it0054/PersonTest.class");
verifier.assertFilePresent("target/maven-core-it0054-1.0.jar");
verifier.assertFilePresent("target/maven-core-it0054-1.0.jar!/it0054.properties");
verifier.verifyErrorFreeLog();
}

/** Test that source includes/excludes with in the compiler plugin config.
        This will test excludes and testExcludes...
 */
public void test_it0055() throws Exception {
File basedir = new File(rootdir, "it0055");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("test-compile");
verifier.assertFilePresent("target/classes/org/apache/maven/it0001/Person.class");
verifier.assertFilePresent("target/test-classes/org/apache/maven/it0001/PersonTest.class");
verifier.assertFileNotPresent("target/classes/org/apache/maven/it0001/PersonTwo.class");
verifier.assertFileNotPresent("target/test-classes/org/apache/maven/it0001/PersonTwoTest.class");
verifier.verifyErrorFreeLog();
}

/** Test that multiple executions of the compile goal with different
        includes/excludes will succeed.
 */
public void test_it0056() throws Exception {
File basedir = new File(rootdir, "it0056");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("test-compile");
verifier.assertFilePresent("target/classes/org/apache/maven/it0001/Person.class");
verifier.assertFilePresent("target/classes/org/apache/maven/it0001/PersonTwo.class");
verifier.assertFilePresent("target/classes/org/apache/maven/it0001/PersonThree.class");
verifier.assertFilePresent("target/test-classes/org/apache/maven/it0001/PersonTest.class");
verifier.assertFilePresent("target/test-classes/org/apache/maven/it0001/PersonTwoTest.class");
verifier.assertFilePresent("target/test-classes/org/apache/maven/it0001/PersonThreeTest.class");
verifier.verifyErrorFreeLog();
}

/** Verify that scope == 'provided' dependencies are available to tests.
 */
public void test_it0057() throws Exception {
File basedir = new File(rootdir, "it0057");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/classes/org/apache/maven/it0001/Person.class");
verifier.assertFilePresent("target/test-classes/org/apache/maven/it0001/PersonTest.class");
verifier.assertFilePresent("target/maven-core-it0057-1.0.jar");
verifier.assertFilePresent("target/maven-core-it0057-1.0.jar!/it0001.properties");
verifier.verifyErrorFreeLog();
}

/** Verify that profiles from settings.xml do not pollute module lists
        across projects in a reactorized build.
 */
public void test_it0058() throws Exception {
File basedir = new File(rootdir, "it0058");
verifier = new Verifier(basedir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("--settings ./settings.xml");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("package");
verifier.assertFilePresent("subproject/target/maven-core-it0058-subproject-1.0.jar");
verifier.verifyErrorFreeLog();
}

/** Verify that maven-1 POMs will be ignored but not stop the resolution
        process.
 */
public void test_it0059() throws Exception {
File basedir = new File(rootdir, "it0059");
verifier = new Verifier(basedir.getAbsolutePath());
Properties verifierProperties = new Properties();
verifierProperties.put("failOnErrorOutput", "false");
verifier.setVerifierProperties(verifierProperties);
verifier.executeGoal("package");
verifier.assertFilePresent("target/maven-core-it0059-1.0.jar");
// don't verify error free log
}

/** Test aggregation of list configuration items when using
        'combine.children=append' attribute. Specifically, merge the list of
        excludes for the testCompile mojo.
 */
public void test_it0060() throws Exception {
File basedir = new File(rootdir, "it0060");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("test");
verifier.assertFilePresent("subproject/target/classes/org/apache/maven/it0001/Person.class");
verifier.assertFilePresent("subproject/target/test-classes/org/apache/maven/it0001/PersonTest.class");
verifier.assertFileNotPresent("subproject/target/test-classes/org/apache/maven/it0001/PersonTwoTest.class");
verifier.assertFileNotPresent("subproject/target/test-classes/org/apache/maven/it0001/PersonThreeTest.class");
verifier.verifyErrorFreeLog();
}

/** Verify that deployment of artifacts to a legacy-layout repository
        results in a groupId directory of 'the.full.group.id' instead of
        'the/full/group/id'.
 */
public void test_it0061() throws Exception {
File basedir = new File(rootdir, "it0061");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("deploy");
verifier.assertFilePresent("target/test-repo/org.apache.maven.it/jars/maven-core-it0061-1.0.jar");
verifier.assertFilePresent("target/test-repo/org.apache.maven.it/poms/maven-core-it0061-1.0.pom");
verifier.verifyErrorFreeLog();
}

/** Test that a deployment of a snapshot falls back to a non-snapshot repository if no snapshot repository is
        specified.
 */
public void test_it0062() throws Exception {
File basedir = new File(rootdir, "it0062");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven", "maven-core-it0062-SNAPSHOT", "1.0", "jar");
verifier.executeGoal("deploy");
verifier.assertFilePresent("target/classes/org/apache/maven/it0062/Person.class");
verifier.assertFilePresent("target/maven-core-it0062-1.0-SNAPSHOT.jar");
verifier.verifyErrorFreeLog();
}

/** Test the use of a system scoped dependency to tools.jar.
 */
public void test_it0063() throws Exception {
File basedir = new File(rootdir, "it0063");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/classes/org/apache/maven/it0001/Person.class");
verifier.assertFilePresent("target/test-classes/org/apache/maven/it0001/PersonTest.class");
verifier.assertFilePresent("target/maven-core-it0063-1.0.jar");
verifier.assertFilePresent("target/maven-core-it0063-1.0.jar!/it0001.properties");
verifier.verifyErrorFreeLog();
}

/** Test the use of a mojo that uses setters instead of private fields
        for the population of configuration values.
 */
public void test_it0064() throws Exception {
File basedir = new File(rootdir, "it0064");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.plugins", "maven-core-it-plugin", "1.0", "maven-plugin");
verifier.executeGoal("core-it:setter-touch");
verifier.assertFilePresent("target/fooValue");
verifier.assertFilePresent("target/barValue.baz");
verifier.verifyErrorFreeLog();
}

/** Test that the basedir of the parent is set correctly.
 */
public void test_it0065() throws Exception {
File basedir = new File(rootdir, "it0065");
verifier = new Verifier(basedir.getAbsolutePath());
FileUtils.deleteFile(new File(basedir, "parent-basedir"));
verifier.executeGoal("install");
verifier.assertFilePresent("subproject/target/child-basedir");
verifier.assertFilePresent("parent-basedir");
verifier.verifyErrorFreeLog();
}

/** Test that nonstandard POM files will be installed correctly.
 */
public void test_it0066() throws Exception {
File basedir = new File(rootdir, "it0066");
verifier = new Verifier(basedir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("-f other-pom.xml");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("install");
verifier.assertFilePresent("");
verifier.verifyErrorFreeLog();
}

/** Test activation of a profile from the command line.
 */
public void test_it0067() throws Exception {
File basedir = new File(rootdir, "it0067");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven", "maven-core-it-support", "1.0", "jar");
List cliOptions = new ArrayList();
cliOptions.add("-P test-profile");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("compile");
verifier.assertFilePresent("target/classes/org/apache/maven/it0021/Person.class");
verifier.verifyErrorFreeLog();
}

/** Test repository accumulation.
 */
public void test_it0068() throws Exception {
File basedir = new File(rootdir, "it0068");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.codehaus.modello", "modello-core", "1.0-alpha-3", "jar");
Properties verifierProperties = new Properties();
verifierProperties.put("failOnErrorOutput", "false");
verifier.setVerifierProperties(verifierProperties);
verifier.executeGoal("generate-sources");
verifier.assertFilePresent("target/generated-sources/modello/org/apache/maven/settings/Settings.java");
// don't verify error free log
}

/** Test offline mode.
 */
public void test_it0069() throws Exception {
File basedir = new File(rootdir, "it0069");
verifier = new Verifier(basedir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("-o");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("compile");
verifier.assertFilePresent("target/classes/org/apache/maven/it0069/ClassworldBasedThing.class");
verifier.verifyErrorFreeLog();
}

/** Test a RAR generation.
 */
public void test_it0070() throws Exception {
File basedir = new File(rootdir, "it0070");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/maven-core-it0070-1.0.rar");
verifier.assertFilePresent("target/maven-core-it0070-1.0.rar!/META-INF/ra.xml");
verifier.assertFilePresent("target/maven-core-it0070-1.0.rar!/SomeResource.txt");
verifier.verifyErrorFreeLog();
}

/** Verifies that dotted property references work within plugin
        configurations.
 */
public void test_it0071() throws Exception {
File basedir = new File(rootdir, "it0071");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.plugins", "maven-core-it-plugin", "1.0", "maven-plugin");
verifier.executeGoal("core-it:touch");
verifier.assertFilePresent("target/foo2");
verifier.verifyErrorFreeLog();
}

/** Verifies that property references with dotted notation work within
        POM interpolation.
 */
public void test_it0072() throws Exception {
File basedir = new File(rootdir, "it0072");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/maven-core-it0072-1.0-SNAPSHOT.jar");
verifier.verifyErrorFreeLog();
}

/** Tests context passing between mojos in the same plugin.
 */
public void test_it0073() throws Exception {
File basedir = new File(rootdir, "it0073");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.plugins", "maven-core-it-plugin", "1.0", "maven-plugin");
List goals = Arrays.asList(new String[] {"core-it:throw", "core-it:catch"});
verifier.executeGoals(goals);
verifier.assertFilePresent("target/thrown-value");
verifier.verifyErrorFreeLog();
}

/** Test that plugin-level configuration instances are not nullified by
        execution-level configuration instances.
 */
public void test_it0074() throws Exception {
File basedir = new File(rootdir, "it0074");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("eclipse:eclipse");
verifier.assertFilePresent(".classpath");
verifier.verifyErrorFreeLog();
}

/** Verify that direct invocation of a mojo from the command line still
        results in the processing of modules included via profiles.
 */
public void test_it0075() throws Exception {
File basedir = new File(rootdir, "it0075");
verifier = new Verifier(basedir.getAbsolutePath());
FileUtils.deleteFile(new File(basedir, "sub1/target/maven-core-it0075-sub1-1.0.jar"));
FileUtils.deleteFile(new File(basedir, "sub2/target/maven-core-it0075-sub2-1.0.jar"));
List cliOptions = new ArrayList();
cliOptions.add("-Dactivate=anything");
verifier.setCliOptions(cliOptions);
List goals = Arrays.asList(new String[] {"help:active-profiles", "package", "eclipse:eclipse", "clean:clean"});
verifier.executeGoals(goals);
verifier.assertFileNotPresent("sub1/target/maven-core-it0075-sub1-1.0.jar");
verifier.assertFileNotPresent("sub2/target/maven-core-it0075-sub2-1.0.jar");
verifier.verifyErrorFreeLog();
}

/** Test that plugins in pluginManagement aren't included in the build
        unless they are referenced by groupId/artifactId within the plugins
        section of a pom.
 */
public void test_it0076() throws Exception {
File basedir = new File(rootdir, "it0076");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("install");
verifier.verifyErrorFreeLog();
}

/** Test test jar attachment.
 */
public void test_it0077() throws Exception {
File basedir = new File(rootdir, "it0077");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.it", "maven-it0077-sub1", "1.0", "test-jar");
List cliOptions = new ArrayList();
cliOptions.add("--settings settings.xml");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("install");
verifier.assertArtifactPresent("org.apache.maven.it", "maven-it0077-sub1", "1.0", "test-jar");
verifier.verifyErrorFreeLog();
}

/** Test that configuration for maven-compiler-plugin is injected from
        PluginManagement section even when it's not explicitly defined in the
        plugins section.
 */
public void test_it0078() throws Exception {
File basedir = new File(rootdir, "it0078");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("compile");
verifier.assertFileNotPresent("target/classes/Test.class");
verifier.verifyErrorFreeLog();
}

/** Test that source attachments have the same build number as the main
        artifact when deployed.
 */
public void test_it0079() throws Exception {
File basedir = new File(rootdir, "it0079");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("deploy");
verifier.assertFilePresent("target/test-repo/org/apache/maven/it/maven-core-it0079/SNAPSHOT/maven-core-it0079-*-1.jar");
verifier.assertFilePresent("target/test-repo/org/apache/maven/it/maven-core-it0079/SNAPSHOT/maven-core-it0079-*-1-sources.jar");
verifier.verifyErrorFreeLog();
}

/** Test that depending on a WAR doesn't also get its dependencies
        transitively.
 */
public void test_it0080() throws Exception {
File basedir = new File(rootdir, "it0080");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("test-component-a/target/test-component-a-0.1.jar");
verifier.assertFilePresent("test-component-b/target/test-component-b-0.1.war");
verifier.assertFilePresent("test-component-b/target/test-component-b-0.1.war!/WEB-INF/lib/test-component-a-0.1.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.ear");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.ear!/test-component-b-0.1.war");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1/test-component-b-0.1.war");
verifier.assertFileNotPresent("test-component-c/target/test-component-c-0.1/test-component-a-0.1.jar");
verifier.verifyErrorFreeLog();
}

/** Test per-plugin dependencies.
 */
public void test_it0081() throws Exception {
File basedir = new File(rootdir, "it0081");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("install");
verifier.assertFilePresent("test-component-c/target/org.apache.maven.wagon.providers.ftp.FtpWagon");
verifier.verifyErrorFreeLog();
}

/** Test that the reactor can establish the artifact location of known projects for dependencies
        using process-sources to see that it works even when they aren't compiled
 */
public void test_it0082() throws Exception {
File basedir = new File(rootdir, "it0082");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("process-sources");
verifier.assertFilePresent("test-component-c/target/my-test");
verifier.verifyErrorFreeLog();
}

/** Verify that overriding a compile time dependency as provided in a WAR ensures it is not included.
 */
public void test_it0083() throws Exception {
File basedir = new File(rootdir, "it0083");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("test-component-a/target/test-component-a-0.1.jar");
verifier.assertFilePresent("test-component-b/target/test-component-b-0.1.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.war");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.war!/WEB-INF/lib/test-component-b-0.1.jar");
verifier.assertFileNotPresent("test-component-c/target/test-component-c-0.1/WEB-INF/lib/test-component-a-0.1.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1/WEB-INF/lib/test-component-b-0.1.jar");
verifier.verifyErrorFreeLog();
}

/** Verify that the collector selecting a particular version gets the correct subtree
 */
public void test_it0084() throws Exception {
File basedir = new File(rootdir, "it0084");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("test-component-a/target/test-component-a-0.1.jar");
verifier.assertFilePresent("test-component-b/target/test-component-b-0.1.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.war");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.war!/WEB-INF/lib/test-component-a-0.1.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.war!/WEB-INF/lib/test-component-b-0.1.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.war!/WEB-INF/lib/maven-core-it-support-1.4.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.war!/WEB-INF/lib/commons-io-1.0.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1/WEB-INF/lib/test-component-a-0.1.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1/WEB-INF/lib/test-component-b-0.1.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1/WEB-INF/lib/maven-core-it-support-1.4.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1/WEB-INF/lib/commons-io-1.0.jar");
verifier.assertFileNotPresent("test-component-c/target/test-component-c-0.1/WEB-INF/lib/commons-lang-1.0.jar");
verifier.verifyErrorFreeLog();
}

/** Verify that system-scoped dependencies get resolved with system scope
        when they are resolved transitively via another (non-system)
        dependency. Inherited scope should not apply in the case of
        system-scoped dependencies, no matter where they are.
 */
public void test_it0085() throws Exception {
File basedir = new File(rootdir, "it0085");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFileNotPresent("war/target/it0085-war-1.0/WEB-INF/lib/pom.xml");
verifier.assertFileNotPresent("war/target/it0085-war-1.0/WEB-INF/lib/it0085-dep-1.0.jar");
verifier.assertFilePresent("war/target/it0085-war-1.0/WEB-INF/lib/junit-3.8.1.jar");
verifier.verifyErrorFreeLog();
}

/** Verify that a plugin dependency class can be loaded from both the plugin classloader and the
        context classloader available to the plugin.
 */
public void test_it0086() throws Exception {
File basedir = new File(rootdir, "it0086");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.plugins", "maven-core-it-plugin", "1.0", "maven-plugin");
verifier.executeGoal("validate");
verifier.verifyErrorFreeLog();
}

/** Verify that a project-level plugin dependency class can be loaded from both the plugin classloader
        and the context classloader available to the plugin.
 */
public void test_it0087() throws Exception {
File basedir = new File(rootdir, "it0087");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.plugins", "maven-core-it-plugin", "1.0", "maven-plugin");
verifier.executeGoal("validate");
verifier.verifyErrorFreeLog();
}

/** Test path translation.
 */
public void test_it0088() throws Exception {
File basedir = new File(rootdir, "it0088");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("test");
verifier.assertFilePresent("target/classes/test.properties");
verifier.assertFilePresent("target/mojo-generated.properties");
verifier.verifyErrorFreeLog();
}

/** Test that Checkstyle PackageNamesLoader.loadModuleFactory(..) method will complete as-is with
        the context classloader available to the plugin.
        */
public void test_it0089() throws Exception {
File basedir = new File(rootdir, "it0089");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.plugins", "maven-core-it-plugin", "1.0", "maven-plugin");
verifier.executeGoal("install");
verifier.assertFilePresent("project/target/output.txt");
verifier.verifyErrorFreeLog();
}

/** Test that ensures that envars are interpolated correctly into plugin
        configurations.
 */
public void test_it0090() throws Exception {
File basedir = new File(rootdir, "it0090");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("test");
verifier.assertFilePresent("target/mojo-generated.properties");
verifier.verifyErrorFreeLog();
}

/** Test that currently demonstrates that properties are not correctly
        interpolated into other areas in the POM. This may strictly be a boolean
        problem: I captured the problem as it was reported.
 */
public void test_it0091() throws Exception {
File basedir = new File(rootdir, "it0091");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("test");
verifier.assertFilePresent("target/classes/test.properties");
verifier.verifyErrorFreeLog();
}

/** Test that legacy repositories with legacy snapshots download correctly.
 */
public void test_it0092() throws Exception {
File basedir = new File(rootdir, "it0092");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven", "maven-core-it-support", "1.0-SNAPSHOT", "jar");
verifier.executeGoal("compile");
verifier.assertArtifactPresent("org.apache.maven", "maven-core-it-support", "1.0-SNAPSHOT", "jar");
verifier.verifyErrorFreeLog();
}

/** Test classloading issues with mojos after 2.0 (MNG-1898).
 */
public void test_it0094() throws Exception {
File basedir = new File(rootdir, "it0094");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("install");
verifier.verifyErrorFreeLog();
}

/** Test URL calculation when modules are in sibling dirs of parent. (MNG-2006)
 */
public void test_it0095() throws Exception {
File basedir = new File(rootdir, "it0095");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("integration-test");
verifier.verifyErrorFreeLog();
}

/** Test that plugin executions from >1 step of inheritance don't run multiple times. 
 */
public void test_it0096() throws Exception {
File basedir = new File(rootdir, "it0096");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.verifyErrorFreeLog();
}

/** Test that the implied relative path for the parent POM works, even two
        levels deep.
 */
public void test_it0097() throws Exception {
File basedir = new File(rootdir, "it0097");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("project/project-level2/project-level3/target/it0097.txt");
verifier.assertFilePresent("project/project-sibling-level2/target/it0097.txt");
verifier.verifyErrorFreeLog();
}

/** Test that quoted system properties are processed correctly. [MNG-1415]
 */
public void test_it0098() throws Exception {
File basedir = new File(rootdir, "it0098");
verifier = new Verifier(basedir.getAbsolutePath());
FileUtils.deleteDirectory(new File(basedir, "${basedir}/test project"));
List cliOptions = new ArrayList();
cliOptions.add("-Dtest.property=\"Test Property\"");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("test");
verifier.verifyErrorFreeLog();
}

/** Test that parent-POMs cached during a build are available as parents
        to other POMs in the multimodule build. [MNG-2130]
 */
public void test_it0099() throws Exception {
File basedir = new File(rootdir, "it0099");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.it0099", "maven-it0099-parent", "1", "pom");
verifier.executeGoal("package");
verifier.verifyErrorFreeLog();
}

/** Test that ${parent.artifactId} resolves correctly. [MNG-2124]
 */
public void test_it0100() throws Exception {
File basedir = new File(rootdir, "it0100");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("verify");
verifier.verifyErrorFreeLog();
}

/** Test that properties defined in an active profile in the user's
        settings are available for interpolation of systemPath in a dependency.
        [MNG-2052]
 */
public void test_it0101() throws Exception {
File basedir = new File(rootdir, "it0101");
verifier = new Verifier(basedir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("--settings settings.xml");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("compile");
verifier.verifyErrorFreeLog();
}

/** Test that <activeByDefault/> calculations for profile activation only
        use profiles defined in the POM. [MNG-2136]
 */
public void test_it0102() throws Exception {
File basedir = new File(rootdir, "it0102");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("verify");
verifier.verifyErrorFreeLog();
}

/** Verify that multimodule builds where one project references another as
        a parent can build, even if that parent is not correctly referenced by
        <relativePath/> and is not in the local repository. [MNG-2196]
 */
public void test_it0103() throws Exception {
File basedir = new File(rootdir, "it0103");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("package");
verifier.verifyErrorFreeLog();
}

/** Verify that plugin configurations are resolved correctly, particularly
        when they contain ${project.build.directory} in the string value of a 
        Map.Entry.
 */
public void test_it0104() throws Exception {
File basedir = new File(rootdir, "it0104");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("test");
verifier.verifyErrorFreeLog();
}

/** MRESOURCES-18
 */
public void test_it0105() throws Exception {
File basedir = new File(rootdir, "it0105");
verifier = new Verifier(basedir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("-Dparam=PARAM");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("test");
verifier.assertFilePresent("target/classes/test.properties");
verifier.verifyErrorFreeLog();
}

/** When a project has modules and its parent is not preinstalled [MNG-2318]
 */
public void test_it0106() throws Exception {
File basedir = new File(rootdir, "it0106");
verifier = new Verifier(basedir.getAbsolutePath());
verifier.executeGoal("clean");
verifier.verifyErrorFreeLog();
}

/** Verify that default implementation of an implementation for a complex object works as 
        expected [MNG-2293] */
public void test_it0107() throws Exception {
File basedir = new File(rootdir, "it0107");
verifier = new Verifier(basedir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("-X");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("core-it:param-implementation");
verifier.verifyErrorFreeLog();
}

    public void tearDown() throws VerificationException 
    {
        verifier.resetStreams();                                                                                                                                                                                                            
            
    }                                                                                                                                                                                                                                       
}

