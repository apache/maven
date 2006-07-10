package org.apache.maven.bootstrap.model;

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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Describes a dependency.
 *
 * @version $Id$
 */
public class Dependency extends Model
{
    private String id;

    private String url;

    private String jar;

    private String scope = SCOPE_COMPILE;

    private String resolvedVersion;

    private boolean optional;

    public static final String SCOPE_TEST = "test";

    public static final String SCOPE_COMPILE = "compile";

    public static final String SCOPE_RUNTIME = "runtime";

    private Set exclusions = new HashSet();

    public Dependency( List chain )
    {
        super(chain);
    }

    public Dependency( String groupId, String artifactId, String version, String type, List chain )
    {
        this( chain );
        setVersion( version );
        setArtifactId( artifactId );
        setGroupId( groupId );
        setType( type );
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getId()
    {
        if ( isValid( getGroupId() ) && isValid( getArtifactId() ) )
        {
            return getGroupId() + ":" + getArtifactId();
        }

        return id;
    }

    public String getArtifactDirectory()
    {
        if ( isValid( getGroupId() ) )
        {
            return getGroupId();
        }

        return getId();
    }

    public String getArtifact()
    {
        // If the jar name has been explicty set then use that. This
        // is when the <jar/> element is explicity used in the POM.
        if ( jar != null )
        {
            return jar;
        }

        String artifact;

        if ( isValid( getArtifactId() ) )
        {
            artifact = getArtifactId() + "-" + getResolvedVersion() + ".";
        }
        else
        {
            artifact = getId() + "-" + getResolvedVersion() + ".";
        }

        if ( "jar".equals( getType() ) || "maven-plugin".equals( getType() ) )
        {
            artifact += "jar";
        }
        else
        {
            artifact += getType();
        }
        return artifact;
    }

    public void setJar( String jar )
    {
        // This is a check we need because of the jelly interpolation
        // process. If we don't check an empty string will be set and
        // screw up getArtifact() above.
        if ( jar.trim().length() == 0 )
        {
            return;
        }

        this.jar = jar;
    }

    public String getJar()
    {
        return jar;
    }

    public String getScope()
    {
        return scope;
    }

    public void setScope( String scope )
    {
        this.scope = scope;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public String getUrl()
    {
        return url;
    }

    public String getType()
    {
        return getPackaging();
    }

    public void setType( String type )
    {
        setPackaging( type );
    }

    private boolean isValid( String value )
    {
        return value != null && !value.trim().equals( "" );

    }

    public String toString()
    {
        return "Dependency[" + getId() + ":" + getVersion() + ":" + getType() + "]";
    }


    public String getConflictId()
    {
        return getGroupId() + ":" + getArtifactId() + ":" + getType();
    }

    public String getDependencyConflictId()
    {
        return getGroupId() + ":" + getArtifactId() + ":" + getType() + ":" + getVersion();
    }

    public void setResolvedVersion( String resolvedVersion )
    {
        this.resolvedVersion = resolvedVersion;
    }

    public String getResolvedVersion()
    {
        if ( resolvedVersion == null )
        {
            resolvedVersion = getVersion();
        }
        return resolvedVersion;
    }

    public void addExclusion( Exclusion currentExclusion )
    {
        exclusions.add( currentExclusion.getConflictId() );
    }

    public Set getExclusions()
    {
        return exclusions;
    }

    public Dependency getPomDependency()
    {
        Dependency dep = new Dependency( getGroupId(), getArtifactId(), getVersion(), "pom", getChain() );
        dep.getRepositories().addAll( getRepositories() );
        return dep;
    }

    public void setOptional( boolean optional )
    {
        this.optional = optional;
    }

    public boolean isOptional()
    {
        return optional;
    }

    public boolean equals( Object o )
    {
        if ( o instanceof Dependency )
        {
            return super.equals( o );
        }
        else
        {
            return false;
        }
    }
}
