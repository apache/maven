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
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
        assertNotEmpty( project.getGroupId(), "groupId" );

        assertNotEmpty( project.getArtifactId(), "artifactId" );

        File projectBaseDir = project.getFile().getParentFile();
        
        Map eclipseSourceRoots = new HashMap();

        Collection referencedProjects = writeEclipseClasspath(
        	projectBaseDir, outputDir, project, executedProject, reactorProjects, eclipseSourceRoots
        );
        
        writeEclipseProject( projectBaseDir, outputDir, project, executedProject, referencedProjects, eclipseSourceRoots );

        writeEclipseSettings( projectBaseDir, outputDir, project, executedProject);

        log.info( "Wrote Eclipse project for " + project.getArtifactId() + " to " + outputDir.getAbsolutePath() );
    }


    // ----------------------------------------------------------------------
    // .settings/
    // ----------------------------------------------------------------------

    private void writeEclipseSettings(
        File projectBaseDir, File outputDir, MavenProject project, MavenProject executedProject
    )
        throws EclipsePluginException
    {
        
        // check if it's necessary to create project specific settings
        
        Properties coreSettings = new Properties();
        
        // FIXME: need a better way to do this

        for ( Iterator it = project.getModel().getBuild().getPlugins().iterator(); it.hasNext(); )
        {
            Plugin plugin = (Plugin) it.next();
            
            if ( plugin.getArtifactId().equals( "maven-compiler-plugin" ) )
            {
                handleCompilerPlugin( plugin, coreSettings );
            }
        }
    
        // write the settings, if needed
        
        if ( !coreSettings.isEmpty() )
        {
            File settingsDir = new File( outputDir, "/.settings" );
            
            settingsDir.mkdirs();
            
            coreSettings.put( "eclipse.preferences.version", "1" );
            
            try
            {
                File coreSettingsFile = new File( settingsDir, "org.eclipse.jdt.core.prefs" );
                coreSettings.store( new FileOutputStream( coreSettingsFile ), null
                );
            
                log.info( "Wrote settings to " + coreSettingsFile );
            }
            catch (FileNotFoundException e)
            {
                throw new EclipsePluginException( "Cannot create settings file", e );
            }
            catch (IOException e)
            {
                throw new EclipsePluginException( "Error writing settings file", e );
            }
        }
        else
        {
            log.info( "Not writing settings - defaults suffice" );
        }
    }

    // ----------------------------------------------------------------------
    // .project
    // ----------------------------------------------------------------------

    protected void writeEclipseProject( File projectBaseDir, File basedir, MavenProject project, MavenProject executedProject, Collection referencedProjects, Map eclipseSourceRoots )
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
            
            addSourceLinks( writer, projectBaseDir, basedir, eclipseSourceRoots );

            writer.endElement(); // linkedResources
        }

        writer.endElement(); // projectDescription

        close( w );
    }

    // ----------------------------------------------------------------------
    // .classpath
    // ----------------------------------------------------------------------

    protected Collection writeEclipseClasspath( File projectBaseDir, File basedir, MavenProject project, MavenProject executedProject, List reactorProjects, Map eclipseSourceRoots )
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
                        null, eclipseSourceRoots );

        addResources( writer, projectBaseDir, basedir,
                      project.getBuild().getResources(),
                      null, eclipseSourceRoots );

        // ----------------------------------------------------------------------
        // The test sources and resources
        // ----------------------------------------------------------------------

        addSourceRoots( writer, projectBaseDir, basedir,
                        executedProject.getTestCompileSourceRoots(),
                        project.getBuild().getTestOutputDirectory(),
                        eclipseSourceRoots );

        addResources( writer, projectBaseDir, basedir,
                      project.getBuild().getTestResources(),
                      project.getBuild().getTestOutputDirectory(),
                      eclipseSourceRoots );

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
        
        Collection referencedProjects = new HashSet();

        Set artifacts = project.getArtifacts();

        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            addDependency( writer, artifact, reactorProjects, referencedProjects );
        }

        writer.endElement();

        close( w );
        
        return referencedProjects;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private void addSourceRoots( XMLWriter writer, File projectBaseDir, File basedir, List sourceRoots, String output, Map addedSourceRoots )
    {
        for ( Iterator it = sourceRoots.iterator(); it.hasNext(); )
        {
            String sourceRoot = (String) it.next();

            if ( new File( sourceRoot ).isDirectory() )
            {
                // Don't add the same sourceroots twice. No include/exclude
            	// patterns possible in maven for (test|script|)source directories.
                if ( addedSourceRoots.containsKey( sourceRoot ) )
                {
                	continue; 
                }

                writer.startElement( "classpathentry" );

                writer.addAttribute( "kind", "src" );

                String eclipseSourceRoot = toRelative( projectBaseDir, sourceRoot );
                
                if (!projectBaseDir.equals(basedir))
                {
                    eclipseSourceRoot = eclipseSourceRoot.replaceAll( "/", "-" );
                }
                
                addedSourceRoots.put( sourceRoot, eclipseSourceRoot );
                
                writer.addAttribute( "path", eclipseSourceRoot );

                if ( output != null )
                {
                    writer.addAttribute( "output", toRelative( projectBaseDir, output ) );
                }

                writer.endElement();
            }
        }
    }

    private void addResources( XMLWriter writer, File projectBaseDir, File basedir, List resources, String output, Map addedSourceRoots )
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
            	output = resource.getTargetPath();
            }

            File resourceDirectory = new File( resource.getDirectory() );

            if ( !resourceDirectory.exists() || !resourceDirectory.isDirectory() )
            {
                continue;
            }

            String resourceDir = resource.getDirectory();
            
            // don't add the same sourceroot twice; eclipse can't handle
            // that, even with mutual exclusive include/exclude patterns.
            if ( addedSourceRoots.containsKey( resourceDir ) )
            {
            	continue;
            }

            String eclipseResourceDir = toRelative( projectBaseDir, resourceDir );
            
            if ( ! projectBaseDir.equals( basedir ) )
            {
                eclipseResourceDir = eclipseResourceDir.replaceAll( "/", "-" );
            }

            addedSourceRoots.put( resourceDir, eclipseResourceDir );
            
            writer.startElement( "classpathentry" );

            writer.addAttribute( "kind", "src" );
            
            writer.addAttribute( "path", eclipseResourceDir );

