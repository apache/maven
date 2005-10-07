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
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.velocity.VelocityComponent;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class DefaultArchetype
    extends AbstractLogEnabled
    implements Archetype
{
    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private static final String DEFAULT_TEST_RESOURCE_DIR = "/src/test/resources";

    private static final String DEFAULT_TEST_SOURCE_DIR = "/src/test/java";

    private static final String DEFAULT_RESOURCE_DIR = "/src/main/resources";

    private static final String DEFAULT_SOURCE_DIR = "/src/main/java";


    private VelocityComponent velocity;

    private ArtifactResolver artifactResolver;

    // ----------------------------------------------------------------------
    // Implementation
    // ----------------------------------------------------------------------

    private ArtifactFactory artifactFactory;

    // groupId = maven
    // artifactId = maven-foo-archetype
    // version = latest

    public void createArchetype( String archetypeGroupId, String archetypeArtifactId, String archetypeVersion,
                                 ArtifactRepository localRepository, List remoteRepositories, Map parameters )
        throws ArchetypeNotFoundException, ArchetypeDescriptorException, ArchetypeTemplateProcessingException
    {
        // ----------------------------------------------------------------------
        // Download the archetype
        // ----------------------------------------------------------------------

        Artifact archetypeArtifact = artifactFactory.createArtifact( archetypeGroupId, archetypeArtifactId,
                                                                     archetypeVersion, Artifact.SCOPE_RUNTIME, "jar" );

        try
        {
            artifactResolver.resolve( archetypeArtifact, remoteRepositories, localRepository );
        }
        catch ( ArtifactResolutionException e )
        {
            // TODO: this is an error now, not "not found"
            throw new ArchetypeNotFoundException( "Cannot download archetype.", e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new ArchetypeNotFoundException( "Cannot download archetype.", e );
        }

        // ---------------------------------------------------------------------
        // Get Logger and display all parameters used 
        // ---------------------------------------------------------------------
        if ( getLogger().isInfoEnabled() )
        {
            if ( !parameters.isEmpty() )
            {
                getLogger().info( "----------------------------------------------------------------------------" );

                getLogger().info( "Using following parameters for creating Archetype: " + archetypeArtifactId + ":" +
                    archetypeVersion );

                getLogger().info( "----------------------------------------------------------------------------" );

                Set keys = parameters.keySet();

                Iterator it = keys.iterator();

                while ( it.hasNext() )
                {
                    String parameterName = (String) it.next();

                    String parameterValue = (String) parameters.get( parameterName );

                    getLogger().info( "Parameter: " + parameterName + ", Value: " + parameterValue );
                }
            }
            else
            {
                getLogger().info( "No Parameters found for creating Archetype" );
            }
        }

        // ----------------------------------------------------------------------
        // Load the descriptor
        // ----------------------------------------------------------------------

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
                throw new ArchetypeDescriptorException(
                    "The " + ARCHETYPE_DESCRIPTOR + " descriptor cannot be found." );
            }

            descriptor = (ArchetypeDescriptor) builder.build( new InputStreamReader( is ) );
        }
        catch ( Exception e )
        {
            throw new ArchetypeDescriptorException( "Error reading the " + ARCHETYPE_DESCRIPTOR + " descriptor.", e );
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        String basedir = (String) parameters.get( "basedir" );

        String artifactId = (String) parameters.get( "artifactId" );

        File pomFile = new File( basedir, ARCHETYPE_POM );

        File outputDirectoryFile;

        if ( pomFile.exists() && descriptor.isAllowPartial() )
        {
            outputDirectoryFile = new File( basedir );
        }
        else
        {
            outputDirectoryFile = new File( basedir, artifactId );

            if ( outputDirectoryFile.exists() )
            {
                throw new ArchetypeTemplateProcessingException(
                    outputDirectoryFile.getName() + " already exists - please run from a clean directory" );
            }

            pomFile = new File( outputDirectoryFile, ARCHETYPE_POM );
        }

        String outputDirectory = outputDirectoryFile.getAbsolutePath();

        String packageName = (String) parameters.get( "package" );

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
            if ( !pomFile.exists() )
            {
                processTemplate( outputDirectory, context, ARCHETYPE_POM, false, null );
            }

            // ---------------------------------------------------------------------
            // Model generated for the new archetype, so process it now
            // ---------------------------------------------------------------------

            FileReader pomReader = new FileReader( pomFile );

            MavenXpp3Reader reader = new MavenXpp3Reader();

            Model generatedModel = reader.read( pomReader );

            // XXX: Following POM processing block may be a candidate for 
            // refactoring out into service methods or moving to 
            // createProjectDirectoryStructure(outputDirectory)
            Build build = null;

            boolean overrideSrcDir = false;

            boolean overrideScriptSrcDir = false;

            boolean overrideResourceDir = false;

            boolean overrideTestSrcDir = false;

            boolean overrideTestResourceDir = false;

            boolean foundBuildElement = ( null != ( build = generatedModel.getBuild() ) );

            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug(
                    "********************* Debug info for resources created from generated Model ***********************" );
            }

            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "Was build element found in generated POM?: " + foundBuildElement );
            }

            // create source directory if specified in POM
            if ( foundBuildElement && null != build.getSourceDirectory() )
            {
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "Overriding default source directory " );
                }

                overrideSrcDir = true;

                String srcDirectory = build.getSourceDirectory();

                srcDirectory = StringUtils.replace( srcDirectory, "\\", "/" );

                FileUtils.mkdir(
                    outputDirectory + ( srcDirectory.startsWith( "/" ) ? srcDirectory : ( "/" + srcDirectory ) ) );
            }

            // create script source directory if specified in POM
            if ( foundBuildElement && null != build.getScriptSourceDirectory() )
            {
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "Overriding default script source directory " );
                }

                overrideScriptSrcDir = true;

                String scriptSourceDirectory = build.getScriptSourceDirectory();

                scriptSourceDirectory = StringUtils.replace( scriptSourceDirectory, "\\", "/" );

                FileUtils.mkdir( outputDirectory + ( scriptSourceDirectory.startsWith( "/" ) ? scriptSourceDirectory
                    : ( "/" + scriptSourceDirectory ) ) );
            }

            // create resource director(y/ies) if specified in POM
            if ( foundBuildElement && build.getResources().size() > 0 )
            {
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().info( "Overriding default resource directory " );
                }

                overrideResourceDir = true;

                Iterator resourceItr = build.getResources().iterator();

                while ( resourceItr.hasNext() )
                {
                    Resource resource = (Resource) resourceItr.next();

                    String resourceDirectory = resource.getDirectory();

                    resourceDirectory = StringUtils.replace( resourceDirectory, "\\", "/" );

                    FileUtils.mkdir( outputDirectory +
                        ( resourceDirectory.startsWith( "/" ) ? resourceDirectory : ( "/" + resourceDirectory ) ) );
                }
            }
            // create test source directory if specified in POM
            if ( foundBuildElement && null != build.getTestSourceDirectory() )
            {
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "Overriding default test directory " );
                }

                overrideTestSrcDir = true;

                String testDirectory = build.getTestSourceDirectory();

                testDirectory = StringUtils.replace( testDirectory, "\\", "/" );

                FileUtils.mkdir(
                    outputDirectory + ( testDirectory.startsWith( "/" ) ? testDirectory : ( "/" + testDirectory ) ) );
            }

            // create test resource directory if specified in POM
            if ( foundBuildElement && build.getTestResources().size() > 0 )
            {
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "Overriding default test resource directory " );
                }

                overrideTestResourceDir = true;

                Iterator testResourceItr = build.getTestResources().iterator();

                while ( testResourceItr.hasNext() )
                {
                    Resource resource = (Resource) testResourceItr.next();

                    String testResourceDirectory = resource.getDirectory();

                    testResourceDirectory = StringUtils.replace( testResourceDirectory, "\\", "/" );

                    FileUtils.mkdir( outputDirectory + ( testResourceDirectory.startsWith( "/" ) ? testResourceDirectory
                        : ( "/" + testResourceDirectory ) ) );
                }
            }

            getLogger().info(
                "********************* End of debug info from resources from generated POM ***********************" );

            // ----------------------------------------------------------------------
            // Main
            // ----------------------------------------------------------------------

            if ( descriptor.getSources().size() > 0 )
            {
                if ( !overrideSrcDir )
                {
                    FileUtils.mkdir( outputDirectory + DEFAULT_SOURCE_DIR );
                }
                processSources( outputDirectory, context, descriptor.getSources(), packageName );
            }

            if ( descriptor.getResources().size() > 0 )
            {
                if ( !overrideResourceDir )
                {
                    FileUtils.mkdir( outputDirectory + DEFAULT_RESOURCE_DIR );
                }
                processResources( outputDirectory, context, descriptor.getResources(), packageName );
            }

            // ----------------------------------------------------------------------
            // Test
            // ----------------------------------------------------------------------

            if ( descriptor.getTestSources().size() > 0 )
            {
                if ( !overrideTestSrcDir )
                {
                    FileUtils.mkdir( outputDirectory + DEFAULT_TEST_SOURCE_DIR );
                }

                processSources( outputDirectory, context, descriptor.getTestSources(), packageName );
            }

            if ( descriptor.getTestResources().size() > 0 )
            {
                if ( !overrideTestResourceDir )
                {
                    FileUtils.mkdir( outputDirectory + DEFAULT_TEST_RESOURCE_DIR );
                }
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

        // ----------------------------------------------------------------------
        // Log message on Archetype creation
        // ----------------------------------------------------------------------
        if ( getLogger().isInfoEnabled() )
        {
            getLogger().info( "Archetype created in dir: " + outputDirectory );
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

    protected void processTemplate( String outputDirectory, Context context, String template, boolean packageInFileName,
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
