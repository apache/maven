package org.apache.maven.bootstrap.installer;

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

import org.apache.maven.bootstrap.Bootstrap;
import org.apache.maven.bootstrap.model.Dependency;
import org.apache.maven.bootstrap.model.ModelReader;
import org.apache.maven.bootstrap.util.FileUtils;
import org.apache.maven.bootstrap.util.SimpleArgumentParser;
import org.codehaus.plexus.util.Expand;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Arrays;

/**
 * Main class for bootstrap module.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class BootstrapInstaller
{
    private final Bootstrap bootstrapper;

    private final String prefix;

    private String pluginsDirectory;

    private boolean buildPlugins;

    private boolean updateSnapshots;

    private boolean offline;

    public BootstrapInstaller( SimpleArgumentParser parser )
        throws Exception
    {
        this.bootstrapper = new Bootstrap( parser );

        this.prefix = parser.getArgumentValue( "--prefix" );

        this.buildPlugins = parser.isArgumentSet( "--build-plugins" );

        this.pluginsDirectory = parser.getArgumentValue( "--plugins-directory" );

        // TODO: use from Bootstrap.java
        this.updateSnapshots = parser.isArgumentSet( "--update-snapshots" );

        this.offline = parser.isArgumentSet( "--offline" );
    }

    public static void main( String[] args )
        throws Exception
    {
        SimpleArgumentParser parser = Bootstrap.createDefaultParser();
        parser.addArgument( "--prefix", "The location to install Maven", true, getDefaultPrefix() );
        parser.addArgument( "--build-plugins", "Build the plugins from SVN" );
        parser.addArgument( "--plugins-directory", "Where the plugins are located to build from", true );
        parser.addArgument( "--update-snapshots", "Update snapshots during build" );
        parser.addArgument( "--offline", "Run build in offline mode", "-o" );

        parser.parseCommandLineArguments( args );

        BootstrapInstaller bootstrap = new BootstrapInstaller( parser );

        bootstrap.run();
    }

    private static String getDefaultPrefix()
    {
        String value;
        if ( Os.isFamily( "windows" ) )
        {
            value = "c:\\program files";
        }
        else
        {
            value = "/usr/local";
        }
        return value;
    }

    private void run()
        throws Exception
    {
        Date fullStart = new Date();

        String basedir = System.getProperty( "user.dir" );

        // TODO: only build this guy, then move the next part to a new phase using it for resolution
        // Root POM
//        buildProject( basedir, "", resolver, false );
//        buildProject( basedir, "maven-artifact-manager", resolver );

        bootstrapper.buildProject( new File( basedir ), true );

        ModelReader mavenCoreModel = bootstrapper.getCachedModel( "org.apache.maven", "maven-core" );

        File installation = new File( basedir, "bootstrap/target/installation" );
        createInstallation( installation, mavenCoreModel );

        // TODO: should just need assembly from basedir
        runMaven( installation, new File( basedir ), new String[]{"clean", "install"} );

        File mavenCoreDir = mavenCoreModel.getProjectFile().getParentFile();
        runMaven( installation, mavenCoreDir, new String[]{"clean", "assembly:assembly"} );

        String finalName = "maven-" + mavenCoreModel.getVersion();
        File file = new File( mavenCoreDir, "target/" + finalName + "-bin.zip" );

        File mavenHome = new File( prefix, finalName );

        System.out.println( "Installing Maven in " + mavenHome );

        FileUtils.deleteDirectory( mavenHome );

        Expand expand = new Expand();
        expand.setSrc( file );
        expand.setDest( new File( prefix ) );
        expand.execute();

        if ( !mavenHome.exists() )
        {
            throw new Exception( "Maven was not installed in " + mavenHome );
        }

        fixScriptPermissions( new File( mavenHome, "bin" ) );

        if ( buildPlugins )
        {
            if ( pluginsDirectory == null )
            {
                throw new UnsupportedOperationException( "SVN checkout of plugins not yet supported" );
            }

            runMaven( installation, new File( pluginsDirectory ),
                      new String[]{"--no-plugin-registry", "--fail-at-end", "clean", "install"} );
        }

        Bootstrap.stats( fullStart, new Date() );
    }

    private static void fixScriptPermissions( File binDirectory )
        throws InterruptedException, CommandLineException
    {
        if ( Os.isFamily( "unix" ) )
        {
            Commandline cli = new Commandline();

            cli.setExecutable( "chmod" );

            cli.createArgument().setValue( "+x" );

            cli.createArgument().setValue( new File( binDirectory, "mvn" ).getAbsolutePath() );

            cli.createArgument().setValue( new File( binDirectory, "m2" ).getAbsolutePath() );

            cli.execute().waitFor();
        }
    }

    private void runMaven( File installation, File basedir, String[] args )
        throws Exception, InterruptedException
    {
        Commandline cli = new Commandline();

        cli.setExecutable( new File( installation, "bin/mvn" ).getAbsolutePath() );

        // TODO: should we just remove this from the equation?
        cli.addEnvironment( "M2_HOME", installation.getAbsolutePath() );
        // No env is passed through
        cli.addEnvironment( "JAVA_HOME", System.getProperty( "java.home" ) );

        cli.setWorkingDirectory( basedir.getAbsolutePath() );

        cli.createArgument().setValue( "-e" );
        cli.createArgument().setValue( "--batch-mode" );

        if ( offline )
        {
            cli.createArgument().setValue( "-o" );
        }
        if ( updateSnapshots )
        {
            cli.createArgument().setValue( "--update-snapshots" );
        }

        for ( int i = 0; i < args.length; i++ )
        {
            cli.createArgument().setValue( args[i] );
        }

        System.out.println( "Running Maven... " );
        System.out.println( cli.toString() );

        int exitCode = CommandLineUtils.executeCommandLine( cli,
                                                            new WriterStreamConsumer( new PrintWriter( System.out ) ),
                                                            new WriterStreamConsumer( new PrintWriter( System.err ) ) );

        if ( exitCode != 0 )
        {
            throw new Exception( "Error executing Maven: exit code = " + exitCode );
        }
    }

    private void createInstallation( File dir, ModelReader mavenCoreModel )
        throws IOException, CommandLineException, InterruptedException
    {
        FileUtils.deleteDirectory( dir );

        dir.mkdirs();

        File libDirectory = new File( dir, "lib" );
        libDirectory.mkdir();

        File binDirectory = new File( dir, "bin" );

        File coreDirectory = new File( dir, "core" );
        coreDirectory.mkdir();

        File bootDirectory = new File( coreDirectory, "boot" );
        bootDirectory.mkdir();

        for ( Iterator i = mavenCoreModel.getDependencies().iterator(); i.hasNext(); )
        {
            Dependency dep = (Dependency) i.next();

            if ( dep.getArtifactId().equals( "classworlds" ) )
            {
                FileUtils.copyFileToDirectory( bootstrapper.getArtifactFile( dep ), bootDirectory );
            }
            else if ( dep.getArtifactId().equals( "plexus-container-default" ) ||
                dep.getArtifactId().equals( "plexus-utils" ) )
            {
                FileUtils.copyFileToDirectory( bootstrapper.getArtifactFile( dep ), coreDirectory );
            }
            else
            {
                FileUtils.copyFileToDirectory( bootstrapper.getArtifactFile( dep ), libDirectory );
            }
        }

        Dependency coreAsDep = new Dependency( mavenCoreModel.getGroupId(), mavenCoreModel.getArtifactId(),
                                               mavenCoreModel.getVersion(), mavenCoreModel.getPackaging(),
                                               Collections.EMPTY_LIST );

        FileUtils.copyFileToDirectory( bootstrapper.getArtifactFile( coreAsDep ), libDirectory );

        File srcBinDirectory = new File( mavenCoreModel.getProjectFile().getParentFile(), "src/bin" );

        FileUtils.copyDirectory( srcBinDirectory, binDirectory, null, "**/.svn/**" );

        fixScriptPermissions( binDirectory );
    }
}
