package org.apache.maven.plugin.jar;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License" );
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

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Signs a JAR using jarsigner.
 *
 * @author <a href="jerome@coffeebreaks.org">Jerome Lacoste</a>
 * @version $Id$
 * @goal sign
 * @phase package
 * @requiresProject
 * @todo refactor the common code with javadoc plugin
 */
public class JarSignMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${workingdir}" default-value="${basedir}"
     * @required
     */
    private File workingDirectory;

    /**
     * Directory containing the generated JAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    private File basedir;

    /**
     * Name of the generated JAR (without classifier and extension).
     *
     * @parameter alias="jarname" expression="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * Path of the jar to sign. When specified, the finalName is ignored.
     *
     * @parameter alias="jarpath"
     * @required
     */
    private String jarPath;

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${keystore}"
     */
    private String keystore;

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${storepass}"
     */
    private String storepass;

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${keypass}"
     */
    private String keypass;

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${sigfile}"
     * @todo make a File?
     */
    private String sigfile;

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${signedjar}" default-value="${project.build.directory}/signed/${project.build.finalName}.jar"
     * @required
     * @todo make a File?
     */
    private String signedjar;

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${type}"
     */
    private String type;

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${alias}"
     * @required
     */
    private String alias;

    /**
     * Enable verbose
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${verbose}" default-value="false"
     */
    private boolean verbose;

    public void execute()
        throws MojoExecutionException
    {

        List arguments = new ArrayList();

        Commandline commandLine = new Commandline();

        commandLine.setExecutable( getJarsignerPath() );

        addArgIf( arguments, verbose, "-verbose" );

        // I believe Commandline to add quotes where appropriate, although I haven't tested it enough.
        // FIXME addArgIfNotEmpty will break those parameters containing a space.
        // Look at webapp:gen-keystore for a way to fix that
        addArgIfNotEmpty( arguments, "-keystore", this.keystore );
        addArgIfNotEmpty( arguments, "-storepass", this.storepass );
        addArgIfNotEmpty( arguments, "-keypass", this.keypass );
        addArgIfNotEmpty( arguments, "-signedjar", this.signedjar );
        addArgIfNotEmpty( arguments, "-storetype", this.type );
        addArgIfNotEmpty( arguments, "-sigfile", this.sigfile );

        if ( jarPath != null )
        {
            arguments.add( new File( jarPath ) );
        }
        else
        {
            arguments.add( AbstractJarMojo.getJarFile( basedir, finalName, null ) );
        }

        addArgIf( arguments, alias != null, this.alias );

        for ( Iterator it = arguments.iterator(); it.hasNext(); )
        {
            commandLine.createArgument().setValue( it.next().toString() );
        }

        commandLine.setWorkingDirectory( workingDirectory.getAbsolutePath() );

        createParentDirIfNecessary( signedjar );

        getLog().debug( "Executing: " + commandLine );

        // jarsigner may ask for some input if the parameters are missing or incorrect. 
        // This should take care of it and make it fail gracefully
        final InputStream inputStream = new InputStream()
        {
            public int read()
            {
                return -1;
            }
        };
        StreamConsumer outConsumer = new StreamConsumer()
        {
            public void consumeLine( String line )
            {
                getLog().info( line );
            }
        };
        final StringBuffer errBuffer = new StringBuffer();
        StreamConsumer errConsumer = new StreamConsumer()
        {
            public void consumeLine( String line )
            {
                errBuffer.append( line );
                getLog().warn( line );
            }
        };

        try
        {
            int result = executeCommandLine( commandLine, inputStream, outConsumer, errConsumer );

            if ( result != 0 )
            {
                throw new MojoExecutionException( "Result of " + commandLine + " execution is: \'" + result + "\': "
                    + errBuffer.toString() + "." );
            }
        }
        catch ( CommandLineException e )
        {
            throw new MojoExecutionException( "command execution failed", e );
        }
    }

    private void createParentDirIfNecessary( final String file )
    {
        if ( file != null )
        {
            final File fileDir = new File( file ).getParentFile();

            if ( fileDir != null )
            { // not a relative path
                boolean mkdirs = fileDir.mkdirs();
                getLog().debug( "mdkirs: " + mkdirs + " " + fileDir );
            }
        }
    }

    // taken from JavadocReport then slightly refactored
    // should probably share with other plugins that use $JAVA_HOME/bin tools

    /**
     * Get the path of jarsigner tool depending the OS.
     *
     * @return the path of the jarsigner tool
     */
    private String getJarsignerPath()
    {
        return getJDKCommandPath( "jarsigner", getLog() );
    }

    private static String getJDKCommandPath( String command, Log logger )
    {
        String path = getJDKCommandExe( command ).getAbsolutePath();
        logger.debug( command + " executable=[" + path + "]" );
        return path;
    }

    private static File getJDKCommandExe( String command )
    {
        String fullCommand = command + ( SystemUtils.IS_OS_WINDOWS ? ".exe" : "" );

        File exe;

        // For IBM's JDK 1.2
        if ( SystemUtils.IS_OS_AIX )
        {
            exe = new File( SystemUtils.getJavaHome() + "/../sh", fullCommand );
        }
        else if ( SystemUtils.IS_OS_MAC_OSX )
        {
            exe = new File( SystemUtils.getJavaHome() + "/bin", fullCommand );
        }
        else
        {
            exe = new File( SystemUtils.getJavaHome() + "/../bin", fullCommand );
        }

        return exe;
    }

    // Helper methods. Could/should be shared e.g. with JavadocReport

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * conditionally based on the given flag.
     *
     * @param arguments
     * @param b the flag which controls if the argument is added or not.
     * @param value the argument value to be added.
     */
    private void addArgIf( List arguments, boolean b, String value )
    {
        if ( b )
        {
            arguments.add( value );
        }
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * if the the value is not null or empty.
     * <p>
     * Moreover, the value could be comma separated.
     *
     * @param arguments
     * @param key the argument name.
     * @param value the argument value to be added.
     * @see #addArgIfNotEmpty(java.util.List,String,String,boolean)
     */
    private void addArgIfNotEmpty( List arguments, String key, String value )
    {
        addArgIfNotEmpty( arguments, key, value, false );
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * if the the value is not null or empty.
     * <p>
     * Moreover, the value could be comma separated.
     *
     * @param arguments
     * @param key the argument name.
     * @param value the argument value to be added.
     * @param repeatKey repeat or not the key in the command line
     */
    private void addArgIfNotEmpty( List arguments, String key, String value, boolean repeatKey )
    {
        if ( !StringUtils.isEmpty( value ) )
        {
            arguments.add( key );

            StringTokenizer token = new StringTokenizer( value, "," );
            while ( token.hasMoreTokens() )
            {
                String current = token.nextToken().trim();

                if ( !StringUtils.isEmpty( current ) )
                {
                    arguments.add( current );

                    if ( token.hasMoreTokens() && repeatKey )
                    {
                        arguments.add( key );
                    }
                }
            }
        }
    }

    //
    // methods used for tests purposes - allow mocking and simulate automatic setters
    //

    protected int executeCommandLine( Commandline commandLine, InputStream inputStream, StreamConsumer stream1,
                                     StreamConsumer stream2 )
        throws CommandLineException
    {
        return CommandLineUtils.executeCommandLine( commandLine, inputStream, stream1, stream2 );
    }

    public void setWorkingDir( File workingDir )
    {
        this.workingDirectory = workingDir;
    }

    public void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

    public void setKeystore( String keystore )
    {
        this.keystore = keystore;
    }

    public void setKeypass( String keypass )
    {
        this.keypass = keypass;
    }

    public void setSignedJar( String signedjar )
    {
        this.signedjar = signedjar;
    }

    public void setAlias( String alias )
    {
        this.alias = alias;
    }

    // hiding for now - I don't think this is required to be seen
    /*
     public void setFinalName( String finalName ) {
     this.finalName = finalName;
     }
     */

    public void setJarPath( String jarPath )
    {
        this.jarPath = jarPath;
    }

    public void setStorepass( String storepass )
    {
        this.storepass = storepass;
    }

    public void setSigFile( String sigfile )
    {
        this.sigfile = sigfile;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public void setVerbose( boolean verbose )
    {
        this.verbose = verbose;
    }
}
