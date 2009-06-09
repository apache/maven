package org.apache.maven.project;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class MavenProjectDynamismTest
    extends PlexusTestCase
{

    private DefaultMavenProjectBuilder projectBuilder;

    public void setUp()
        throws Exception
    {
        super.setUp();

        projectBuilder = (DefaultMavenProjectBuilder) lookup( MavenProjectBuilder.class.getName() );
    }

    public void testBuildSectionGroupIdInterpolation()
        throws IOException, XmlPullParserException, URISyntaxException, ProjectBuildingException,
        ModelInterpolationException
    {
        MavenProject project = buildProject( "pom-interp.xml" );

        projectBuilder.calculateConcreteState( project, new DefaultProjectBuilderConfiguration() );

        String basepath = new File( project.getBasedir(), project.getGroupId() ).getAbsolutePath();

        Build build = project.getBuild();

        assertTrue( build.getSourceDirectory() + " doesn't start with base-path: " + basepath,
                    build.getSourceDirectory().startsWith( basepath ) );
        assertTrue( build.getTestSourceDirectory() + " doesn't start with base-path: " + basepath,
                    build.getTestSourceDirectory().startsWith( basepath ) );
        
        // TODO: MNG-3731
//        assertTrue( build.getScriptSourceDirectory() + " doesn't start with base-path: " + basepath,
//                    build.getScriptSourceDirectory().startsWith( basepath ) );

        List plugins = build.getPlugins();
        assertNotNull( plugins );
        assertEquals( 1, plugins.size() );

        Plugin plugin = (Plugin) plugins.get( 0 );
        assertEquals( "my-plugin", plugin.getArtifactId() );

        Xpp3Dom conf = (Xpp3Dom) plugin.getConfiguration();
        assertNotNull( conf );

        Xpp3Dom[] children = conf.getChildren();
        assertEquals( 3, children.length );

        for ( int i = 0; i < children.length; i++ )
        {
            assertEquals( "Configuration parameter: " + children[i].getName()
                + " should have a an interpolated POM groupId as its value.", children[i].getValue(),
                          project.getGroupId() );
        }

        project.getProperties().setProperty( "foo", "bar" );

        projectBuilder.restoreDynamicState( project, new DefaultProjectBuilderConfiguration() );

        String projectGidExpr = "${project.groupId}";
        String pomGidExpr = "${pom.groupId}";
        String nakedGidExpr = "${groupId}";

        build = project.getBuild();

        assertTrue( build.getSourceDirectory() + " didn't start with: " + projectGidExpr,
                    build.getSourceDirectory().startsWith( projectGidExpr ) );
        assertTrue( build.getTestSourceDirectory() + " didn't start with: " + pomGidExpr,
                    build.getTestSourceDirectory().startsWith( pomGidExpr ) );
        assertTrue( build.getScriptSourceDirectory() + " didn't start with: " + nakedGidExpr,
                    build.getScriptSourceDirectory().startsWith( nakedGidExpr ) );

        plugins = build.getPlugins();
        assertNotNull( plugins );
        assertEquals( 1, plugins.size() );

        plugin = (Plugin) plugins.get( 0 );
        assertEquals( "my-plugin", plugin.getArtifactId() );

        conf = (Xpp3Dom) plugin.getConfiguration();
        assertNotNull( conf );

        children = conf.getChildren();
        assertEquals( 3, children.length );

        assertEquals( "Configuration parameter: " + children[0].getName() + " should have " + projectGidExpr
            + " as its value.", children[0].getValue(), projectGidExpr );

        assertEquals( "Configuration parameter: " + children[1].getName() + " should have " + pomGidExpr
            + " as its value.", children[1].getValue(), pomGidExpr );

        assertEquals( "Configuration parameter: " + children[2].getName() + " should have " + nakedGidExpr
            + " as its value.", children[2].getValue(), nakedGidExpr );
    }

    public void testRoundTrip()
        throws IOException, XmlPullParserException, URISyntaxException, ModelInterpolationException,
        ProjectBuildingException
    {
        MavenProject project = buildProject( "pom.xml" );
        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration();
        projectBuilder.calculateConcreteState( project, config );

        File baseDir = project.getBasedir();
        File buildDir = new File( baseDir, "target" );

        String basedirExpr = "${pom.basedir}";
        String buildDirExpr = "${pom.build.directory}";

        assertTrue( project.isConcrete() );

        Build build = project.getBuild();

        assertEquals( "Concrete source directory should be absolute.",
                      new File( baseDir, "/src/main/java" ).getAbsolutePath(),
                      new File( build.getSourceDirectory() ).getAbsolutePath() );

        assertEquals( "Concrete test-source directory should be absolute.",
                      new File( baseDir, "/src/test/java" ).getAbsolutePath(),
                      new File( build.getTestSourceDirectory() ).getAbsolutePath() );

        assertEquals( "Concrete script-source directory should be absolute.",
                      new File( baseDir, "/src/main/scripts" ).getAbsolutePath(),
                      new File( build.getScriptSourceDirectory() ).getAbsolutePath() );

        List compileSourceRoots = project.getCompileSourceRoots();

        assertNotNull( "Concrete compile-source roots should not be null.", compileSourceRoots );

        assertEquals( "Concrete compile-source roots should contain one entry.", 1, compileSourceRoots.size() );

        assertEquals( "Concrete compile-source roots should contain interpolated source-directory value.",
                      new File( baseDir, "/src/main/java" ).getAbsolutePath(),
                      new File( (String) compileSourceRoots.get( 0 ) ).getAbsolutePath() );

        List testCompileSourceRoots = project.getTestCompileSourceRoots();

        assertNotNull( "Concrete test-compile-source roots should not be null.", testCompileSourceRoots );

        assertEquals( "Concrete test-compile-source roots should contain one entry.", 1, testCompileSourceRoots.size() );

        assertEquals( "Concrete test-compile-source roots should contain interpolated test-source-directory value.",
                      new File( baseDir, "/src/test/java" ).getAbsolutePath(),
                      new File( (String) testCompileSourceRoots.get( 0 ) ).getAbsolutePath() );

        List scriptSourceRoots = project.getScriptSourceRoots();

        assertNotNull( "Concrete script-source roots should not be null.", scriptSourceRoots );

        assertEquals( "Concrete script-source roots should contain one entry.", 1, scriptSourceRoots.size() );

        assertEquals( "Concrete script-source roots should contain interpolated script-source-directory value.",
                      new File( baseDir, "/src/main/scripts" ).getAbsolutePath(),
                      new File( (String) scriptSourceRoots.get( 0 ) ).getAbsolutePath() );

        List resources = build.getResources();

        assertNotNull( "Concrete resources should not be null.", resources );

        assertEquals( "Concrete resources should contain one entry.", 1, resources.size() );

        assertEquals( "Concrete resource should contain absolute path.",
                      new File( buildDir, "generated-resources/plexus" ).getAbsolutePath(),
                      new File( ( (Resource) resources.get( 0 ) ).getDirectory() ).getAbsolutePath() );

        List filters = build.getFilters();

        assertNotNull( "Concrete filters should not be null.", filters );

        assertEquals( "Concrete filters should contain one entry.", 1, filters.size() );

        assertEquals( "Concrete filter entry should contain absolute path.",
                      new File( buildDir, "/generated-filters.properties" ).getAbsolutePath(),
                      new File( (String) filters.get( 0 ) ).getAbsolutePath() );

        assertEquals( "Concrete output-directory should be absolute.",
                      new File( buildDir, "/classes" ).getAbsolutePath(),
                      new File( build.getOutputDirectory() ).getAbsolutePath() );

        assertEquals( "Concrete test-output-directory should be absolute.",
                      new File( buildDir, "/test-classes" ).getAbsolutePath(),
                      new File( build.getTestOutputDirectory() ).getAbsolutePath() );

        assertEquals( "Concrete build directory should be absolute.", new File( baseDir, "target" ).getAbsolutePath(),
                      new File( build.getDirectory() ).getAbsolutePath() );

        // Next, we have to change something to ensure the project is restored to its dynamic state.
        project.getProperties().setProperty( "restoreTrigger", "true" );

        // --------------------------------------------------------------------
        // NOW, RESTORE THE DYNAMIC STATE FOR THE BUILD SECTION AND
        // ASSOCIATED DIRECTORIES ATTACHED TO THE PROJECT INSTANCE.
        // --------------------------------------------------------------------

        projectBuilder.restoreDynamicState( project, config );

        assertFalse( project.isConcrete() );

        build = project.getBuild();

        assertEquals( "Restored source directory should be expressed in terms of the basedir.\nWas: "
            + build.getSourceDirectory() + "\nShould be: " + basedirExpr + "/src/main/java\n", basedirExpr
            + "/src/main/java", build.getSourceDirectory() );

        assertEquals( "Restored test-source directory should be expressed in terms of the basedir.", basedirExpr
            + "/src/test/java", build.getTestSourceDirectory() );

        assertEquals( "Restored script-source directory should be expressed in terms of the basedir.", basedirExpr
            + "/src/main/scripts", build.getScriptSourceDirectory() );

        compileSourceRoots = project.getCompileSourceRoots();

        assertNotNull( "Restored compile-source roots should not be null.", compileSourceRoots );

        assertEquals( "Restored compile-source roots should contain one entry.", 1, compileSourceRoots.size() );

        assertEquals( "Restored compile-source roots should contain uninterpolated source-directory value.",
                      "${pom.basedir}/src/main/java", compileSourceRoots.get( 0 ) );

        testCompileSourceRoots = project.getTestCompileSourceRoots();

        assertNotNull( "Restored test-compile-source roots should not be null.", testCompileSourceRoots );

        assertEquals( "Restored test-compile-source roots should contain one entry.", 1, testCompileSourceRoots.size() );

        assertEquals( "Restored test-compile-source roots should contain uninterpolated test-source-directory value.",
                      "${pom.basedir}/src/test/java", testCompileSourceRoots.get( 0 ) );

        scriptSourceRoots = project.getScriptSourceRoots();

        assertNotNull( "Restored script-source roots should not be null.", scriptSourceRoots );

        assertEquals( "Restored script-source roots should contain one entry.", 1, scriptSourceRoots.size() );

        assertEquals( "Restored script-source roots should contain uninterpolated script-source-directory value.",
                      "${pom.basedir}/src/main/scripts", scriptSourceRoots.get( 0 ) );

        resources = build.getResources();

        assertNotNull( "Restored resources should not be null.", resources );

        assertEquals( "Restored resources should contain one entry.", 1, resources.size() );

        assertEquals( "Restored resource should contain uninterpolated reference to build directory.", buildDirExpr
            + "/generated-resources/plexus", ( (Resource) resources.get( 0 ) ).getDirectory() );

        filters = build.getFilters();

        assertNotNull( "Restored filters should not be null.", filters );

        assertEquals( "Restored filters should contain one entry.", 1, filters.size() );

        assertEquals( "Restored filter entry should contain uninterpolated reference to build directory.", buildDirExpr
            + "/generated-filters.properties", filters.get( 0 ) );

        assertEquals( "Restored output-directory should be expressed in terms of the build-directory.", buildDirExpr
            + "/classes", build.getOutputDirectory() );

        assertEquals( "Restored test-output-directory should be expressed in terms of the build-directory.",
                      buildDirExpr + "/test-classes", build.getTestOutputDirectory() );

        assertEquals( "Restored build directory should be relative.", "target", build.getDirectory() );
    }

    public void testShouldPreserveAddedResourceInRestoredState()
        throws IOException, XmlPullParserException, URISyntaxException, ProjectBuildingException,
        ModelInterpolationException
    {
        MavenProject project = buildProject( "pom.xml" );

        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration();

        projectBuilder.calculateConcreteState( project, config );

        Build build = project.getBuild();

        Resource r = new Resource();
        r.setDirectory( "myDir" );

        build.addResource( r );

        List resources = build.getResources();
        assertNotNull( "Concrete resources should not be null.", resources );
        assertEquals( "Concrete resources should contain two entries.", 2, resources.size() );
        assertResourcePresent( "concrete resources",
                               new File( build.getDirectory(), "generated-resources/plexus" ).getAbsolutePath(),
                               resources );
        assertResourcePresent( "concrete resources", "myDir", resources );

        // Next, we have to change something to ensure the project is restored to its dynamic state.
        project.getProperties().setProperty( "restoreTrigger", "true" );

        projectBuilder.restoreDynamicState( project, config );

        build = project.getBuild();

        resources = build.getResources();
        assertNotNull( "Restored resources should not be null.", resources );
        assertEquals( "Restored resources should contain two entries.", 2, resources.size() );
        assertResourcePresent( "restored resources", "${pom.build.directory}/generated-resources/plexus", resources );
        assertResourcePresent( "restored resources", "myDir", resources );
    }

    public void testShouldPreserveAddedFilterInRestoredState()
        throws IOException, XmlPullParserException, URISyntaxException, ProjectBuildingException,
        ModelInterpolationException
    {
        MavenProject project = buildProject( "pom.xml" );

        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration();
        projectBuilder.calculateConcreteState( project, config );

        Build build = project.getBuild();

        build.addFilter( "myDir/filters.properties" );

        List filters = build.getFilters();
        assertNotNull( "Concrete filters should not be null.", filters );
        assertEquals( "Concrete filters should contain two entries.", 2, filters.size() );
        assertFilterPresent( "concrete filters",
                             new File( build.getDirectory(), "generated-filters.properties" ).getAbsolutePath(),
                             filters );

        assertFilterPresent( "concrete filters", "myDir/filters.properties", filters );

        // Next, we have to change something to ensure the project is restored to its dynamic state.
        project.getProperties().setProperty( "restoreTrigger", "true" );

        projectBuilder.restoreDynamicState( project, config );

        build = project.getBuild();

        filters = build.getFilters();
        assertNotNull( "Restored filters should not be null.", filters );
        assertEquals( "Restored filters should contain two entries.", 2, filters.size() );
        assertFilterPresent( "restored filters", "${pom.build.directory}/generated-filters.properties", filters );
        assertFilterPresent( "restored filters", "myDir/filters.properties", filters );
    }

    public void testShouldIncorporateChangedBuildDirectoryViaExpressionsOnNextConcreteCalculation()
        throws IOException, XmlPullParserException, URISyntaxException, ProjectBuildingException,
        ModelInterpolationException
    {
        MavenProject project = buildProject( "pom.xml" );

        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration();
        projectBuilder.calculateConcreteState( project, config );

        Build build = project.getBuild();

        assertEquals( "First concrete build directory should be absolute and point to target dir.",
                      new File( project.getBasedir(), "target" ).getAbsolutePath(), build.getDirectory() );
        assertEquals( "First concrete build output-directory should be absolute and point to target/classes dir.",
                      new File( project.getBasedir(), "target/classes" ).getAbsolutePath(),
                      new File( build.getOutputDirectory() ).getAbsolutePath() );

        build.setDirectory( "target2" );

        assertEquals( "AFTER CHANGING BUILD DIRECTORY, build directory should be relative and point to target2 dir.",
                      "target2", build.getDirectory() );
        assertEquals(
                      "AFTER CHANGING BUILD DIRECTORY, build output-directory should be absolute and still point to target/classes dir.",
                      new File( project.getBasedir(), "target/classes" ).getAbsolutePath(),
                      new File( build.getOutputDirectory() ).getAbsolutePath() );

        projectBuilder.restoreDynamicState( project, config );
        projectBuilder.calculateConcreteState( project, config );

        build = project.getBuild();

        assertEquals( "Second concrete build directory should be absolute and point to target2 dir.",
                      new File( project.getBasedir(), "target2" ).getAbsolutePath(),
                      new File( build.getDirectory() ).getAbsolutePath() );
        assertEquals( "Second concrete build output-directory should be absolute and point to target2/classes dir.",
                      new File( project.getBasedir(), "target2/classes" ).getAbsolutePath(),
                      new File( build.getOutputDirectory() ).getAbsolutePath() );
    }

    public void testShouldPreserveInitialValuesForPropertiesReferencingBuildPaths()
        throws IOException, XmlPullParserException, URISyntaxException, ProjectBuildingException,
        ModelInterpolationException
    {
        MavenProject project = buildProject( "pom.xml" );

        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration();
        projectBuilder.calculateConcreteState( project, config );

        project.getBuild().setDirectory( "target2" );

        String originalValue = project.getProperties().getProperty( "myProperty" );

        projectBuilder.restoreDynamicState( project, config );
        projectBuilder.calculateConcreteState( project, config );

        assertEquals( "After resetting build-directory and going through a recalculation phase for the project, "
            + "property value for 'myProperty' should STILL be the absolute initial build directory.", originalValue,
                      project.getProperties().getProperty( "myProperty" ) );
    }

    public void testShouldAlignCompileSourceRootsInConcreteState()
        throws IOException, XmlPullParserException, URISyntaxException, ProjectBuildingException,
        ModelInterpolationException
    {
        MavenProject project = buildProject( "pom-relative.xml" );

        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration();
        projectBuilder.calculateConcreteState( project, config );

        List compileSourceRoots = project.getCompileSourceRoots();
        assertNotNull( "First concrete state compile-source roots should not be null.", compileSourceRoots );
        assertEquals( "First concrete state should contain one compile-source root.", 1, compileSourceRoots.size() );
        assertEquals( "First concrete state should have an absolute path for compile-source root.",
                      new File( project.getBasedir(), "src/main/java" ).getAbsolutePath(), compileSourceRoots.get( 0 ) );

        String newSourceRoot =
            new File( project.getBuild().getDirectory(), "generated-sources/modello" ).getAbsolutePath();

        project.addCompileSourceRoot( newSourceRoot );

        // Next, we have to change something to ensure the project is restored to its dynamic state.
        project.getProperties().setProperty( "restoreTrigger", "true" );

        projectBuilder.restoreDynamicState( project, config );

        compileSourceRoots = project.getCompileSourceRoots();
        assertNotNull( "Restored dynamic state compile-source roots should not be null.", compileSourceRoots );
        assertEquals( "Restored dynamic state should contain two compile-source roots.", 2, compileSourceRoots.size() );
        assertEquals( "Restored dynamic state should have a relative path for original compile-source root.",
                      "src/main/java", compileSourceRoots.get( 0 ) );
        assertEquals( "Restored dynamic state should have a relative path for new compile-source root.",
                      "target/generated-sources/modello", compileSourceRoots.get( 1 ) );

        projectBuilder.calculateConcreteState( project, config );

        compileSourceRoots = project.getCompileSourceRoots();
        assertNotNull( "Second concrete state compile-source roots should not be null.", compileSourceRoots );
        assertEquals( "Second concrete state should contain two compile-source roots.", 2, compileSourceRoots.size() );
        assertEquals( "Second concrete state should have an absolute path for original compile-source root.",
                      new File( project.getBasedir(), "src/main/java" ).getAbsolutePath(), compileSourceRoots.get( 0 ) );
        assertEquals( "Second concrete state should have an absolute path for new compile-source root.", newSourceRoot,
                      compileSourceRoots.get( 1 ) );
    }

    public void testShouldMaintainAddedAndExistingPluginEntriesInRoundTrip()
        throws IOException, XmlPullParserException, URISyntaxException, ProjectBuildingException,
        ModelInterpolationException
    {
        MavenProject project = buildProject( "pom-plugins.xml" );

        String firstPlugin = "one:first-maven-plugin";
        String secondPlugin = "two:second-maven-plugin";
        String thirdPlugin = "three:third-maven-plugin";

        project.getBuild().flushPluginMap();
        Map pluginMap = project.getBuild().getPluginsAsMap();

        assertNotNull( "Before calculating concrete state, project should contain plugin: " + firstPlugin,
                       pluginMap.get( firstPlugin ) );
        assertNotNull( "Before calculating concrete state, project should contain plugin: " + secondPlugin,
                       pluginMap.get( secondPlugin ) );
        assertNull( "Before calculating concrete state, project should NOT contain plugin: " + thirdPlugin,
                    pluginMap.get( thirdPlugin ) );

        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration();
        projectBuilder.calculateConcreteState( project, config );

        project.getBuild().flushPluginMap();
        pluginMap = project.getBuild().getPluginsAsMap();

        assertNotNull( "After calculating concrete state, project should contain plugin: " + firstPlugin,
                       pluginMap.get( firstPlugin ) );
        assertNotNull( "After calculating concrete state, project should contain plugin: " + secondPlugin,
                       pluginMap.get( secondPlugin ) );
        assertNull( "After calculating concrete state, project should NOT contain plugin: " + thirdPlugin,
                    pluginMap.get( thirdPlugin ) );

        Plugin third = new Plugin();
        third.setGroupId( "three" );
        third.setArtifactId( "third-maven-plugin" );
        third.setVersion( "3" );

        project.addPlugin( third );

        project.getBuild().flushPluginMap();
        pluginMap = project.getBuild().getPluginsAsMap();

        assertNotNull( "After adding third plugin, project should contain plugin: " + firstPlugin,
                       pluginMap.get( firstPlugin ) );
        assertNotNull( "After adding third plugin, project should contain plugin: " + secondPlugin,
                       pluginMap.get( secondPlugin ) );
        assertNotNull( "After adding third plugin, project should contain plugin: " + thirdPlugin,
                       pluginMap.get( thirdPlugin ) );

        projectBuilder.restoreDynamicState( project, config );

        project.getBuild().flushPluginMap();
        pluginMap = project.getBuild().getPluginsAsMap();

        assertNotNull( "After restoring project dynamism, project should contain plugin: " + firstPlugin,
                       pluginMap.get( firstPlugin ) );
        assertNotNull( "After restoring project dynamism, project should contain plugin: " + secondPlugin,
                       pluginMap.get( secondPlugin ) );
        assertNotNull( "After restoring project dynamism, project should contain plugin: " + thirdPlugin,
                       pluginMap.get( thirdPlugin ) );
    }

    public void testShouldMaintainAddedAndExistingSourceRootsInRoundTrip()
        throws IOException, XmlPullParserException, URISyntaxException, ProjectBuildingException,
        ModelInterpolationException
    {
        MavenProject project = buildProject( "pom-source-roots.xml" );
        
        File basedir = project.getBasedir();

        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration();
        projectBuilder.calculateConcreteState( project, config );

        assertTrue( "Before adding source roots, project should be concrete", project.isConcrete() );
        assertEquals( "Before adding source roots, project should contain one compile source root", 1, project.getCompileSourceRoots().size() );
        assertEquals( "First compile source root should be absolute ref to src/main/java", new File( basedir, "src/main/java" ).getAbsolutePath(), project.getCompileSourceRoots().get( 0 ) );
        
        assertEquals( "Before adding source roots, project should contain one test source root", 1, project.getTestCompileSourceRoots().size() );
        assertEquals( "First test source root should be absolute ref to src/test/java", new File( basedir, "src/test/java" ).getAbsolutePath(), project.getTestCompileSourceRoots().get( 0 ) );
        
        assertEquals( "Before adding source roots, project should contain one script source root", 1, project.getScriptSourceRoots().size() );
        assertEquals( "First script source root should be relative ref to src/main/scripts", "src/main/scripts", project.getScriptSourceRoots().get( 0 ) );

        project.addCompileSourceRoot( new File( basedir, "target/generated/src/main/java" ).getAbsolutePath() );
        project.addTestCompileSourceRoot( new File( basedir, "target/generated/src/test/java" ).getAbsolutePath() );
        project.addScriptSourceRoot( new File( basedir, "target/generated/src/main/scripts" ).getAbsolutePath() );
        
        project.getProperties().setProperty( "trigger-transition", "true" );
        
        projectBuilder.restoreDynamicState( project, config );
        
        projectBuilder.calculateConcreteState( project, config );
        
        assertTrue( "After adding source roots and transitioning, project should be concrete", project.isConcrete() );
        assertEquals( "After adding source roots and transitioning, project should contain two compile source roots", 2, project.getCompileSourceRoots().size() );
        assertEquals( "First compile source root should be absolute ref to src/main/java", new File( basedir, "src/main/java" ).getAbsolutePath(), project.getCompileSourceRoots().get( 0 ) );
        assertEquals( "Second compile source root should be absolute ref to target/generated/src/main/java", new File( basedir, "target/generated/src/main/java" ).getAbsolutePath(), project.getCompileSourceRoots().get( 1 ) );
        
        assertEquals( "After adding source roots and transitioning, project should contain two test source roots", 2, project.getTestCompileSourceRoots().size() );
        assertEquals( "First test source root should be absolute ref to src/test/java", new File( basedir, "src/test/java" ).getAbsolutePath(), project.getTestCompileSourceRoots().get( 0 ) );
        assertEquals( "Second test source root should be absolute ref to target/generated/src/test/java", new File( basedir, "target/generated/src/test/java" ).getAbsolutePath(), project.getTestCompileSourceRoots().get( 1 ) );
        
        assertEquals( "After adding source roots and transitioning, project should contain two script source roots", 2, project.getScriptSourceRoots().size() );
        assertEquals( "First script source root should be relative ref to src/main/scripts", "src/main/scripts", project.getScriptSourceRoots().get( 0 ) );
        assertEquals( "Second script source root should be relative ref to target/generated/src/main/scripts", "target/generated/src/main/scripts", project.getScriptSourceRoots().get( 1 ) );
    }

    public void testShouldInterpolatePluginLevelDependency()
        throws IOException, XmlPullParserException, URISyntaxException, ProjectBuildingException,
        ModelInterpolationException
    {
        MavenProject project = buildProject( "plugin-level-dep.pom.xml" );

        Plugin plugin =
            (Plugin) project.getBuild().getPluginsAsMap().get( "org.apache.maven.plugins:maven-compiler-plugin" );

        assertNotNull( "ERROR - compiler plugin config not found!", plugin );
        assertTrue( "ERROR - compiler plugin custom dependencies not found!",
                    ( plugin.getDependencies() != null && !plugin.getDependencies().isEmpty() ) );

        Dependency dep = (Dependency) plugin.getDependencies().get( 0 );

        assertEquals( "custom dependency version should be an INTERPOLATED reference to this project's version.",
                      project.getVersion(), dep.getVersion() );
    }

    // Useful for diagnostics.
    // private void displayPOM( Model model )
    // throws IOException
    // {
    // StringWriter writer = new StringWriter();
    // new MavenXpp3Writer().write( writer, model );
    //
    // System.out.println( writer.toString() );
    // }

    private void assertResourcePresent( String testLabel, String directory, List resources )
    {
        boolean found = false;

        if ( resources != null )
        {
            for ( Iterator it = resources.iterator(); it.hasNext(); )
            {
                Resource resource = (Resource) it.next();
                if ( new File( directory ).getAbsolutePath().equals(
                                                                     new File( resource.getDirectory() ).getAbsolutePath() ) )
                {
                    found = true;
                    break;
                }
            }
        }

        if ( !found )
        {
            fail( "Missing resource with directory: " + directory + " in " + testLabel );
        }
    }

    private void assertFilterPresent( String testLabel, String path, List filters )
    {
        boolean found = false;

        if ( filters != null )
        {
            for ( Iterator it = filters.iterator(); it.hasNext(); )
            {
                String filterPath = (String) it.next();
                if ( new File( path ).getAbsolutePath().equals( new File( filterPath ).getAbsolutePath() ) )
                {
                    found = true;
                    break;
                }
            }
        }

        if ( !found )
        {
            fail( "Missing filter with path: " + path + " in " + testLabel );
        }
    }

    private MavenProject buildProject( String path )
        throws IOException, XmlPullParserException, URISyntaxException, ProjectBuildingException
    {
        ClassLoader cloader = Thread.currentThread().getContextClassLoader();
        URL resource = cloader.getResource( "project-dynamism/" + path );

        if ( resource == null )
        {
            fail( "Cannot find classpath resource for POM: " + path );
        }

        String resourcePath = StringUtils.replace( resource.getPath(), "%20", " " );
        URI uri = new File( resourcePath ).toURI().normalize();

        File pomFile = new File( uri );
        pomFile = pomFile.getAbsoluteFile();

        MavenProject project = projectBuilder.build( pomFile, new DefaultProjectBuilderConfiguration() );

        return project;
    }

}
