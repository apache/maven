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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a Model.
 *
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 *
 */
public class Model
{
    private Map plugins = new HashMap();

    private String artifactId;

    private String version;

    private String groupId;

    private String parentGroupId;

    private String parentArtifactId;

    private String parentVersion;

    private String packaging = "jar";

    private File pomFile;

    private List modules = new ArrayList();

    private List resources = new ArrayList();

    private Set repositories = new HashSet();

    private Map dependencies = new HashMap();

    private Map parentDependencies = new HashMap();

    private Map transitiveDependencies = new HashMap();

    private Map managedDependencies = new HashMap();

    private List chain;

    public Model()
    {
        this.chain = new ArrayList();
    }

    public Model( List chain )
    {
        this.chain = new ArrayList( chain );
        this.chain.add( this );
    }

    public String getId()
    {
        return groupId + ":" + artifactId + ":" + packaging + ":" + version;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getPackaging()
    {
        return packaging;
    }

    public String getParentArtifactId()
    {
        return parentArtifactId;
    }

    public void setParentArtifactId( String artifactId )
    {
        this.parentArtifactId = artifactId;
    }

    public void setPackaging( String packaging )
    {
        this.packaging = packaging;
    }

    public String getParentGroupId()
    {
        return parentGroupId;
    }

    public void setParentGroupId( String groupId )
    {
        this.parentGroupId = groupId;
    }

    public String getParentVersion()
    {
        return parentVersion;
    }

    public void setParentVersion( String version )
    {
        this.parentVersion = version;
    }

    public Map getPlugins()
    {
        return plugins;
    }

    public List getModules()
    {
        return modules;
    }

    public List getResources()
    {
        return resources;
    }

    public File getProjectFile()
    {
        return pomFile;
    }

    public void setPomFile( File file )
    {
        this.pomFile = file;
    }

    public Set getRepositories()
    {
        return repositories;
    }

    public Collection getAllDependencies()
    {
        Map m = new HashMap();
        m.putAll( transitiveDependencies );
        m.putAll( parentDependencies );
        m.putAll( dependencies );
        return m.values();
    }

    public List getChain()
    {
        return chain;
    }

    public Map getDependencies()
    {
        return dependencies;
    }

    public Map getParentDependencies()
    {
        return parentDependencies;
    }

    public Map getTransitiveDependencies()
    {
        return transitiveDependencies;
    }

    public Map getManagedDependencies()
    {
        return managedDependencies;
    }

    public Collection getManagedDependenciesCollection()
    {
        Map m = new HashMap();
        m.putAll( managedDependencies );
        return m.values();
    }

    public String toString()
    {
        return "Model[" + getId() + "]";
    }

    public int hashCode()
    {
        int result = 17;
        result = 37 * result + groupId.hashCode();
        result = 37 * result + artifactId.hashCode();
        result = 37 * result + packaging.hashCode();
        result = 37 * result + version.hashCode();
        return result;
    }

    public boolean equals( Object o )
    {
        if ( o == this )
        {
            return true;
        }

        if ( !( o instanceof Model ) )
        {
            return false;
        }

        Model d = (Model) o;

        if ( !d.getGroupId().equals( groupId ) )
        {
            return false;
        }
        else if ( !d.getArtifactId().equals( artifactId ) )
        {
            return false;
        }
        else if ( !d.getVersion().equals( version ) )
        {
            return false;
        }
        else if ( !d.getPackaging().equals( packaging ) )
        {
            return false;
        }
        return true;
    }

}
