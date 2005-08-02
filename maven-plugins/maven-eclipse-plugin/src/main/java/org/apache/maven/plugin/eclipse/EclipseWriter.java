package org.apache.maven.plugin.eclipse;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:kenney@neonics.com">Kenney Westerhof</a>
 * @version $Id$
 */
public class EclipseWriter
{
    private Log log;
    
    private File localRepository;
    
    public void setLocalRepositoryFile( File localRepository )
    {
        this.localRepository = localRepository;
    }
    
    public void setLog(Log log)
    {
        this.log = log;
    }

    public void write( File outputDir, MavenProject project, MavenProject executedProject, List reactorProjects )
        throws EclipsePluginException
    {
        Map map = new HashMap();

        assertNotEmpty( project.getGroupId(), "groupId" );

        assertNotEmpty( project.getArtifactId(), "artifactId" );

        map.put( "project.artifactId", project.getArtifactId() );
        
        File projectBaseDir = project.getFile().getParentFile();

        List referencedProjects = writeEclipseClasspath( projectBaseDir, outputDir, project, executedProject, map, reactorProjects );
        
        writeEclipseProject( projectBaseDir, outputDir, project, executedProject, referencedProjects, map );

        log.info( "Wrote Eclipse project for " + project.getArtifactId() + " to " + outputDir.getAbsolutePath() );
    }

    // ----------------------------------------------------------------------
    // .project
    // ----------------------------------------------------------------------

    protected void writeEclipseProject( File projectBaseDir, File basedir, MavenProject project, MavenProject executedProject, List referencedProjects, Map map )
        throws EclipsePluginException
    {
        FileWriter w;

        try
        {
            w = new FileWriter( new File( basedir, ".project" ) );
        }
        catch ( IOException ex )
        {
            throw new EclipsePluginException( "Exception while opening file.", ex );
        }

        XMLWriter writer = new PrettyPrintXMLWriter( w );

        writer.startElement( "projectDescription" );

        writer.startElement( "name" );

        if ( project.getArtifactId() == null )
        {
            throw new EclipsePluginException( "Missing element from the POM: artifactId." );
        }

        writer.writeText( project.getArtifactId() );

        writer.endElement();

        // TODO: this entire element might be dropped if the comment is null.
        // but as the maven1 eclipse plugin does it, it's better to be safe than sorry
        // A eclipse developer might want to look at this.
        writer.startElement( "comment" );

        if ( project.getDescription() != null )
        {
            writer.writeText( project.getDescription() );
        }

        writer.endElement();

        writer.startElement( "projects" );

        for ( Iterator it = referencedProjects.iterator(); it.hasNext(); )
        {
            writer.startElement( "project" );
            
            writer.writeText( ( (MavenProject) it.next() ).getArtifactId() );
            
            writer.endElement();
        }
        
        writer.endElement(); // projects

        writer.startElement( "buildSpec" );

        writer.startElement( "buildCommand" );

        writer.startElement( "name" );

        writer.writeText( "org.eclipse.jdt.core.javabuilder" );

        writer.endElement(); // name

        writer.startElement( "arguments" );

        writer.endElement(); // arguments

        writer.endElement(); // buildCommand

        writer.endElement(); // buildSpec

        writer.startElement( "natures" );

        writer.startElement( "nature" );

        writer.writeText( "org.eclipse.jdt.core.javanature" );

        writer.endElement(); // nature

        writer.endElement(); // natures


        if ( ! projectBaseDir.equals( basedir ) )
        {
            writer.startElement( "linkedResources" );

            addFileLink( writer, projectBaseDir, basedir, project.getFile() );

            addSourceLinks( writer, projectBaseDir, basedir, executedProject.getCompileSourceRoots() );

            addResourceLinks( writer, projectBaseDir, basedir, executedProject.getBuild().getResources() );

            addSourceLinks( writer, projectBaseDir, basedir, executedProject.getTestCompileSourceRoots() );

            addResourceLinks( writer, projectBaseDir, basedir, executedProject.getBuild().getTestResources() );

            writer.endElement(); // linedResources
        }

        writer.endElement(); // projectDescription

        close( w );
    }

    // ----------------------------------------------------------------------
    // .classpath
    // ----------------------------------------------------------------------

