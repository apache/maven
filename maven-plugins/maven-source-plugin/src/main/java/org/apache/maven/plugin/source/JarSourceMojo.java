package org.apache.maven.plugin.source;

/*
 * Copyright 2005 The Apache Software Foundation.
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.jar.JarArchiver;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This plugin bundles all the generated sources into a jar archive.
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 * @goal jar
 * @phase package
 */
public class JarSourceMojo
    extends AbstractMojo
{
    /**
     * @deprecated ICK! This needs to be generalized OUTSIDE of this mojo!
     */
    private static final List BANNED_PACKAGINGS;
    
    static
    {
        List banned = new ArrayList();
        
        banned.add( "pom" );
        
        BANNED_PACKAGINGS = banned;
    }

    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    private MavenProject project;

    /**
     * @parameter expression="${component.org.apache.maven.project.MavenProjectHelper}
     */
    private MavenProjectHelper projectHelper;
    
    /**
     * @parameter expression="${project.packaging}"
     * @readonly
     * @required
     */
    private String packaging;

    /**
     * @parameter expression="${project.build.finalName}"
     * @required
     */
    private String finalName;
    
    /**
     * @parameter expression="${attach}" default-value="true"
     */
    private boolean attach = true;

    /**
     * @parameter expression="${project.compileSourceRoots}"
     * @required
     */
    private List compileSourceRoots;

    /**
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;
    
    public void execute()
        throws MojoExecutionException
    {
        if ( !attach )
        {
            getLog().info( "NOT adding java-sources to attached artifacts list." );
            
            return;
        }
        else if ( BANNED_PACKAGINGS.contains( packaging ) )
        {
            getLog().info( "NOT adding java-sources to attached artifacts for packaging: \'" + packaging + "\'." );
            
            return;
        }
        
        // TODO: use a component lookup?
        JarArchiver archiver = new JarArchiver();

        SourceBundler sourceBundler = new SourceBundler();

        File outputFile = new File( outputDirectory, finalName + "-sources.jar" );

        File[] sourceDirectories = new File[compileSourceRoots.size()];
        int count = 0;
        for ( Iterator i = compileSourceRoots.iterator(); i.hasNext(); count++ )
        {
            sourceDirectories[count] = new File( (String) i.next() );
        }

        try
        {
            sourceBundler.makeSourceBundle( outputFile, sourceDirectories, archiver );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error building source JAR", e );
        }

        // TODO: these introduced dependencies on the project are going to become problematic - can we export it
        //  through metadata instead?
        projectHelper.attachArtifact( project, "java-source", "sources", outputFile );
    }
}
