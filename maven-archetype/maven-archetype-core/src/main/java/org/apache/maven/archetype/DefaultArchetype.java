package org.apache.maven.archetype;

import org.apache.maven.archetype.descriptor.ArchetypeDescriptor;
import org.apache.maven.archetype.descriptor.ArchetypeDescriptorBuilder;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.velocity.VelocityComponent;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.net.URLClassLoader;
import java.net.URL;

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

    private WagonManager wagonManager;

    private ArtifactResolver artifactResolver;

    // ----------------------------------------------------------------------
    // Implementation
    // ----------------------------------------------------------------------

    // groupId = maven
    // artifactId = maven-foo-archetype
    // version = latest

    public void createArchetype( String archetypeId, ArtifactRepository localRepository, Set remoteRepositories, Map parameters )
        throws ArchetypeNotFoundException, ArchetypeDescriptorException, ArchetypeTemplateProcessingException
    {
        Artifact archetypeJar = wagonManager.createArtifact( "maven", "maven-archetype-" + archetypeId, "1.0-alpha-1-SNAPSHOT", "jar" );

        try
        {
            artifactResolver.resolve( archetypeJar, remoteRepositories, localRepository );
        }
        catch ( Exception e )
        {
            throw new ArchetypeNotFoundException( "Cannot download archetype.", e );
        }

        String outputDirectory = (String) parameters.get( "outputDirectory" );

        String packageName = (String) parameters.get( "package" );

        createProjectDirectoryStructure( outputDirectory );

        ArchetypeDescriptorBuilder builder = new ArchetypeDescriptorBuilder();

        ArchetypeDescriptor descriptor = null;

        URLClassLoader archetypeJarLoader;

        try
        {
            URL[] urls = new URL[1];

            urls[0] = archetypeJar.getFile().toURL();

            archetypeJarLoader = new URLClassLoader( urls );

            InputStream is = getStream( ARCHETYPE_DESCRIPTOR, archetypeJarLoader );

            if ( is == null )
            {
                throw new ArchetypeDescriptorException( "The " + ARCHETYPE_DESCRIPTOR + " descriptor cannot be found." );
            }

            descriptor = (ArchetypeDescriptor) builder.build( new InputStreamReader( is ) );
        }
        catch ( Exception e )
        {
            throw new ArchetypeDescriptorException( "Error reading the " + ARCHETYPE_DESCRIPTOR + " descriptor.", e );
        }

        Context context = new VelocityContext();

        context.put( "package", packageName );

        for ( Iterator iterator = parameters.keySet().iterator(); iterator.hasNext(); )
        {
            String key = (String) iterator.next();

            Object value = parameters.get( key );

            context.put( key, value );
        }

        try
        {
            ClassLoader old = Thread.currentThread().getContextClassLoader();

            Thread.currentThread().setContextClassLoader( archetypeJarLoader );

            processTemplate( outputDirectory, context, ARCHETYPE_POM, null );

            processSources( outputDirectory, context, descriptor.getSources(), packageName );

            processSources( outputDirectory, context, descriptor.getTestSources(), packageName );

            Thread.currentThread().setContextClassLoader( old );
        }
        catch ( Exception e )
        {
            throw new ArchetypeTemplateProcessingException( "Error processing templates.", e );
        }
    }

    protected void processSources( String outputDirectory, Context context, List sources, String packageName )
        throws Exception
    {
        for ( Iterator i = sources.iterator(); i.hasNext(); )
        {
            String template = (String) i.next();

            processTemplate( outputDirectory, context, template, packageName );
        }
    }

    protected void processTemplate( String outputDirectory, Context context, String template, String packageName )
        throws Exception
    {
        File f;

        if ( packageName != null )
        {
            String path = packageName.replace( '.', '/' );

            String filename = FileUtils.filename( template );

            String dirname = FileUtils.dirname( template );

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
        FileUtils.mkdir( outputDirectory + "/src/main/java" );

        FileUtils.mkdir( outputDirectory + "/src/main/resources" );

        FileUtils.mkdir( outputDirectory + "/src/test/java" );

        FileUtils.mkdir( outputDirectory + "/src/test/resources" );
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