//			Example of setting include/exclude patterns for future reference.
//
//          TODO: figure out how to merge if the same dir is specified twice
//          with different in/exclude patterns. We can't write them now,
//			since only the the first one would be included.
//
//          if ( resource.getIncludes().size() != 0 )
//          {
//          	writer.addAttribute(
//            		"including", StringUtils.join( resource.getIncludes().iterator(), "|" )
//        		);
//          }
//
//          if ( resource.getExcludes().size() != 0 )
//          {
//          	writer.addAttribute(
//          		"excluding", StringUtils.join( resource.getExcludes().iterator(), "|" )
//          	);
//          }
            
            if ( output != null )
            {
                writer.addAttribute( "output", toRelative( projectBaseDir, output ) );
            }

            writer.endElement();
        }
    }

    private void addSourceLinks( XMLWriter writer, File projectBaseDir, File basedir, Map sourceRoots )
    {
        for ( Iterator it = sourceRoots.keySet().iterator(); it.hasNext(); )
        {
            String sourceRoot = (String) it.next();
            
            String linkName = (String) sourceRoots.get( sourceRoot );
            
            sourceRoot = sourceRoot.replaceAll("\\\\", "/");
            
            log.debug( "Adding link '" + linkName + "' to '" + sourceRoot + "'" );

            if ( new File( sourceRoot ).isDirectory() )
            {
                writer.startElement( "link" );

                writer.startElement( "name" );

                writer.writeText( linkName );
                
                writer.endElement(); // name

                writer.startElement( "type" );

                writer.writeText( "2" );

                writer.endElement(); // type

                writer.startElement( "location" );

                writer.writeText( sourceRoot );

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
    private void addDependency( XMLWriter writer, Artifact artifact, List reactorProjects, Collection referencedProjects )
    {
        MavenProject reactorProject = findReactorProject( reactorProjects, artifact );

        String path = null;
        
        String kind = null;

        if ( reactorProject != null )
        {
            // if there's a dependency on multiple artifact attachments of the
            // same project, don't add it again.

            if ( !markAddedOnce( reactorProject, referencedProjects ) )
            {
                return;
            }

            path = "/" + reactorProject.getArtifactId();
            
            kind = "src";
        }
        else
        {
            File artifactPath = artifact.getFile();

            if ( artifactPath == null )
            {
                log.error( "The artifacts path was null. Artifact id: " + artifact.getId() );
    
                return;
            }
            
            path = "M2_REPO/" + toRelative( localRepository, artifactPath.getPath() );
            
            kind = "var";
        }

        writer.startElement( "classpathentry" );

        writer.addAttribute( "kind", kind );

        writer.addAttribute( "path", path );

        writer.endElement();
    }
    
    private static boolean markAddedOnce( MavenProject project, Collection referencedProjects )
    {
        if ( referencedProjects.contains( project ) )
        {
            return false;
        }
        else
        {
            referencedProjects.add( project );

            return true;
        }
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

    private static void close( Writer closeable )
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

    private static String toRelative( File basedir, String absolutePath )
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

    private static void assertNotEmpty( String string, String elementName )
        throws EclipsePluginException
    {
        if ( string == null )
        {
            throw new EclipsePluginException( "Missing element from the project descriptor: '" + elementName + "'." );
        }
    }

    private static void handleCompilerPlugin( Plugin plugin, Properties coreSettings )
    {
        Xpp3Dom pluginConfig = (Xpp3Dom) plugin.getConfiguration();

        if ( pluginConfig == null )
        {
            return;
        }

        String source = null;

        Xpp3Dom sourceChild = pluginConfig.getChild( "source" );

        if (sourceChild != null)
        {
            source = sourceChild.getValue();
        }

        String target = null;

        Xpp3Dom targetChild = pluginConfig.getChild( "target" );

        if (targetChild != null)
        {
            target = targetChild.getValue();
        }
        
        if ( source != null && !source.equals( "1.3" ) )
        {
            coreSettings.put( "org.eclipse.jdt.core.compiler.source", source );

            coreSettings.put( "org.eclipse.jdt.core.compiler.compliance", source );
        }

        if ( target != null && !target.equals( "1.2" ) )
        {
            coreSettings.put( "org.eclipse.jdt.core.compiler.codegen.targetPlatform", target );
        }
    }
}
