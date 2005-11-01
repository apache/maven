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
import java.io.InputStream;
import java.io.Writer;

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
    public void testMasterProject()
        throws Exception
    {
        File basedir = getTestFile( "src/test/projects/master-test" );

        executeMaven2CommandLine( basedir );

        assertFileEquals( null, new File( basedir, "module-1/project" ), new File( basedir, "module-1/.project" ) );
        assertFileEquals( null, new File( basedir, "module-1/classpath" ), new File( basedir, "module-1/.classpath" ) );
        assertFileEquals( null, new File( basedir, "module-1/wtpmodules" ), new File( basedir, "module-1/.wtpmodules" ) );

        // the real test: this should include any sort of direct/transitive dependency handled by mvn
        assertFileEquals( null, new File( basedir, "module-2/project" ), new File( basedir, "module-2/.project" ) );

        // manual check, easier to handle
        checkModule2Classpath( new File( basedir, "module-2/.classpath" ) );

        // manual check, easier to handle
        checkModule2Wtpmodules( new File( basedir, "module-2/.wtpmodules" ) );

    }

    private void checkModule2Classpath( File file )
        throws Exception
    {
        InputStream fis = new FileInputStream( file );
        String classpath = IOUtil.toString( fis );
        IOUtil.close( fis );

        // direct dependencies: include all
        assertContains( "Invalid classpath", classpath, "/direct-compile" );
        assertContains( "Invalid classpath", classpath, "/direct-test" );
        assertContains( "Invalid classpath", classpath, "/direct-sysdep" );
        assertContains( "Invalid classpath", classpath, "/direct-optional" );

        // referenced project: no deps!
        assertContains( "Invalid classpath", classpath, "/module-1" );
        assertDoesNotContain( "Invalid classpath", classpath, "/refproject-compile" );
        assertDoesNotContain( "Invalid classpath", classpath, "/refproject-test" );
        assertDoesNotContain( "Invalid classpath", classpath, "/refproject-sysdep" );
        assertDoesNotContain( "Invalid classpath", classpath, "/refproject-optional" );

        // transitive dependencies from referenced projects
        assertContains( "Invalid classpath", classpath, "/deps-direct-compile" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-direct-test" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-direct-system" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-direct-optional" );

        // transitive dependencies from referenced projects
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-refproject-compile" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-refproject-test" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-refproject-system" );
        assertDoesNotContain( "Invalid classpath", classpath, "/deps-refproject-optional" );
    }

    private void checkModule2Wtpmodules( File file )
        throws Exception
    {
        InputStream fis = new FileInputStream( file );
        String classpath = IOUtil.toString( fis );
        IOUtil.close( fis );

        // direct dependencies: include all
        assertContains( "Invalid wtpmodules", classpath, "/direct-compile" );
        assertDoesNotContain( "Invalid wtpmodules", classpath, "/direct-test" );
        assertContains( "Invalid wtpmodules", classpath, "/direct-system" );
        assertContains( "Invalid wtpmodules", classpath, "/direct-optional" );

        // referenced project: only runtime deps
        assertContains( "Invalid wtpmodules", classpath, "/module-1" );
        assertContains( "Invalid wtpmodules", classpath, "/refproject-compile" );
        assertDoesNotContain( "Invalid wtpmodules", classpath, "/refproject-test" );
        assertDoesNotContain( "Invalid wtpmodules", classpath, "/refproject-system" );
        assertDoesNotContain( "Invalid wtpmodules", classpath, "/refproject-optional" );

        // transitive dependencies from referenced projects
        assertContains( "Invalid wtpmodules", classpath, "/deps-direct-compile" );
        assertDoesNotContain( "Invalid wtpmodules", classpath, "/deps-direct-test" );
        assertDoesNotContain( "Invalid wtpmodules", classpath, "/deps-direct-system" );
        assertDoesNotContain( "Invalid wtpmodules", classpath, "/deps-direct-optional" );

        // transitive dependencies from referenced projects
        assertDoesNotContain( "Invalid wtpmodules", classpath, "/deps-refproject-compile" );
        assertDoesNotContain( "Invalid wtpmodules", classpath, "/deps-refproject-test" );
        assertDoesNotContain( "Invalid wtpmodules", classpath, "/deps-refproject-system" );
        assertDoesNotContain( "Invalid wtpmodules", classpath, "/deps-refproject-optional" );
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

        Commandline cmd = new Commandline();

        cmd.setWorkingDirectory( workingDir.getAbsolutePath() );

        cmd.setExecutable( "mvn" );
        cmd.createArgument().setValue( "-s" + settings.getAbsolutePath() );
        cmd.createArgument().setValue( "-e" );

        cmd.createArgument().setValue( "eclipse:clean" );
        cmd.createArgument().setValue( "eclipse:eclipse" );

        return cmd;
    }

}
