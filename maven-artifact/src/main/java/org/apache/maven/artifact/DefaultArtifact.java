package org.apache.maven.artifact;

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

import org.codehaus.plexus.util.StringUtils;

import java.io.File;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 */
public class DefaultArtifact
    implements Artifact
{

    // ----------------------------------------------------------------------
    // These are the only things i need to specify
    // ----------------------------------------------------------------------

    private String groupId;

    private String artifactId;

    private String version;

    private String type;

    private String classifier;

    private String scope;

    private String extension;

    private String path;

    /**
     * @todo this should be replaced by type handler
     */
    public DefaultArtifact( String groupId, String artifactId, String version, String scope, String type,
                           String extension )
    {
        this( groupId, artifactId, version, scope, type, null, extension );
    }

    /**
     * !!! WARNING !!! Never put <classifier/> in the POM. It is for mojo use
     * only. Classifier is for specifying derived artifacts, like ejb-client.
     */
    public DefaultArtifact( String groupId, String artifactId, String version, String scope, String type,
                           String classifier, String extension )
    {
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

        this.extension = extension;
    }

    public DefaultArtifact( String groupId, String artifactId, String version, String type )
    {
        this( groupId, artifactId, version, null, type, null, type );
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

    public String getExtension()
    {
        if ( extension != null )
        {
            return extension;
        }

        return type;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public String getPath()
    {
        return path;
    }

    public void setPath( String path )
    {
        this.path = path;
    }

    public boolean exists()
    {
        return getFile().exists();
    }

    public File getFile()
    {
        return new File( getPath() );
    }

    public File getChecksumFile()
    {
        return new File( getFile().getAbsolutePath() + ".md5" );
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