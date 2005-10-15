package org.apache.maven.plugin.antlr;

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

import antlr.Tool;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.Permission;
import java.util.StringTokenizer;

/**
 * @goal generate
 * @phase generate-sources
 * @description Antlr plugin
 */
public class AntlrPlugin
    extends AbstractMojo
{
    /**
     * @parameter expression="${grammars}"
     * @required
     */
    private String grammars;

    /**
     * @parameter expression="${basedir}/src/main/antlr"
     * @required
     */
    private File sourceDirectory;

    /**
     * @parameter expression="${project.build.directory}/generated-sources/antlr"
     * @required
     */
    private String outputDirectory;

    /**
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    public void execute()
        throws MojoExecutionException
    {
        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        StringTokenizer st = new StringTokenizer( grammars, ", " );

        while ( st.hasMoreTokens() )
        {
            String eachGrammar = st.nextToken().trim();

            File grammar = new File( sourceDirectory, eachGrammar );

            getLog().info( "grammar: " + grammar );

            File generated = null;

            try
            {
                generated = getGeneratedFile( grammar.getPath(), outputDirectory );
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( "Failed to get generated file", e );
            }

            if ( generated.exists() )
            {
                if ( generated.lastModified() > grammar.lastModified() )
                {
                    // it's more recent, skip.
                    getLog().info( "The grammar is already generated" );
                    continue;
                }
            }

            if ( !generated.getParentFile().exists() )
            {
                generated.getParentFile().mkdirs();
            }

            String[] args = new String[]{"-o", generated.getParentFile().getPath(), grammar.getPath(),};

            SecurityManager oldSm = System.getSecurityManager();

            System.setSecurityManager( NoExitSecurityManager.INSTANCE );

            try
            {
                Tool.main( args );
            }
            catch ( SecurityException e )
            {
                if ( !e.getMessage().equals( "exitVM-0" ) )
                {
                    throw new MojoExecutionException( "Execution failed", e );
                }
            }
            finally
            {
                System.setSecurityManager( oldSm );
            }
        }

        if ( project != null )
        {
            project.addCompileSourceRoot( outputDirectory );
        }
    }

    protected File getGeneratedFile( String grammar, String outputDirectory )
        throws Exception
    {
        String generatedFileName = null;

        String packageName = "";

        try
        {

            BufferedReader in = new BufferedReader( new FileReader( grammar ) );

            String line;

            while ( ( line = in.readLine() ) != null )
            {
                line = line.trim();

                int extendsIndex = line.indexOf( " extends " );

                if ( line.startsWith( "class " ) && extendsIndex > -1 )
                {
                    generatedFileName = line.substring( 6, extendsIndex ).trim();

                    break;
                }
                else if ( line.startsWith( "package" ) )
                {
                    packageName = line.substring( 8 ).trim();
                }
            }

            in.close();
        }
        catch ( Exception e )
        {
            throw new Exception( "Unable to determine generated class", e );
        }
        if ( generatedFileName == null )
        {
            return null;
        }

        File genFile = null;

        if ( "".equals( packageName ) )
        {
            genFile = new File( outputDirectory, generatedFileName + ".java" );
        }
        else
        {
            String packagePath = packageName.replace( '.', File.separatorChar );

            packagePath = packagePath.replace( ';', File.separatorChar );

            genFile = new File( new File( outputDirectory, packagePath ), generatedFileName + ".java" );
        }

        return genFile;
    }

}

class NoExitSecurityManager
    extends SecurityManager
{
    static final NoExitSecurityManager INSTANCE = new NoExitSecurityManager();

    private NoExitSecurityManager()
    {
    }

    public void checkPermission( Permission permission )
    {
    }

    public void checkExit( int status )
    {
        throw new SecurityException( "exitVM-" + status );
    }
}
