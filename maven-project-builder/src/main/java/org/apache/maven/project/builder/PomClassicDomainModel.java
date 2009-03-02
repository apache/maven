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

import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.ModelMarshaller;
import org.apache.maven.shared.model.InputStreamDomainModel;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * Provides a wrapper for the maven model.
 */
public class PomClassicDomainModel implements InputStreamDomainModel
{

    /**
     * Bytes containing the underlying model
     */
    private byte[] inputBytes;

    /**
     * History of joins and deletes of model properties
     */
    private String eventHistory;

    private String id;

    private File file;

    private File parentFile;

    private File projectDirectory;

    private List<ModelProperty> modelProperties;
    
    private int lineageCount;

    private String parentGroupId = null, parentArtifactId = null, parentVersion = null, parentId = null, parentRelativePath;

    public PomClassicDomainModel( List<ModelProperty> modelProperties )
    {
        this.modelProperties = modelProperties;
        try
        {
            String xml = ModelMarshaller.unmarshalModelPropertiesToXml( modelProperties, ProjectUri.baseUri );
            inputBytes = xml.getBytes( "UTF-8" );
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( "Unmarshalling of model properties failed", e );
        }
        initializeProperties( modelProperties );
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
        modelProperties = getModelProperties();
        initializeProperties( modelProperties );
    }

    private void initializeProperties(List<ModelProperty> modelProperties)
    {
        String groupId = null, artifactId = null, version = null;
        for(ModelProperty mp : modelProperties)
        {
            if(mp.getUri().equals(ProjectUri.groupId))
            {
                groupId = mp.getResolvedValue();
            }
            else if(mp.getUri().equals(ProjectUri.artifactId))
            {
                artifactId = mp.getResolvedValue();
            }
            else if(mp.getUri().equals(ProjectUri.version))
            {
                version = mp.getResolvedValue();
            }
            else if(mp.getUri().equals(ProjectUri.Parent.artifactId))
            {
                parentArtifactId = mp.getResolvedValue();
            }
            else if(mp.getUri().equals(ProjectUri.Parent.groupId))
            {
                parentGroupId = mp.getResolvedValue();
            }
            else if(mp.getUri().equals(ProjectUri.Parent.version))
            {
                parentVersion = mp.getResolvedValue();
            }
            else if(mp.getUri().equals(ProjectUri.Parent.relativePath))
            {
                parentRelativePath = mp.getResolvedValue();
            }

            if(groupId != null && artifactId != null && version != null && parentGroupId != null &&
                    parentArtifactId != null && parentVersion != null & parentRelativePath != null)
            {
                break;
            }
        }
            if( groupId == null && parentGroupId != null)
            {
                groupId = parentGroupId;
            }
            if( artifactId == null && parentArtifactId != null)
            {
                artifactId = parentArtifactId;
            }
            if( version == null && parentVersion != null )
            {
                version = parentVersion;
            }

        if(parentGroupId != null && parentArtifactId != null && parentVersion != null)
        {
            parentId = parentGroupId + ":" + parentArtifactId + ":" + parentVersion;
        }

        if(parentRelativePath == null)
        {
            parentRelativePath = ".." + File.separator + "pom.xml";
        }

        id = groupId + ":" + artifactId + ":" + version;
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

    public String getParentGroupId() {
        return parentGroupId;
    }

    public String getParentArtifactId() {
        return parentArtifactId;
    }

    public String getParentVersion() {
        return parentVersion;
    }

    /**
     * This should only be set for projects that are in the build. Setting for poms in the repo may cause unstable behavior.
     * 
     * @param projectDirectory
     */
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

    public String getParentId() throws IOException
    {
        return parentId;
    }

    public String getRelativePathOfParent()
    {
        return parentRelativePath;
    }

    public String getId() throws IOException
    {
        return id;
    }


    public boolean matchesParentOf( PomClassicDomainModel domainModel ) throws IOException
    {
        if ( domainModel == null )
        {
            throw new IllegalArgumentException( "domainModel: null" );
        }

        return getId().equals(domainModel.getParentId());
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

    public List<ModelProperty> getModelProperties() throws IOException
    {
        if(modelProperties == null)
        {
            Set<String> s = new HashSet<String>();
            //TODO: Should add all collections from ProjectUri
            s.addAll(PomTransformer.URIS);
            s.add(ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.xUri);
            s.add(ProjectUri.DependencyManagement.Dependencies.Dependency.Exclusions.xUri);
            s.add(ProjectUri.Dependencies.Dependency.Exclusions.xUri);
            s.add(ProjectUri.Build.Plugins.Plugin.Executions.xUri);
            s.add(ProjectUri.Build.Plugins.Plugin.Executions.Execution.Goals.xURI);
            s.add(ProjectUri.Reporting.Plugins.Plugin.ReportSets.xUri);
            s.add(ProjectUri.Reporting.Plugins.Plugin.ReportSets.ReportSet.configuration);
            s.add(ProjectUri.Build.Plugins.Plugin.Executions.Execution.configuration);
            //TODO: More profile info
            s.add(ProjectUri.Profiles.Profile.Build.PluginManagement.Plugins.Plugin.Executions.xUri);
            s.add(ProjectUri.Profiles.Profile.DependencyManagement.Dependencies.Dependency.Exclusions.xUri);
            s.add(ProjectUri.Profiles.Profile.Dependencies.Dependency.Exclusions.xUri);
            s.add(ProjectUri.Profiles.Profile.Build.Plugins.Plugin.Executions.xUri);
            s.add(ProjectUri.Profiles.Profile.Build.Plugins.Plugin.Executions.Execution.Goals.xURI);
            s.add(ProjectUri.Profiles.Profile.Reporting.Plugins.Plugin.ReportSets.xUri);
            s.add(ProjectUri.Profiles.Profile.Reporting.Plugins.Plugin.ReportSets.ReportSet.configuration);
            s.add(ProjectUri.Profiles.Profile.Build.Plugins.Plugin.Executions.Execution.configuration);

            s.add(ProjectUri.Profiles.Profile.modules);
            s.add(ProjectUri.Profiles.Profile.Dependencies.xUri);
            s.add(ProjectUri.Profiles.Profile.Build.Plugins.Plugin.configuration);
            
            modelProperties = ModelMarshaller.marshallXmlToModelProperties(
                getInputStream(), ProjectUri.baseUri, s );
        }
        return new ArrayList<ModelProperty>(modelProperties);
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

    public int getLineageCount()
    {
        return lineageCount;
    }

    public void setLineageCount( int lineageCount )
    {
        this.lineageCount = lineageCount;
    }

    public PomClassicDomainModel createCopy()
    {
        List<ModelProperty> props = new ArrayList<ModelProperty>();
        for(ModelProperty mp : modelProperties)
        {
            props.add(mp.createCopyOfOriginal());
        }

        return new PomClassicDomainModel(props);
    }

    /**
     * Returns true if this.asString.equals(o.asString()), otherwise false.
     *
     * @param o domain model
     * @return true if this.asString.equals(o.asString()), otherwise false.
     */
    public boolean equals( Object o )
    {
        try {
            return o instanceof PomClassicDomainModel && getId().equals( ( (PomClassicDomainModel) o ).getId() );
        } catch (IOException e) {
            return false;
        }
    }

}