    protected List writeEclipseClasspath( File projectBaseDir, File basedir, MavenProject project, MavenProject executedProject, Map map, List reactorProjects )
        throws EclipsePluginException
    {
        FileWriter w;

        try
        {
            w = new FileWriter( new File( basedir, ".classpath" ) );
        }
        catch ( IOException ex )
        {
            throw new EclipsePluginException( "Exception while opening file.", ex );
        }

        XMLWriter writer = new PrettyPrintXMLWriter( w );

        writer.startElement( "classpath" );

        // ----------------------------------------------------------------------
        // The source roots
        // ----------------------------------------------------------------------

        addSourceRoots( writer, projectBaseDir, basedir,
                        executedProject.getCompileSourceRoots(),
                        null );

        addResources( writer, projectBaseDir, basedir,
                      project.getBuild().getResources(),
                      null );

        // ----------------------------------------------------------------------
        // The test sources and resources
        // ----------------------------------------------------------------------

        addSourceRoots( writer, projectBaseDir, basedir,
                        executedProject.getTestCompileSourceRoots(),
                        project.getBuild().getTestOutputDirectory() );

        addResources( writer, projectBaseDir, basedir,
                      project.getBuild().getTestResources(),
                      project.getBuild().getTestOutputDirectory() );

        // ----------------------------------------------------------------------
        // The default output
        // ----------------------------------------------------------------------

        writer.startElement( "classpathentry" );

        writer.addAttribute( "kind", "output" );

        writer.addAttribute( "path", toRelative( projectBaseDir, project.getBuild().getOutputDirectory() ) );

        writer.endElement();

        // ----------------------------------------------------------------------
        // The JRE reference
        // ----------------------------------------------------------------------

        writer.startElement( "classpathentry" );

        writer.addAttribute( "kind", "var" );

        writer.addAttribute( "rootpath", "JRE_SRCROOT" );

        writer.addAttribute( "path", "JRE_LIB" );

        writer.addAttribute( "sourcepath", "JRE_SRC" );

        writer.endElement();

        // ----------------------------------------------------------------------
        // The dependencies
        // ----------------------------------------------------------------------
        
        List referencedProjects = new ArrayList();

        Set artifacts = project.getArtifacts();

        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            
            MavenProject refProject = addDependency( writer, artifact, reactorProjects );
            
            if ( refProject != null )
            {
                referencedProjects.add( refProject );
            }
        }

        writer.endElement();

        close( w );
        
        return referencedProjects;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private void addSourceRoots( XMLWriter writer, File projectBaseDir, File basedir, List sourceRoots, String output )
    {
        for ( Iterator it = sourceRoots.iterator(); it.hasNext(); )
        {
            String sourceRoot = (String) it.next();

            if ( new File( sourceRoot ).isDirectory() )
            {
                writer.startElement( "classpathentry" );

                writer.addAttribute( "kind", "src" );

                sourceRoot = toRelative( projectBaseDir, sourceRoot );
                if (!projectBaseDir.equals(basedir))
                {
                    sourceRoot = sourceRoot.replaceAll( "/", "-" );
                }
                
                writer.addAttribute( "path", sourceRoot );

                if ( output != null )
                {
                    writer.addAttribute( "output", toRelative( projectBaseDir, output ) );
                }

                writer.endElement();
            }
        }
    }

    private void addResources( XMLWriter writer, File projectBaseDir, File basedir, List resources, String output )
    {
        for ( Iterator it = resources.iterator(); it.hasNext(); )
        {
            Resource resource = (Resource) it.next();

            if ( resource.getIncludes().size() != 0 )
            {
                log.warn( "This plugin currently doesn't support include patterns for resources. Adding the entire directory." );
            }

            if ( resource.getExcludes().size() != 0 )
            {
                log.warn( "This plugin currently doesn't support exclude patterns for resources. Adding the entire directory." );
            }

            if ( !StringUtils.isEmpty( resource.getTargetPath() ) )
            {
                log.error( "This plugin currently doesn't support target paths for resources." );

                return;
            }

            File resourceDirectory = new File( resource.getDirectory() );

            if ( !resourceDirectory.exists() || !resourceDirectory.isDirectory() )
            {
                continue;
            }

            writer.startElement( "classpathentry" );

            writer.addAttribute( "kind", "src" );

            String resourceDir = resource.getDirectory();
            resourceDir = toRelative( projectBaseDir, resourceDir );
            if (!projectBaseDir.equals(basedir))
            {
                resourceDir = resourceDir.replaceAll( "/", "-" );
            }
            
            writer.addAttribute( "path", resourceDir );

            if ( output != null )
            {
                writer.addAttribute( "output", toRelative( projectBaseDir, output ) );
            }

            writer.endElement();
        }
    }

