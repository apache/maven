/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.maven.plugin.eclipse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.maven.cli.ConsoleDownloadMonitor;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.PlexusLoggerAdapter;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * <p>
 * Master test for eclipse .classpath and .wtpmodules generation.
 * </p>
 * <p>
 * This test use a 2 modules project with all the mvn dependencies flavours (direct, transitive, with  
 * compile/test/provided/system scope, required and optional, artifacts and modules).
 * </p>
 * <p>
 * In order to fully test the eclipse plugin execution in a such complex environment mvn is executed from a command line.
 * Mvn is started using a custom settings.xml file, created on the fly. The custom settings.xml only adds a mirror for
 * the central repository which is actually a local (file://) repository for loading files from <code>src/test/m2repo</code>
 * </p>
 * <p>The following is the base layout of modules/dependencies. The actual test is to check generated files for module-2</p>
 * <pre>
 * 
 *            +----------------+       +-----------------+       +-----------------+
 *           /| module 1 (jar) | ----> |   refproject    | ----> | deps-refproject |
 *          / +----------------+       +-----------------+       +-----------------+  
 *         /           ^
 *    root             | (depends on)
 *         \           |
 *          \ +----------------+       +-----------------+       +-----------------+
 *           \| module 2 (war) | ----> |     direct      | ----> |   deps-direct   |
 *            +----------------+       +-----------------+       +-----------------+   
 * 
 * </pre>
 * @todo a know problem with this approach is that tests are running with the installed version of the plugin! Don't
 * enable test in pom.xml at the moment or you will never be able to build.
 * @author Fabrizio Giustina
 * @version $Id$
 */
public class EclipsePluginMasterProjectTest
    extends AbstractEclipsePluginTestCase
{

    protected File basedir;

    protected MavenEmbedder maven;

    protected List projectList = new ArrayList();

    protected void setUp()
        throws Exception
    {
        this.basedir = getTestFile( "src/test/projects/master-test" );

        this.maven = new MavenEmbedder();
        this.maven.setClassLoader( Thread.currentThread().getContextClassLoader() );
        this.maven.setLogger( new MavenEmbedderConsoleLogger() );
        this.maven.start();

        projectList.add( maven.readProjectWithDependencies( new File( basedir, "pom.xml" ) ) );

        super.setUp();

        executeMaven2CommandLine( basedir );
    }

    protected void tearDown()
        throws Exception
    {
        maven.stop();
        super.tearDown();
    }

    /**
     * Currently disabled because:
     * <ul>
     *   <li>the reactor build is not run by the embedder</li>
     *   <li>the embedder doesn't support custom settings</li>
     * </ul>
     * @throws Exception
     */
    public void executeMaven2WithEmbedder()
        throws Exception
    {
        EventMonitor eventMonitor = new DefaultEventMonitor( new PlexusLoggerAdapter( new MavenEmbedderConsoleLogger() ) );

        this.maven.execute( projectList, Arrays.asList( new String[] {
            "org.apache.maven.plugins:maven-eclipse-plugin:clean",
            "org.apache.maven.plugins:maven-eclipse-plugin:eclipse" } ), eventMonitor, new ConsoleDownloadMonitor(),
                            new Properties(), this.basedir );
    }

    public void testModule1Project()
        throws Exception
    {
        assertFileEquals( null, new File( basedir, "module-1/project" ), new File( basedir, "module-1/.project" ) );
    }

    public void testModule1Classpath()
        throws Exception
    {

        InputStream fis = new FileInputStream( new File( basedir, "module-1/.classpath" ) );
        String classpath = IOUtil.toString( fis );
        IOUtil.close( fis );

        // direct dependencies, include all
        assertContains( "Invalid classpath", classpath, "/refproject-compile" );
        assertContains( "Invalid classpath", classpath, "/refproject-sysdep" );
        assertContains( "Invalid classpath", classpath, "/refproject-test" );
        assertContains( "Invalid classpath", classpath, "/refproject-optional" );
        assertContains( "Invalid classpath", classpath, "/refproject-provided" );

        // transitive dependencies
        assertContains( "Invalid classpath", classpath, "/deps-refproject-compile" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-refproject-test" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-refproject-optional" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-refproject-provided" );

    }

    public void testModule1Wtpmodules()
        throws Exception
    {
        assertFileEquals( null, new File( basedir, "module-1/wtpmodules" ), new File( basedir, "module-1/.wtpmodules" ) );
    }

    public void testModule2Project()
        throws Exception
    {
        assertFileEquals( null, new File( basedir, "module-2/project" ), new File( basedir, "module-2/.project" ) );
    }

    public void testModule2Classpath()
        throws Exception
    {
        InputStream fis = new FileInputStream( new File( basedir, "module-2/.classpath" ) );
        String classpath = IOUtil.toString( fis );
        IOUtil.close( fis );

        // direct dependencies: include all
        assertContains( "Invalid classpath", classpath, "/direct-compile" );
        assertContains( "Invalid classpath", classpath, "/direct-test" );
        assertContains( "Invalid classpath", classpath, "/direct-sysdep" );
        assertContains( "Invalid classpath", classpath, "/direct-optional" );
        assertContains( "Invalid classpath", classpath, "/direct-provided" );

        // referenced project: not required, but it's not a problem to have them included
        assertContains( "Invalid classpath", classpath, "/module-1" );
        // assertDoesNotContain( "Invalid classpath", classpath, "/refproject-compile" );
        // assertDoesNotContain( "Invalid classpath", classpath, "/refproject-sysdep" );
        assertDoesNotContain( "Invalid classpath", classpath, "/refproject-test" );
        assertDoesNotContain( "Invalid classpath", classpath, "/refproject-optional" );
        assertDoesNotContain( "Invalid classpath", classpath, "/refproject-provided" );

        // transitive dependencies from referenced projects
        assertContains( "Invalid classpath", classpath, "/deps-direct-compile" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-direct-test" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-direct-optional" );
        // @todo should this be included? see MNG-514
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-direct-provided" );

        // transitive dependencies from referenced projects
        assertContains( "Invalid classpath", classpath, "/deps-refproject-compile" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-refproject-test" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-refproject-optional" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-refproject-provided" );
    }

    public void testModule2Wtpmodules()
        throws Exception
    {
        InputStream fis = new FileInputStream( new File( basedir, "module-2/.wtpmodules" ) );
        String wtpmodules = IOUtil.toString( fis );
        IOUtil.close( fis );

        // direct dependencies: include only runtime (also optional) dependencies
        assertContains( "Invalid wtpmodules", wtpmodules, "/direct-compile" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/direct-test" );
        assertContains( "Invalid wtpmodules", wtpmodules, "/direct-sysdep" );
        assertContains( "Invalid wtpmodules", wtpmodules, "/direct-optional" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/direct-provided" );

        // referenced project: only runtime deps
        assertContains( "Invalid wtpmodules", wtpmodules, "/module-1" );
        assertContains( "Invalid wtpmodules", wtpmodules, "/refproject-compile" );
        assertContains( "Invalid wtpmodules", wtpmodules, "/refproject-sysdep" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/refproject-test" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/refproject-optional" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/refproject-provided" );

        // transitive dependencies from referenced projects
        assertContains( "Invalid wtpmodules", wtpmodules, "/deps-direct-compile" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/deps-direct-test" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/deps-direct-optional" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/deps-direct-provided" );

        // transitive dependencies from referenced projects
        assertContains( "Invalid wtpmodules", wtpmodules, "/deps-refproject-compile" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/deps-refproject-test" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/deps-refproject-optional" );
        assertDoesNotContain( "Invalid wtpmodules", wtpmodules, "/deps-refproject-provided" );
    }

    /**
     * Execute mvn from command line.
     * @throws Exception any exception caught is thrown during tests
     */
    protected void executeMaven2CommandLine( File workingDir )
        throws Exception
    {

        Commandline cmd = createMaven2CommandLine( workingDir );

        int exitCode = CommandLineUtils.executeCommandLine( cmd, new DefaultConsumer(), new DefaultConsumer() );

        if ( exitCode != 0 )
        {
            throw new CommandLineException( "The command line failed. Exit code: " + exitCode );
        }
    }

    /**
     * Convenience method to create a m2 command line from a given working directory.
     *
     * @param workingDir a not null working directory.
     * @return the m2 command line
     * @throws Exception any exception caught is thrown during tests
     */
    protected Commandline createMaven2CommandLine( File workingDir )
        throws Exception
    {

        assertNotNull( "workingDir can't be null", workingDir );
        assertTrue( "workingDir must exist", workingDir.exists() );

        // read default settings and extract local repository path
        MavenSettingsBuilder settingsBuilder = (MavenSettingsBuilder) lookup( MavenSettingsBuilder.ROLE );
        Settings defaultSettings = settingsBuilder.buildSettings();

        String settingsPath = createTestSettings( defaultSettings );

        Commandline cmd = new Commandline();

        cmd.setWorkingDirectory( workingDir.getAbsolutePath() );

        cmd.setExecutable( "mvn" );
        cmd.createArgument().setValue( "-s" + settingsPath );
        cmd.createArgument().setValue( "-e" );

        cmd.createArgument().setValue( "eclipse:clean" );
        cmd.createArgument().setValue( "eclipse:eclipse" );

        return cmd;
    }

    private String createTestSettings( Settings defaultSettings )
        throws IOException
    {
        // prepare a temporary settings.xml
        File settings = File.createTempFile( "settings", ".xml" );
        settings.deleteOnExit();
        Writer w = new FileWriter( settings );
        XMLWriter writer = new PrettyPrintXMLWriter( w );
        writer.startElement( "settings" );

        // keep default local repository
        writer.startElement( "localRepository" );
        writer.writeText( defaultSettings.getLocalRepository() );
        writer.endElement();

        writer.startElement( "interactiveMode" );
        writer.writeText( "false" );
        writer.endElement();

        writer.startElement( "mirrors" );
        writer.startElement( "mirror" );

        // add a file mirror, so that dependencies are loaded from the plugin directory
        writer.startElement( "id" );
        writer.writeText( "localtest" );
        writer.endElement();
        writer.startElement( "url" );
        writer.writeText( "file://" + getBasedir().replace( '\\', '/' ) + "/src/test/m2repo" );
        writer.endElement();
        writer.startElement( "mirrorOf" );
        writer.writeText( "central" );
        writer.endElement();

        writer.endElement();
        writer.endElement();

        writer.endElement();
        IOUtil.close( w );
        settings.deleteOnExit();

        return settings.getAbsolutePath();
    }

}
