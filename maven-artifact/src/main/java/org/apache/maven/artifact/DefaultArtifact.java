package org.apache.maven.artifact;

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

import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 * @todo this should possibly be replaced by type handler
 */
public class DefaultArtifact
    implements Artifact
{
    private final String groupId;

    private final String artifactId;

    private final String version;

    private final String type;

    private final String classifier;

    private final String scope;

    private List metadataList;

    private File file;

    /**
     * !!! WARNING !!! Never put <classifier/> in the POM. It is for mojo use
     * only. Classifier is for specifying derived artifacts, like ejb-client.
     */
    public DefaultArtifact( String groupId, String artifactId, String version, String scope, String type,
                            String classifier )
    {
        // These should help us catch coding errors until this code gets a whole lot clearer
        if ( type == null )
        {
            throw new NullPointerException( "Artifact type cannot be null." );
        }

        this.groupId = groupId;

        this.artifactId = artifactId;

        this.version = version;

        this.type = type;

        this.scope = scope;

        this.classifier = classifier;
    }

    public DefaultArtifact( String groupId, String artifactId, String version, String scope, String type )
    {
        this( groupId, artifactId, version, scope, type, null );
    }

    public DefaultArtifact( String groupId, String artifactId, String version, String type )
    {
        this( groupId, artifactId, version, null, type, null );
    }

    public String getClassifier()
    {
        return classifier;
    }

    public boolean hasClassifier()
    {
        return StringUtils.isNotEmpty( classifier );
    }

    public String getScope()
    {
        return scope;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public String getType()
    {
        return type;
    }

    public void setFile( File file )
    {
        this.file = file;
    }

    public File getFile()
    {
        if ( file == null )
        {
            throw new IllegalStateException( "Artifact's local file has not yet been assigned - not resolved" );
        }
        return file;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public String getId()
    {
        return getConflictId() + ( hasClassifier() ? ( ":" + getClassifier() ) : "" ) + ":" + getVersion();
    }

    public String getConflictId()
    {
        return getGroupId() + ":" + getArtifactId() + ":" + getType();
    }

    public void addMetadata( ArtifactMetadata metadata )
    {
        if ( metadataList == null )
        {
            metadataList = new ArrayList();
        }
        metadataList.add( metadata );
    }

    public List getMetadataList()
    {
        return metadataList == null ? Collections.EMPTY_LIST : metadataList;
    }

    // ----------------------------------------------------------------------
    // Object overrides
    // ----------------------------------------------------------------------

    public String toString()
    {
        return getId();
    }

    public int hashCode()
    {
        return getId().hashCode();
    }

    public boolean equals( Object o )
    {
        Artifact other = (Artifact) o;

        return getId().equals( other.getId() );
    }
}