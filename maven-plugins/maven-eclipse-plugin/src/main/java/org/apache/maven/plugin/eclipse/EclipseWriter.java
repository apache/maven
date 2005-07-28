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
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class EclipseWriter
{
    private File localRepository;

    public void setLocalRepositoryFile( File localRepository )
    {
        this.localRepository = localRepository;
    }

    public void write( MavenProject project, MavenProject executedProject, List reactorProjects )
        throws EclipsePluginException
    {
        File basedir = project.getFile().getParentFile();

        Map map = new HashMap();

        assertNotEmpty( project.getGroupId(), "groupId" );

        assertNotEmpty( project.getArtifactId(), "artifactId" );

        map.put( "project.artifactId", project.getArtifactId() );

        writeEclipseProject( basedir, project, map );

        writeEclipseClasspath( basedir, project, executedProject, map, reactorProjects );

        System.out.println( "Wrote Eclipse project for " + project.getArtifactId() + " to " + basedir.getAbsolutePath() );
    }

    // ----------------------------------------------------------------------
    // .project
    // ----------------------------------------------------------------------

    protected void writeEclipseProject( File basedir, MavenProject project, Map map )
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

        // TODO: Add project dependencies here
        // Should look in the reactor for other projects

        writer.startElement( "projects" );

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

        writer.endElement(); // projectDescription

        close( w );
    }

    // ----------------------------------------------------------------------
    // .classpath
    // ----------------------------------------------------------------------

    protected void writeEclipseClasspath( File basedir, MavenProject project, MavenProject executedProject, Map map, List reactorProjects )
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

        addSourceRoots( writer, project.getBasedir(),
                        executedProject.getCompileSourceRoots(),
                        null );

        addResources( writer, project.getBasedir(),
                      project.getBuild().getResources(),
                      null );

        // ----------------------------------------------------------------------
        // The test sources and resources
        // ----------------------------------------------------------------------

        addSourceRoots( writer, project.getBasedir(),
                        executedProject.getTestCompileSourceRoots(),
                        project.getBuild().getTestOutputDirectory() );

        addResources( writer, project.getBasedir(),
                      project.getBuild().getTestResources(),
                      project.getBuild().getTestOutputDirectory() );

        // ----------------------------------------------------------------------
        // The default output
        // ----------------------------------------------------------------------

        writer.startElement( "classpathentry" );

        writer.addAttribute( "kind", "output" );

        writer.addAttribute( "path", toRelative( basedir, project.getBuild().getOutputDirectory() ) );

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

        Set artifacts = project.getArtifacts();

        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            
            addDependency( writer, artifact, reactorProjects );
        }

        writer.endElement();

        close( w );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private void addSourceRoots( XMLWriter writer, File basedir, List sourceRoots, String output )
    {
        for ( Iterator it = sourceRoots.iterator(); it.hasNext(); )
        {
            String sourceRoot = (String) it.next();

            if ( new File( sourceRoot ).isDirectory() )
            {
                writer.startElement( "classpathentry" );

                writer.addAttribute( "kind", "src" );

                writer.addAttribute( "path", toRelative( basedir, sourceRoot ) );

                if ( output != null )
                {
                    writer.addAttribute( "output", toRelative( basedir, output ) );
                }

                writer.endElement();
            }
        }
    }

    private void addResources( XMLWriter writer, File basedir, List resources, String output )
    {
        for ( Iterator it = resources.iterator(); it.hasNext(); )
        {
            Resource resource = (Resource) it.next();

            if ( resource.getIncludes().size() != 0 )
            {
                System.err.println( "This plugin currently doesn't support include patterns for resources. Adding the entire directory." );
            }

            if ( resource.getExcludes().size() != 0 )
            {
                System.err.println( "This plugin currently doesn't support exclude patterns for resources. Adding the entire directory." );
            }

            if ( !StringUtils.isEmpty( resource.getTargetPath() ) )
            {
                System.err.println( "This plugin currently doesn't support target paths for resources." );

                return;
            }

            File resourceDirectory = new File( resource.getDirectory() );

            if ( !resourceDirectory.exists() || !resourceDirectory.isDirectory() )
            {
                continue;
            }

            writer.startElement( "classpathentry" );

            writer.addAttribute( "kind", "src" );

            writer.addAttribute( "path", toRelative( basedir, resource.getDirectory() ) );

            if ( output != null )
            {
                writer.addAttribute( "output", toRelative( basedir, output ) );
            }

            writer.endElement();
        }
    }

    private void addDependency( XMLWriter writer, Artifact artifact, List reactorProjects )
    {
        String path = getProjectPath( reactorProjects, artifact );
        
        String kind = path == null ? "var" : "src";
                
        // fall-through when no local project could be found in the reactor
        if ( path == null )
        {
            File artifactPath = artifact.getFile();

            if ( artifactPath == null )
            {
                System.err.println( "The artifacts path was null. Artifact id: " + artifact.getId() );
    
                return;
            }
            
            path = "M2_REPO/" + toRelative( localRepository, artifactPath.getPath() );
        }

        writer.startElement( "classpathentry" );

        writer.addAttribute( "kind", kind );

        writer.addAttribute( "path", path );

        writer.endElement();
    }
    
    private String getProjectPath( List reactorProjects, Artifact artifact )
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
                return "/" + project.getArtifactId();
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
