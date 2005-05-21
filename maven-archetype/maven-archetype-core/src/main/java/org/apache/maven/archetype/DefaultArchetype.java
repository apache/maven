package org.apache.maven.archetype;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import org.apache.maven.archetype.descriptor.ArchetypeDescriptor;
import org.apache.maven.archetype.descriptor.ArchetypeDescriptorBuilder;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.velocity.VelocityComponent;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class DefaultArchetype
    implements Archetype
{
    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private VelocityComponent velocity;

    private ArtifactResolver artifactResolver;

    // ----------------------------------------------------------------------
    // Implementation
    // ----------------------------------------------------------------------

    private ArtifactFactory artifactFactory;

    // groupId = maven
    // artifactId = maven-foo-archetype
    // version = latest

    public void createArchetype( String archetypeGroupId,
                                 String archetypeArtifactId,
                                 String archetypeVersion,
                                 ArtifactRepository localRepository,
                                 List remoteRepositories,
                                 Map parameters )
        throws ArchetypeNotFoundException, ArchetypeDescriptorException, ArchetypeTemplateProcessingException
    {
        // ----------------------------------------------------------------------
        // Download the archetype
        // ----------------------------------------------------------------------

        Artifact archetypeArtifact = artifactFactory.createArtifact( archetypeGroupId,
                                                                     archetypeArtifactId,
                                                                     archetypeVersion,
                                                                     Artifact.SCOPE_RUNTIME,
                                                                     "jar" );

        try
        {
            artifactResolver.resolve( archetypeArtifact, remoteRepositories, localRepository );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ArchetypeNotFoundException( "Cannot download archetype.", e );
        }

        // ----------------------------------------------------------------------
        // Load the descriptor
        // ----------------------------------------------------------------------

        String outputDirectory = (String) parameters.get( "outputDirectory" );

        String packageName = (String) parameters.get( "package" );

        ArchetypeDescriptorBuilder builder = new ArchetypeDescriptorBuilder();

        ArchetypeDescriptor descriptor = null;

        URLClassLoader archetypeJarLoader;

        try
        {
            URL[] urls = new URL[1];

            urls[0] = archetypeArtifact.getFile().toURL();

            archetypeJarLoader = new URLClassLoader( urls );

            InputStream is = getStream( ARCHETYPE_DESCRIPTOR, archetypeJarLoader );

            if ( is == null )
            {
                throw new ArchetypeDescriptorException( "The " + ARCHETYPE_DESCRIPTOR +
                                                        " descriptor cannot be found." );
            }

            descriptor = (ArchetypeDescriptor) builder.build( new InputStreamReader( is ) );
        }
        catch ( Exception e )
        {
            throw new ArchetypeDescriptorException( "Error reading the " + ARCHETYPE_DESCRIPTOR + " descriptor.", e );
        }

        // ----------------------------------------------------------------------
        // Set up the Velocity context
        // ----------------------------------------------------------------------

        Context context = new VelocityContext();

        context.put( "package", packageName );

        for ( Iterator iterator = parameters.keySet().iterator(); iterator.hasNext(); )
        {
            String key = (String) iterator.next();

            Object value = parameters.get( key );

            context.put( key, value );
        }

        // ----------------------------------------------------------------------
        // Process the templates
        // ----------------------------------------------------------------------

        ClassLoader old = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader( archetypeJarLoader );

        try
        {
            processTemplate( outputDirectory, context, ARCHETYPE_POM, false, null );

            // ----------------------------------------------------------------------
            // Main
            // ----------------------------------------------------------------------

            if ( descriptor.getSources().size() > 0 )
            {
                FileUtils.mkdir( outputDirectory + "/src/main/java" );

                processSources( outputDirectory, context, descriptor.getSources(), packageName );
            }

            if ( descriptor.getResources().size() > 0 )
            {
                FileUtils.mkdir( outputDirectory + "/src/main/resources" );

                processResources( outputDirectory, context, descriptor.getResources(), packageName );
            }

            // ----------------------------------------------------------------------
            // Test
            // ----------------------------------------------------------------------

            if ( descriptor.getTestSources().size() > 0 )
            {
                FileUtils.mkdir( outputDirectory + "/src/test/java" );

                processSources( outputDirectory, context, descriptor.getTestSources(), packageName );
            }

            if ( descriptor.getTestResources().size() > 0 )
            {
                FileUtils.mkdir( outputDirectory + "/src/test/resources" );

                processResources( outputDirectory, context, descriptor.getTestResources(), packageName );
            }

            // ----------------------------------------------------------------------
            // Site
            // ----------------------------------------------------------------------

            if ( descriptor.getSiteResources().size() > 0 )
            {
                processResources( outputDirectory, context, descriptor.getSiteResources(), packageName );
            }

        }
        catch ( Exception e )
        {
            throw new ArchetypeTemplateProcessingException( "Error processing templates.", e );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( old );
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected void processSources( String outputDirectory, Context context, List sources, String packageName )
        throws Exception
    {
        for ( Iterator i = sources.iterator(); i.hasNext(); )
        {
            String template = (String) i.next();

            processTemplate( outputDirectory, context, template, true, packageName );
        }
    }

    protected void processResources( String outputDirectory, Context context, List resources, String packageName )
        throws Exception
    {
        for ( Iterator i = resources.iterator(); i.hasNext(); )
        {
            String template = (String) i.next();

            processTemplate( outputDirectory, context, template, false, packageName );
        }
    }

    protected void processTemplate( String outputDirectory,
                                    Context context,
                                    String template,
                                    boolean packageInFileName,
                                    String packageName )
        throws Exception
    {
        File f;

        template = StringUtils.replace( template, "\\", "/" );

        if ( packageInFileName && packageName != null )
        {
            String templateFileName = StringUtils.replace( template, "/", File.separator );

            String path = packageName.replace( '.', '/' );

            String filename = FileUtils.filename( templateFileName );

            String dirname = FileUtils.dirname( templateFileName );

            f = new File( new File( new File( outputDirectory, dirname ), path ), filename );
        }
        else
        {
            f = new File( outputDirectory, template );
        }

        if ( !f.getParentFile().exists() )
        {
            f.getParentFile().mkdirs();
        }

        Writer writer = new FileWriter( f );

        template = ARCHETYPE_RESOURCES + "/" + template;

        velocity.getEngine().mergeTemplate( template, context, writer );

        writer.flush();

        writer.close();
    }

    protected void createProjectDirectoryStructure( String outputDirectory )
    {
    }

    private InputStream getStream( String name, ClassLoader loader )
    {
        if ( loader == null )
        {
            return Thread.currentThread().getContextClassLoader().getResourceAsStream( name );
        }

        return loader.getResourceAsStream( name );
    }
}
