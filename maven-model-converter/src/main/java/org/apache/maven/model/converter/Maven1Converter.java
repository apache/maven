package org.apache.maven.model.converter;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.maven.model.Model;
import org.apache.maven.model.converter.plugins.PluginConfigurationConverter;
import org.apache.maven.model.converter.relocators.PluginRelocator;
import org.apache.maven.model.converter.relocators.PluginRelocatorManager;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.v3_0_0.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Converts a Maven 1 project.xml (v3 pom) to a Maven 2 pom.xml (v4 pom).
 *
 * @author Fabrizio Giustina
 * @author Dennis Lundberg
 * @version $Id$
 * @plexus.component role="org.apache.maven.model.converter.Maven1Converter"
 */
public class Maven1Converter
    extends AbstractLogEnabled
{
    /**
     * Available converters for specific plugin configurations
     *
     * @plexus.requirement role="org.apache.maven.model.converter.plugins.PluginConfigurationConverter"
     */
    private List converters;

    /**
     * Plexus component that manages plugin relocators
     *
     * @plexus.requirement
     */
    private PluginRelocatorManager pluginRelocatorManager;

    private File basedir;

    private File outputdir;

    private String fileName = "project.xml";

    private List listeners = new ArrayList();

    public void execute()
        throws ProjectConverterException
    {
        File projectxml = new File( basedir, fileName );

        if ( !projectxml.exists() )
        {
            throw new ProjectConverterException( "Missing " + fileName + " in " + basedir.getAbsolutePath() );
        }

        PomV3ToV4Translator translator = new PomV3ToV4Translator();

        org.apache.maven.model.v3_0_0.Model v3Model;
        try
        {
            v3Model = loadV3Pom( projectxml );
        }
        catch ( Exception e )
        {
            throw new ProjectConverterException( "Exception caught while loading " + fileName + ". " + e.getMessage(), e );
        }

        Model v4Model;
        try
        {
            v4Model = translator.translate( v3Model );
            removeDistributionManagementStatus( v4Model );
        }
        catch ( Exception e )
        {
            throw new ProjectConverterException( "Exception caught while converting " + fileName + ". " + e.getMessage(),
                                                 e );
        }

        Properties properties = new Properties();

        if ( v3Model.getExtend() != null )
        {
            loadProperties( properties, new File( new File( basedir, v3Model.getExtend() ).getParentFile(),
                                                  "project.properties" ) );
        }

        loadProperties( properties, new File( basedir, "project.properties" ) );

        for ( Iterator i = converters.iterator(); i.hasNext(); )
        {
            PluginConfigurationConverter converter = (PluginConfigurationConverter) i.next();
            converter.convertConfiguration( v4Model, v3Model, properties );
        }

        // @todo Should this be run before or after the configuration converters?
        Collection pluginRelocators = pluginRelocatorManager.getPluginRelocators();
        sendInfoMessage( "There are " + pluginRelocators.size() + " plugin relocators available" );
        PluginRelocator pluginRelocator;
        Iterator iterator = pluginRelocators.iterator();
        while ( iterator.hasNext() )
        {
            pluginRelocator = (PluginRelocator) iterator.next();
            pluginRelocator.relocate( v4Model );
        }

        try
        {
            writeV4Pom( v4Model );
        }
        catch ( IOException e )
        {
            throw new ProjectConverterException( "Failed to write the pom.xml.", e );
        }
    }

    private boolean isEmpty( String value )
    {
        return value == null || value.trim().length() == 0;
    }

    private void loadProperties( Properties properties, File propertiesFile )
    {
        if ( propertiesFile.exists() )
        {
            InputStream is = null;
            try
            {
                is = new FileInputStream( propertiesFile );
                properties.load( is );
            }
            catch ( IOException e )
            {
                sendWarnMessage( "Unable to read " + propertiesFile.getAbsolutePath() + ", ignoring." );
            }
            finally
            {
                IOUtil.close( is );
            }
        }
    }

    private org.apache.maven.model.v3_0_0.Model loadV3Pom( File inputFile )
        throws Exception
    {
        MavenXpp3Reader v3Reader = new MavenXpp3Reader();

        org.apache.maven.model.v3_0_0.Model model;

        model = v3Reader.read( new FileReader( inputFile ) );

        SAXReader r = new SAXReader();

        Document d = r.read( new FileReader( inputFile ) );

        Element root = d.getRootElement();

        Element idElement = root.element( "id" );

        String id = null;

        if ( idElement != null )
        {
            id = idElement.getText();
        }
        //        String id = model.getId();

        String groupId = model.getGroupId();

        String artifactId = model.getArtifactId();

        if ( !isEmpty( id ) )
        {
            int i = id.indexOf( "+" );

            int j = id.indexOf( ":" );

            if ( i > 0 )
            {
                model.setGroupId( id.substring( 0, i ) );

                model.setArtifactId( id.replace( '+', '-' ) );
            }
            else if ( j > 0 )
            {
                model.setGroupId( id.substring( 0, j ) );

                model.setArtifactId( id.substring( j + 1 ) );
            }
            else
            {
                model.setGroupId( id );

                model.setArtifactId( id );
            }

            if ( !isEmpty( groupId ) )
            {
                sendWarnMessage( "Both <id> and <groupId> is set, using <groupId>." );

                model.setGroupId( groupId );
            }

            if ( !isEmpty( artifactId ) )
            {
                sendWarnMessage( "Both <id> and <artifactId> is set, using <artifactId>." );

                model.setArtifactId( artifactId );
            }
        }

        return model;
    }

    /**
     * The status element of the distributionManagement section must not be
     * set in local projects. This method removes that element from the model.
     */
    private void removeDistributionManagementStatus( Model v4Model )
    {
        if ( v4Model.getDistributionManagement() != null )
        {
            if ( "converted".equals( v4Model.getDistributionManagement().getStatus() ) )
            {
                v4Model.getDistributionManagement().setStatus( null );
            }
        }
    }

    /**
     * Write the pom to <code>${basedir}/pom.xml</code>. If the file exists it
     * will be overwritten.
     *
     * @param v4Model
     * @throws ProjectConverterException
     */
    private void writeV4Pom( Model v4Model )
        throws ProjectConverterException, IOException
    {
        if ( outputdir == null )
        {
            outputdir = basedir;
        }

        if ( !outputdir.exists() && !outputdir.mkdirs() )
        {
            throw new IOException( "Failed to create directory " + outputdir );
        }

        File pomxml = new File( outputdir, "pom.xml" );

        if ( pomxml.exists() )
        {
            sendWarnMessage( "pom.xml in " + outputdir.getAbsolutePath() + " already exists, overwriting" );
        }

        MavenXpp3Writer v4Writer = new MavenXpp3Writer();

        // write the new pom.xml
        sendInfoMessage( "Writing new pom to: " + pomxml.getAbsolutePath() );

        Writer output = null;
        try
        {
            output = new FileWriter( pomxml );
            v4Writer.write( output, v4Model );
            output.close();
        }
        catch ( IOException e )
        {
            throw new ProjectConverterException( "Unable to write pom.xml. " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( output );
        }
    }

    public File getBasedir()
    {
        return basedir;
    }

    public void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

    public String getProjectFileName()
    {
        return fileName;
    }

    public void setProjectFileName( String projectFileName )
    {
        this.fileName = projectFileName;
    }

    public void setProjectFile( File projectFile )
    {
        if ( projectFile != null )
        {
            basedir = projectFile.getParentFile();
            fileName = projectFile.getName();
        }
    }

    public File getOutputdir()
    {
        return outputdir;
    }

    public void setOutputdir( File outputdir )
    {
        this.outputdir = outputdir;
    }

    public void addListener( ConverterListener listener )
    {
        if ( !listeners.contains( listener ) )
        {
            listeners.add( listener );
        }
    }

    private void sendInfoMessage( String message )
    {
        getLogger().info( message );
        
        for ( Iterator i = listeners.iterator(); i.hasNext(); )
        {
            ConverterListener listener = (ConverterListener) i.next();
            listener.info( message );
        }
    }

    private void sendWarnMessage( String message )
    {
        getLogger().warn( message );
        
        for ( Iterator i = listeners.iterator(); i.hasNext(); )
        {
            ConverterListener listener = (ConverterListener) i.next();
            listener.warn( message );
        }
    }
}
