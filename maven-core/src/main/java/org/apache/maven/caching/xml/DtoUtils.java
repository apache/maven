package org.apache.maven.caching.xml;

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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.caching.ProjectUtils;
import org.apache.maven.caching.jaxb.ArtifactType;
import org.apache.maven.caching.jaxb.CompletedExecutionType;
import org.apache.maven.caching.jaxb.DigestItemType;
import org.apache.maven.caching.jaxb.PropertyValueType;
import org.apache.maven.caching.jaxb.TrackedPropertyType;
import org.apache.maven.model.Dependency;

import javax.annotation.Nonnull;
import java.util.List;

import static org.apache.maven.caching.checksum.KeyUtils.getArtifactKey;

/**
 * DtoUtils
 */
public class DtoUtils
{

    public static String findPropertyValue( String propertyName, CompletedExecutionType completedExecution )
    {
        final CompletedExecutionType.Configuration configuration = completedExecution.getConfiguration();
        if ( configuration == null )
        {
            return null;
        }
        final List<PropertyValueType> properties = configuration.getProperty();
        for ( PropertyValueType property : properties )
        {
            if ( StringUtils.equals( propertyName, property.getName() ) )
            {
                return property.getValue();
            }
        }
        return null;
    }

    public static ArtifactType createDto( Artifact artifact )
    {
        ArtifactType dto = new ArtifactType();
        dto.setArtifactId( artifact.getArtifactId() );
        dto.setGroupId( artifact.getGroupId() );
        dto.setVersion( artifact.getVersion() );
        dto.setClassifier( artifact.getClassifier() );
        dto.setType( artifact.getType() );
        dto.setScope( artifact.getScope() );
        dto.setFileName( ProjectUtils.normalizedName( artifact ) );
        return dto;
    }

    public static DigestItemType createdDigestedByProjectChecksum( ArtifactType artifact, String projectChecksum )
    {
        DigestItemType dit = new DigestItemType();
        dit.setType( "module" );
        dit.setHash( projectChecksum );
        dit.setFileChecksum( artifact.getFileHash() );
        dit.setValue( getArtifactKey( artifact ) );
        return dit;
    }

    public static DigestItemType createDigestedFile( Artifact artifact, String fileHash )
    {
        DigestItemType dit = new DigestItemType();
        dit.setType( "artifact" );
        dit.setHash( fileHash );
        dit.setFileChecksum( fileHash );
        dit.setValue( getArtifactKey( artifact ) );
        return dit;
    }

    public static Dependency createDependency( ArtifactType artifact )
    {
        final Dependency dependency = new Dependency();
        dependency.setArtifactId( artifact.getArtifactId() );
        dependency.setGroupId( artifact.getGroupId() );
        dependency.setVersion( artifact.getVersion() );
        dependency.setClassifier( artifact.getClassifier() );
        dependency.setType( artifact.getType() );
        dependency.setScope( artifact.getScope() );
        return dependency;
    }

    public static Dependency createDependency( Artifact artifact )
    {
        final Dependency dependency = new Dependency();
        dependency.setArtifactId( artifact.getArtifactId() );
        dependency.setGroupId( artifact.getGroupId() );
        dependency.setVersion( artifact.getVersion() );
        dependency.setType( artifact.getType() );
        dependency.setScope( artifact.getScope() );
        dependency.setClassifier( artifact.getClassifier() );
        return dependency;
    }

    public static void addProperty( CompletedExecutionType execution,
                                    String propertyName,
                                    Object value,
                                    String baseDirPath,
                                    boolean tracked )
    {
        if ( execution.getConfiguration() == null )
        {
            execution.setConfiguration( new CompletedExecutionType.Configuration() );
        }
        final PropertyValueType valueType = new PropertyValueType();
        valueType.setName( propertyName );
        if ( value != null && value.getClass().isArray() )
        {
            value = ArrayUtils.toString( value );
        }
        final String valueText = String.valueOf( value );
        valueType.setValue( StringUtils.remove( valueText, baseDirPath ) );
        valueType.setTracked( tracked );
        execution.getConfiguration().getProperty().add( valueType );
    }

    public static boolean containsAllProperties(
            @Nonnull CompletedExecutionType cachedExecution, List<TrackedPropertyType> trackedProperties )
    {

        if ( trackedProperties == null || trackedProperties.isEmpty() )
        {
            return true;
        }

        if ( !cachedExecution.isSetConfiguration() )
        {
            return false;
        }

        final List<PropertyValueType> executionProperties = cachedExecution.getConfiguration().getProperty();
        for ( TrackedPropertyType trackedProperty : trackedProperties )
        {
            if ( !contains( executionProperties, trackedProperty.getPropertyName() ) )
            {
                return false;
            }
        }
        return true;
    }

    public static boolean contains( List<PropertyValueType> executionProperties, String propertyName )
    {
        for ( PropertyValueType executionProperty : executionProperties )
        {
            if ( StringUtils.equals( executionProperty.getName(), propertyName ) )
            {
                return true;
            }
        }
        return false;
    }

    public static ArtifactType copy( ArtifactType artifact )
    {
        ArtifactType copy = new ArtifactType();
        if ( artifact.isSetArtifactId() )
        {
            copy.setArtifactId( artifact.getArtifactId() );
        }
        if ( artifact.isSetGroupId() )
        {
            copy.setGroupId( artifact.getGroupId() );
        }
        if ( artifact.isSetVersion() )
        {
            copy.setVersion( artifact.getVersion() );
        }
        if ( artifact.isSetType() )
        {
            copy.setType( artifact.getType() );
        }
        if ( artifact.isSetClassifier() )
        {
            copy.setClassifier( artifact.getClassifier() );
        }
        if ( artifact.isSetScope() )
        {
            copy.setScope( artifact.getScope() );
        }
        if ( artifact.isSetFileName() )
        {
            copy.setFileName( artifact.getFileName() );
        }
        if ( artifact.isSetFileHash() )
        {
            copy.setFileHash( artifact.getFileHash() );
        }
        if ( artifact.isSetFileSize() )
        {
            copy.setFileSize( artifact.getFileSize() );
        }
        return copy;
    }
}