    private void addSourceLinks( XMLWriter writer, File projectBaseDir, File basedir, List sourceRoots )
    {
        for ( Iterator it = sourceRoots.iterator(); it.hasNext(); )
        {
            String sourceRoot = (String) it.next();

            if ( new File( sourceRoot ).isDirectory() )
            {
                writer.startElement( "link" );

                writer.startElement( "name" );

                writer.writeText( toRelative( projectBaseDir, sourceRoot ).replaceAll( "/", "-" ) );
                
                writer.endElement(); // name

                writer.startElement( "type" );

                writer.writeText( "2" );

                writer.endElement(); // type

                writer.startElement( "location" );

                writer.writeText( sourceRoot.replaceAll("\\\\", "/") );

                writer.endElement(); // location

                writer.endElement(); // link
            }
        }
    }

    private void addResourceLinks( XMLWriter writer, File projectBaseDir, File basedir, List sourceRoots )
    {
        for ( Iterator it = sourceRoots.iterator(); it.hasNext(); )
        {
            String resourceDir = ((Resource) it.next() ).getDirectory();

            if ( new File( resourceDir ).isDirectory() )
            {
                writer.startElement( "link" );

                writer.startElement( "name" );

                writer.writeText( toRelative( projectBaseDir, resourceDir ).replaceAll( "/", "-" ) );

                writer.endElement(); // name

                writer.startElement( "type" );

                writer.writeText( "2" );

                writer.endElement(); // type

                writer.startElement( "location" );

                writer.writeText( resourceDir.replaceAll( "\\\\", "/" ) );

                writer.endElement(); // location

                writer.endElement(); // link
            }
        }
    }
    
    private void addFileLink( XMLWriter writer, File projectBaseDir, File basedir, File file )
    {
        if ( file.isFile() )
        {
            writer.startElement( "link" );

            writer.startElement( "name" );

            writer.writeText( toRelative( projectBaseDir, file.toString() ).replaceAll( "/", "-" ) );

            writer.endElement(); // name

            writer.startElement( "type" );

            writer.writeText( "1" );

            writer.endElement(); // type

            writer.startElement( "location" );

            writer.writeText( file.toString().replaceAll( "\\\\", "/" ) );

            writer.endElement(); // location

            writer.endElement(); // link
        }
        else
        {
            log.warn( "Not adding a file link to " + file + "; it is not a file" );
        }
    }
 
    /**
     * 
     * @param writer
     * @param artifact
     * @param reactorProjects
     * @return null or the reactorProject providing this dependency
     */
    private MavenProject addDependency( XMLWriter writer, Artifact artifact, List reactorProjects )
    {
        MavenProject reactorProject = findReactorProject( reactorProjects, artifact );

        String path = null;
        
        String kind = null;
        

        if (reactorProject != null)
        {
            path = "/" + reactorProject.getArtifactId();
            
            kind = "src";
        }
        else
        {
            File artifactPath = artifact.getFile();

            if ( artifactPath == null )
            {
                log.error( "The artifacts path was null. Artifact id: " + artifact.getId() );
    
                return null;
            }
            
            path = "M2_REPO/" + toRelative( localRepository, artifactPath.getPath() );
            
            kind = "var";
        }

        writer.startElement( "classpathentry" );

        writer.addAttribute( "kind", kind );

        writer.addAttribute( "path", path );

        writer.endElement();
        
        return reactorProject;
    }
    
    /**
     * Utility method that locates a project producing the given artifact.
     * 
     * @param reactorProjects a list of projects to search.
     * @param artifact the artifact a project should produce.
     * @return null or the first project found producing the artifact.
     */
    private static MavenProject findReactorProject( List reactorProjects, Artifact artifact )
    {
        if ( reactorProjects == null )
        {
            return null;	// we're a single project
        }
        
        for (Iterator it = reactorProjects.iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();
            
            if ( project.getGroupId().equals( artifact.getGroupId() )
                && project.getArtifactId().equals( artifact.getArtifactId() )
                && project.getVersion().equals( artifact.getVersion() )
            )
            {
                return project;
            }
        }
        
        return null;
    }

    private void close( Writer closeable )
    {
        if ( closeable == null )
        {
            return;
        }

        try
        {
            closeable.close();
        }
        catch ( Exception e )
        {
            // ignore
        }
    }

    private String toRelative( File basedir, String absolutePath )
    {
        String relative;

        if ( absolutePath.startsWith( basedir.getAbsolutePath() ) )
        {
            relative = absolutePath.substring( basedir.getAbsolutePath().length() + 1 );
        }
        else
        {
            relative = absolutePath;
        }

        relative = StringUtils.replace( relative, "\\", "/" );

        return relative;
    }

    private void assertNotEmpty( String string, String elementName )
        throws EclipsePluginException
    {
        if ( string == null )
        {
            throw new EclipsePluginException( "Missing element from the project descriptor: '" + elementName + "'." );
        }
    }
}
