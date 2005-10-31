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
import java.io.FileWriter;
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

        // @fixme missing direct optional dependency
        assertFileEquals( null, new File( basedir, "module-1/classpath" ), new File( basedir, "module-1/.classpath" ) );
        assertFileEquals( null, new File( basedir, "module-1/wtpmodules" ), new File( basedir, "module-1/.wtpmodules" ) );

        // the real test: this should include any sort of direct/transitive dependency handled by mvn
        assertFileEquals( null, new File( basedir, "module-2/project" ), new File( basedir, "module-2/.project" ) );

        // @fixme missing direct optional dependency + unneeded transitive dependencies
        assertFileEquals( null, new File( basedir, "module-2/classpath" ), new File( basedir, "module-2/.classpath" ) );

        // @fixme the list of dependencies in .wtpmodules should be the same added by the war plugin
        assertFileEquals( null, new File( basedir, "module-2/wtpmodules" ), new File( basedir, "module-2/.wtpmodules" ) );

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
