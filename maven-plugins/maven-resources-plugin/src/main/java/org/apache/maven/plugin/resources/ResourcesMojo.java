package org.apache.maven.plugin.resources;

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

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.InterpolationFilterReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @goal resources
 * @phase process-resources
 * @description copy application resources
 */
public class ResourcesMojo
    extends AbstractMojo
{
    /**
     * The output directory into which to copy the resources.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private String outputDirectory;

    /**
     * The list of resources we want to transfer.
     *
     * @parameter expression="${project.build.resources}"
     * @required
     */
    private List resources;

    /**
     * Wheter to apply filters during transfer.
     *
     * @parameter
     */
    private boolean filtering = false;

    /**
     * The name of the filter property file to use.
     *
     * @parameter expression="${basedir}/filter.properties"
     */
    private File filterPropertiesFile;

    private Properties filterProperties;

    private static final String[] EMPTY_STRING_ARRAY = {};

    private static final String[] DEFAULT_INCLUDES = {"**/**"};

    public void execute()
        throws MojoExecutionException
    {
        initializeFiltering();
        copyResources( resources, outputDirectory );
    }

    protected void copyResources( List resources, String outputDirectory )
        throws MojoExecutionException
    {
        try
        {
            for ( Iterator i = getJarResources( resources ).entrySet().iterator(); i.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) i.next();
                File source = (File) entry.getKey();
                String destination = (String) entry.getValue();

                File destinationFile = new File( outputDirectory, destination );

                if ( !destinationFile.getParentFile().exists() )
                {
                    destinationFile.getParentFile().mkdirs();
                }

                copyFile( source, destinationFile );
            }
        }
        catch ( Exception e )
        {
            // TODO: handle exception
            throw new MojoExecutionException( "Error copying resources", e );
        }
    }

    private Map getJarResources( List resources )
    {
        Map resourceEntries = new TreeMap();

        for ( Iterator i = resources.iterator(); i.hasNext(); )
        {
            Resource resource = (Resource) i.next();

            String targetPath = resource.getTargetPath();

            File resourceDirectory = new File( resource.getDirectory() );

            if ( !resourceDirectory.exists() )
            {
                continue;
            }

            DirectoryScanner scanner = new DirectoryScanner();
            scanner.addDefaultExcludes();

            scanner.setBasedir( resource.getDirectory() );
            if ( resource.getIncludes() != null && !resource.getIncludes().isEmpty() )
            {
                scanner.setIncludes( (String[]) resource.getIncludes().toArray( EMPTY_STRING_ARRAY ) );
            }
            else
            {
                scanner.setIncludes( DEFAULT_INCLUDES );
            }
            if ( resource.getExcludes() != null && !resource.getExcludes().isEmpty() )
            {
                scanner.setExcludes( (String[]) resource.getExcludes().toArray( EMPTY_STRING_ARRAY ) );
            }

            scanner.scan();

            List includedFiles = Arrays.asList( scanner.getIncludedFiles() );
            for ( Iterator j = includedFiles.iterator(); j.hasNext(); )
            {
                String name = (String) j.next();

                String entryName = name;

                if ( targetPath != null )
                {
                    entryName = targetPath + "/" + name;
                }

                resourceEntries.put( new File( resource.getDirectory(), name ), entryName );
            }
        }

        return resourceEntries;
    }

    private void initializeFiltering()
        throws MojoExecutionException
    {
        if ( filtering )
        {
            try
            {
                filterProperties = PropertyUtils.loadPropertyFile( filterPropertiesFile, true, true );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error loading property file '" + filterPropertiesFile + "'", e );
            }
        }
    }

    private void copyFile( File from, File to )
        throws IOException
    {
        if ( !filtering )
        {
            FileUtils.copyFile( from, to );
        }
        else
        {
            // buffer so it isn't reading a byte at a time!
            Reader fileReader = new BufferedReader( new FileReader( from ) );
            Writer fileWriter = null;
            try
            {
                // support ${token}
                Reader reader = new InterpolationFilterReader( fileReader, filterProperties, "${", "}" );

                // support @token@
                reader = new InterpolationFilterReader( reader, filterProperties, "@", "@" );

                fileWriter = new FileWriter( to );

                IOUtil.copy( reader, fileWriter );
            }
            finally
            {
                IOUtil.close( fileReader );
                IOUtil.close( fileWriter );
            }
        }
    }
}
