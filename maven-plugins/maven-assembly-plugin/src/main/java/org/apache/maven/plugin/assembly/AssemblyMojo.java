package org.apache.maven.plugin.assembly;

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
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.plugins.assembly.model.io.xpp3.AssemblyXpp3Reader;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @goal assembly
 * @requiresDependencyResolution test
 * @description assemble an application bundle or distribution
 * @parameter name="basedir" type="String" required="true" validator="" expression="#basedir" description=""
 * @parameter name="outputDirectory" type="java.io.File" required="true" validator="" expression="#project.build.directory" description=""
 * @parameter name="descriptor" type="java.io.File" required="false" validator="" expression="#maven.assembly.descriptor" description=""
 * @parameter name="finalName" type="String" required="true" validator="" expression="#project.build.finalName" description=""
 * @parameter name="descriptorId" type="String" required="false" validator="" expression="#maven.assembly.descriptorId" description=""
 * @parameter name="dependencies" type="java.util.Set" required="false" validator="" expression="#project.artifacts" description=""
 */
public class AssemblyMojo
    extends AbstractPlugin
{
    private static final String[] EMPTY_STRING_ARRAY = {};

    private String basedir;

    /**
     * @todo use java.io.File
     */
    private String outputDirectory;

    private File descriptor;

    private String descriptorId;

    private String finalName;

    private Set dependencies;

    public void execute()
        throws PluginExecutionException
    {
        try
        {
            doExecute();
        }
        catch ( Exception e )
        {
            // TODO: don't catch exception
            throw new PluginExecutionException( "Error creating assembly", e );
        }
    }

    private void doExecute()
        throws Exception
    {
        Reader r = null;

        if ( descriptor != null )
        {
            r = new FileReader( descriptor );
        }
        else if ( descriptorId != null )
        {
            InputStream resourceAsStream = getClass().getResourceAsStream( "/assemblies/" + descriptorId + ".xml" );
            if ( resourceAsStream == null )
            {
                // TODO: better exception
                throw new Exception( "Descriptor with ID '" + descriptorId + "' not found" );
            }
            r = new InputStreamReader( resourceAsStream );
        }
        else
        {
            // TODO: better exception
            throw new Exception( "You must specify descriptor or descriptorId" );
        }

        try
        {
            AssemblyXpp3Reader reader = new AssemblyXpp3Reader();
            Assembly assembly = reader.read( r );

            // TODO: include dependencies marked for distribution under certain formats
            // TODO: how, might we plug this into an installer, such as NSIS?
            // TODO: allow file mode specifications?

            String fullName = finalName + "-" + assembly.getId();

            for ( Iterator i = assembly.getFormats().iterator(); i.hasNext(); )
            {
                String format = (String) i.next();

                String filename = fullName + "." + format;

                // TODO: use component roles? Can we do that in a mojo?
                Archiver archiver = createArchiver( format );

                processFileSets( archiver, assembly.getFileSets() );
                processDependencySets( archiver, assembly.getDependencySets() );

                archiver.setDestFile( new File( outputDirectory, filename ) );
                archiver.createArchive();
            }
        }
        finally
        {
            IOUtil.close( r );
        }
    }

    private void processDependencySets( Archiver archiver, List dependencySets )
        throws ArchiverException
    {
        for ( Iterator i = dependencySets.iterator(); i.hasNext(); )
        {
            DependencySet depedencySet = (DependencySet) i.next();
            String output = depedencySet.getOutputDirectory();
            output = getOutputDirectory( output );

            AndArtifactFilter filter = new AndArtifactFilter();
            filter.add( new ScopeArtifactFilter( depedencySet.getScope() ) );
            if ( !depedencySet.getIncludes().isEmpty() )
            {
                filter.add( new IncludesArtifactFilter( depedencySet.getIncludes() ) );
            }
            if ( !depedencySet.getExcludes().isEmpty() )
            {
                filter.add( new ExcludesArtifactFilter( depedencySet.getExcludes() ) );
            }

            // TODO: includes and excludes
            for ( Iterator j = dependencies.iterator(); j.hasNext(); )
            {
                Artifact artifact = (Artifact) j.next();

                if ( filter.include( artifact ) )
                {
                    archiver.addFile( artifact.getFile(), output + artifact.getFile().getName() );
                }
            }
        }
    }

    private String getOutputDirectory( String output )
    {
        if ( output == null )
        {
            output = "";
        }
        if ( !output.endsWith( "/" ) && !output.endsWith( "\\" ) )
        {
            // TODO: shouldn't archiver do this?
            output += '/';
        }

        if ( output.startsWith( "/" ) )
        {
            output = finalName + output;
        }
        else
        {
            output = finalName + "/" + output;
        }
        return output;
    }

    private Archiver createArchiver( String format )
        throws ArchiverException
    {
        Archiver archiver;
        if ( format.startsWith( "tar" ) )
        {
            TarArchiver tarArchiver = new TarArchiver();
            archiver = tarArchiver;
            int index = format.indexOf( '.' );
            if ( index >= 0 )
            {
                // TODO: this needs a cleanup in plexus archiver - use a real typesafe enum
                TarArchiver.TarCompressionMethod tarCompressionMethod = new TarArchiver.TarCompressionMethod();
                // TODO: this should accept gz and bz2 as well so we can skip over the switch
                String compression = format.substring( index + 1 );
                if ( compression.equals( "gz" ) )
                {
                    tarCompressionMethod.setValue( "gzip" );
                }
                else if ( compression.equals( "bz2" ) )
                {
                    tarCompressionMethod.setValue( "bzip2" );
                }
                else
                {
                    // TODO: better handling
                    throw new IllegalArgumentException( "Unknown compression format: " + compression );
                }
                tarArchiver.setCompression( tarCompressionMethod );
            }

            // TODO: should be able to do this on a file/dir basis
            tarArchiver.getOptions().setDirMode( "0700" );
            tarArchiver.getOptions().setMode( "0700" );
        }
        else if ( format.startsWith( "zip" ) )
        {
            archiver = new ZipArchiver();
        }
        else if ( format.startsWith( "jar" ) )
        {
            // TODO: use MavenArchiver for manifest?
            archiver = new JarArchiver();
        }
        else
        {
            // TODO: better handling
            throw new IllegalArgumentException( "Unknown format: " + format );
        }
        return archiver;
    }

    private void processFileSets( Archiver archiver, java.util.List fileSets )
        throws ArchiverException
    {
        for ( Iterator i = fileSets.iterator(); i.hasNext(); )
        {
            FileSet fileSet = (FileSet) i.next();
            String directory = fileSet.getDirectory();
            String output = fileSet.getOutputDirectory();
            if ( directory == null )
            {
                directory = basedir;
                if ( output == null )
                {
                    output = "";
                }
            }
            else
            {
                if ( output == null )
                {
                    output = directory;
                }
            }
            output = getOutputDirectory( output );

            String[] includes = (String[]) fileSet.getIncludes().toArray( EMPTY_STRING_ARRAY );
            if ( includes.length == 0 )
            {
                includes = null;
            }

            List excludesList = fileSet.getExcludes();
            excludesList.addAll( getDefaultExcludes() );
            String[] excludes = (String[]) excludesList.toArray( EMPTY_STRING_ARRAY );

            // TODO: default excludes should be in the archiver?
            archiver.addDirectory( new File( directory ), output, includes, excludes );
        }
    }

    public List getDefaultExcludes()
    {
        List defaultExcludes = new ArrayList();
        defaultExcludes.add( "**/*~" );
        defaultExcludes.add( "**/#*#" );
        defaultExcludes.add( "**/.#*" );
        defaultExcludes.add( "**/%*%" );
        defaultExcludes.add( "**/._*" );

        // CVS
        defaultExcludes.add( "**/CVS" );
        defaultExcludes.add( "**/CVS/**" );
        defaultExcludes.add( "**/.cvsignore" );

        // SCCS
        defaultExcludes.add( "**/SCCS" );
        defaultExcludes.add( "**/SCCS/**" );

        // Visual SourceSafe
        defaultExcludes.add( "**/vssver.scc" );

        // Subversion
        defaultExcludes.add( "**/.svn" );
        defaultExcludes.add( "**/.svn/**" );

        // Mac
        defaultExcludes.add( "**/.DS_Store" );

        return defaultExcludes;
    }

    // TODO: move to maven-artifact - generally useful
    private static class AndArtifactFilter
        implements ArtifactFilter
    {
        private final List filters = new ArrayList();

        public boolean include( Artifact artifact )
        {
            boolean include = true;
            for ( Iterator i = filters.iterator(); i.hasNext() && include; )
            {
                ArtifactFilter filter = (ArtifactFilter) i.next();
                if ( !filter.include( artifact ) )
                {
                    include = false;
                }
            }
            return include;
        }

        public void add( ArtifactFilter artifactFilter )
        {
            filters.add( artifactFilter );
        }
    }

    private static class IncludesArtifactFilter
        implements ArtifactFilter
    {
        private final List patterns;

        public IncludesArtifactFilter( List patterns )
        {
            this.patterns = patterns;
        }

        public boolean include( Artifact artifact )
        {
            String id = artifact.getGroupId() + ":" + artifact.getArtifactId();

            boolean matched = false;
            for ( Iterator i = patterns.iterator(); i.hasNext() & !matched; )
            {
                // TODO: what about wildcards? Just specifying groups? versions?
                if ( id.equals( i.next() ) )
                {
                    matched = true;
                }
            }
            return matched;
        }
    }

    private static class ExcludesArtifactFilter
        extends IncludesArtifactFilter
    {
        public ExcludesArtifactFilter( List patterns )
        {
            super( patterns );
        }

        public boolean include( Artifact artifact )
        {
            return !super.include( artifact );
        }
    }
}
