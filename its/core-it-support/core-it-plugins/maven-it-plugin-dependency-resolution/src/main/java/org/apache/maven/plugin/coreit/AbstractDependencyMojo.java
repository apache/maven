package org.apache.maven.plugin.coreit;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Properties;

/**
 * Provides common services for all mojos of this plugin.
 *
 * @author Benjamin Bentmann
 *
 */
public abstract class AbstractDependencyMojo
    extends AbstractMojo
{

    /**
     * The current Maven project.
     */
    @Parameter( defaultValue = "${project}", required = true, readonly = true )
    protected MavenProject project;

    /**
     * The number of trailing path levels that should be used to denote a class path element. If positive, each class
     * path element is trimmed down to the specified number of path levels by discarding leading directories, e.g. set
     * this parameter to 1 to keep only the simple file name. The trimmed down paths will always use the forward slash
     * as directory separator. For non-positive values, the full/absolute path is returned, using the platform-specific
     * separator.
     */
    @Parameter( property = "depres.significantPathLevels" )
    private int significantPathLevels;

    /**
     * Writes the specified artifacts to the given output file.
     *
     * @param pathname  The path to the output file, relative to the project base directory, may be <code>null</code> or
     *                  empty if the output file should not be written.
     * @param artifacts The list of artifacts to write to the file, may be <code>null</code>.
     * @throws MojoExecutionException If the output file could not be written.
     */
    protected void writeArtifacts( String pathname, Collection artifacts )
        throws MojoExecutionException
    {
        if ( pathname == null || pathname.length() <= 0 )
        {
            return;
        }

        File file = resolveFile( pathname );

        getLog().info( "[MAVEN-CORE-IT-LOG] Dumping artifact list: " + file );

        BufferedWriter writer = null;
        try
        {
            file.getParentFile().mkdirs();

            writer = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( file ), "UTF-8" ) );

            if ( artifacts != null )
            {
                for ( Object artifact1 : artifacts )
                {
                    Artifact artifact = (Artifact) artifact1;
                    String id = getId( artifact );
                    writer.write( id );
                    String optional = "";
                    if ( artifact.isOptional() )
                    {
                        optional = " (optional)";
                        writer.write( optional );
                    }
                    writer.newLine();
                    getLog().info( "[MAVEN-CORE-IT-LOG]   " + id + optional );
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to write artifact list", e );
        }
        finally
        {
            if ( writer != null )
            {
                try
                {
                    writer.close();
                }
                catch ( IOException e )
                {
                    // just ignore
                }
            }
        }
    }

    private String getId( Artifact artifact )
    {
        artifact.isSnapshot(); // decouple from MNG-2961
        return artifact.getId();
    }

    /**
     * Writes the specified class path elements to the given output file.
     *
     * @param pathname  The path to the output file, relative to the project base directory, may be <code>null</code> or
     *                  empty if the output file should not be written.
     * @param classPath The list of class path elements to write to the file, may be <code>null</code>.
     * @throws MojoExecutionException If the output file could not be written.
     */
    protected void writeClassPath( String pathname, Collection classPath )
        throws MojoExecutionException
    {
        if ( pathname == null || pathname.length() <= 0 )
        {
            return;
        }

        File file = resolveFile( pathname );

        getLog().info( "[MAVEN-CORE-IT-LOG] Dumping class path: " + file );

        BufferedWriter writer = null;
        try
        {
            file.getParentFile().mkdirs();

            writer = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( file ), "UTF-8" ) );

            if ( classPath != null )
            {
                for ( Object aClassPath : classPath )
                {
                    String element = aClassPath.toString();
                    writer.write( stripLeadingDirs( element, significantPathLevels ) );
                    writer.newLine();
                    getLog().info( "[MAVEN-CORE-IT-LOG]   " + element );
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to write class path list", e );
        }
        finally
        {
            if ( writer != null )
            {
                try
                {
                    writer.close();
                }
                catch ( IOException e )
                {
                    // just ignore
                }
            }
        }
    }

    protected void writeClassPathChecksums( String pathname, Collection classPath )
        throws MojoExecutionException
    {
        if ( pathname == null || pathname.length() <= 0 )
        {
            return;
        }

        File file = resolveFile( pathname );

        getLog().info( "[MAVEN-CORE-IT-LOG] Dumping class path checksums: " + file );

        Properties checksums = new Properties();

        if ( classPath != null )
        {
            for ( Object aClassPath : classPath )
            {
                String element = aClassPath.toString();

                File jarFile = new File( element );

                if ( !jarFile.isFile() )
                {
                    getLog().info( "[MAVEN-CORE-IT-LOG]   ( no file )                              < " + element );
                    continue;
                }

                String key = stripLeadingDirs( element, significantPathLevels );

                String hash;
                try
                {
                    hash = calcChecksum( jarFile );
                }
                catch ( NoSuchAlgorithmException e )
                {
                    throw new MojoExecutionException( "Failed to lookup message digest", e );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Failed to calculate checksum for " + jarFile, e );
                }

                checksums.setProperty( key, hash );

                getLog().info( "[MAVEN-CORE-IT-LOG]   " + hash + " < " + element );
            }
        }

        FileOutputStream os = null;
        try
        {
            file.getParentFile().mkdirs();

            os = new FileOutputStream( file );

            checksums.store( os, "MAVEN-CORE-IT" );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to write class path checksums", e );
        }
        finally
        {
            if ( os != null )
            {
                try
                {
                    os.close();
                }
                catch ( IOException e )
                {
                    // just ignore
                }
            }
        }
    }

    private String calcChecksum( File jarFile )
        throws IOException, NoSuchAlgorithmException
    {
        MessageDigest digester = MessageDigest.getInstance( "SHA-1" );

        try ( FileInputStream is = new FileInputStream( jarFile ) )
        {
            DigestInputStream dis = new DigestInputStream( is, digester );

            for ( byte[] buffer = new byte[1024 * 4]; dis.read( buffer ) >= 0; )
            {
                // just read it
            }
        }

        byte[] digest = digester.digest();

        StringBuilder hash = new StringBuilder( digest.length * 2 );

        for ( byte aDigest : digest )
        {
            @SuppressWarnings( "checkstyle:magicnumber" ) int b = aDigest & 0xFF;

            if ( b < 0x10 )
            {
                hash.append( '0' );
            }

            hash.append( Integer.toHexString( b ) );
        }

        return hash.toString();
    }

    private String stripLeadingDirs( String path, int significantPathLevels )
    {
        String result;
        if ( significantPathLevels > 0 )
        {
            result = "";
            File file = new File( path );
            for ( int i = 0; i < significantPathLevels && file != null; i++ )
            {
                if ( result.length() > 0 )
                {
                    // NOTE: Always use forward slash here to ease platform-independent testing
                    result = '/' + result;
                }
                result = file.getName() + result;
                file = file.getParentFile();
            }
        }
        else
        {
            result = path;
        }
        return result;
    }

    // NOTE: We don't want to test path translation here so resolve relative path manually for robustness
    private File resolveFile( String pathname )
    {
        File file = null;

        if ( pathname != null )
        {
            if ( pathname.contains( "@idx@" ) )
            {
                // helps to distinguished forked executions of the same mojo
                pathname = pathname.replaceAll( "@idx@", String.valueOf( nextCounter() ) );
            }

            file = new File( pathname );

            if ( !file.isAbsolute() )
            {
                file = new File( project.getBasedir(), pathname );
            }
        }

        return file;
    }

    private int nextCounter()
    {
        int counter = 0;

        String key = getClass().getName();

        synchronized ( System.class )
        {
            counter = Integer.getInteger( key, 0 );
            System.setProperty( key, Integer.toString( counter + 1 ) );
        }

        return counter;
    }

}
