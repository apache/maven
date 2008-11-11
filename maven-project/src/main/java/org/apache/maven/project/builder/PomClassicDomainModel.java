package org.apache.maven.project.builder;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.shared.model.InputStreamDomainModel;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

/**
 * Provides a wrapper for the maven model.
 */
public final class PomClassicDomainModel
    implements InputStreamDomainModel
{

    /**
     * Bytes containing the underlying model
     */
    private byte[] inputBytes;

    /**
     * History of joins and deletes of model properties
     */
    private String eventHistory;

    /**
     * Maven model
     */
    private Model model;

    private String id;

    private File file;

    private File parentFile;

    private File projectDirectory;

    /**
     * Constructor
     *
     * @param model maven model
     * @throws IOException if there is a problem constructing the model
     */
    public PomClassicDomainModel( Model model )
        throws IOException
    {
        if ( model == null )
        {
            throw new IllegalArgumentException( "model: null" );
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer out = null;
        MavenXpp3Writer writer = new MavenXpp3Writer();
        try
        {
            out = WriterFactory.newXmlWriter( baos );
            writer.write( out, model );
        }
        finally
        {
            if ( out != null )
            {
                out.close();
            }
        }
        inputBytes = baos.toByteArray();
    }

    /**
     * Constructor
     *
     * @param inputStream input stream of the maven model
     * @throws IOException if there is a problem constructing the model
     */
    public PomClassicDomainModel( InputStream inputStream )
        throws IOException
    {
        if ( inputStream == null )
        {
            throw new IllegalArgumentException( "inputStream: null" );
        }
        this.inputBytes = IOUtil.toByteArray( inputStream );
    }

    public PomClassicDomainModel( File file )
        throws IOException
    {
        this( new FileInputStream( file ) );
        this.file = file;
    }

    public File getParentFile()
    {
        return parentFile;
    }

    public void setParentFile( File parentFile )
    {
        this.parentFile = parentFile;
    }
    
    public void setProjectDirectory(File projectDirectory)
    {
        this.projectDirectory = projectDirectory;
    }

    public File getProjectDirectory()
    {
        return projectDirectory;
    }

    public boolean isPomInBuild()
    {
        return projectDirectory != null;
    }

    /**
     * Returns true if groupId.equals(a.groupId) && artifactId.equals(a.artifactId) && version.equals(a.version),
     * otherwise returns false.
     *
     * @param a model to compare
     * @return true if groupId.equals(a.groupId) && artifactId.equals(a.artifactId) && version.equals(a.version),
     *         otherwise returns false.
     */
    public boolean matchesModel( Model a )
    {
        if ( a == null )
        {
            throw new IllegalArgumentException( "a: null" );
        }
        if ( model == null )
        {
            try
            {
                model = getModel();
            }
            catch ( IOException e )
            {
                return false;
            }
        }
        return a.getId().equals( this.getId() );
    }

    public String getId()
    {
        if ( id == null )
        {
            if ( model == null )
            {
                try
                {
                    model = getModel();
                }
                catch ( IOException e )
                {
                    return "";
                }
            }
            String groupId = ( model.getGroupId() == null && model.getParent() != null )
                ? model.getParent().getGroupId()
                : model.getGroupId();
            String artifactId = ( model.getArtifactId() == null && model.getParent() != null )
                ? model.getParent().getArtifactId()
                : model.getArtifactId();
            String version = ( model.getVersion() == null && model.getParent() != null )
                ? model.getParent().getVersion()
                : model.getVersion();

            id = groupId + ":" + artifactId + ":" + version;
        }
        return id;
    }


    public boolean matchesParent( Parent parent )
    {
        if ( parent == null )
        {
            throw new IllegalArgumentException( "parent: null" );
        }
        return getId().equals( parent.getGroupId() + ":" + parent.getArtifactId() + ":" + parent.getVersion() );
    }

    /**
     * Returns XML model as string
     *
     * @return XML model as string
     */
    public String asString()
    {
        try
        {
            return IOUtil.toString( ReaderFactory.newXmlReader( new ByteArrayInputStream( inputBytes ) ) );
        }
        catch ( IOException ioe )
        {
            // should not occur: everything is in-memory
            return "";
        }
    }

    /**
     * Returns maven model
     *
     * @return maven model
     */
    public Model getModel()
        throws IOException
    {
        if ( model != null )
        {
            return model;
        }
        try
        {
            return new MavenXpp3Reader().read( ReaderFactory.newXmlReader( new ByteArrayInputStream( inputBytes ) ) );
        }
        catch ( XmlPullParserException e )
        {
            e.printStackTrace();
            throw new IOException( e.getMessage() );
        }
    }

    /**
     * @see org.apache.maven.shared.model.InputStreamDomainModel#getInputStream()
     */
    public InputStream getInputStream()
    {
        byte[] copy = new byte[inputBytes.length];
        System.arraycopy( inputBytes, 0, copy, 0, inputBytes.length );
        return new ByteArrayInputStream( copy );
    }

    /**
     * @return file of pom. May be null.
     */
    public File getFile()
    {
        return file;
    }

    /**
     * @see org.apache.maven.shared.model.DomainModel#getEventHistory()
     */
    public String getEventHistory()
    {
        return eventHistory;
    }

    /**
     * @see org.apache.maven.shared.model.DomainModel#setEventHistory(String)
     */
    public void setEventHistory( String eventHistory )
    {
        if ( eventHistory == null )
        {
            throw new IllegalArgumentException( "eventHistory: null" );
        }
        this.eventHistory = eventHistory;
    }

    /**
     * Returns true if this.asString.equals(o.asString()), otherwise false.
     *
     * @param o domain model
     * @return true if this.asString.equals(o.asString()), otherwise false.
     */
    public boolean equals( Object o )
    {
        return o instanceof PomClassicDomainModel && getId().equals( ( (PomClassicDomainModel) o ).getId() );
    }

}
