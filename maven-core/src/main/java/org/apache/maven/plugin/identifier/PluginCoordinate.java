package org.apache.maven.plugin.identifier;
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

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.model.Plugin;

/**
 * The co-ordinate of a plugin - the identifier, and the version
 * that is in use.
 */
public class PluginCoordinate
{
  PluginIdentifier identifier;
  ArtifactVersion version;
  
  public PluginCoordinate(Plugin plugin)
  {
    this.identifier = new PluginIdentifier(plugin.getGroupId(), plugin.getArtifactId());
    if( plugin.getVersion() != null )
    {
      this.version = new DefaultArtifactVersion(plugin.getVersion());
    }
    else
    {
    	throw new IllegalArgumentException("Invalid version spec");
    }
  }
  
  public PluginCoordinate(String groupId, String artifactId, String version)
  {
    this.identifier = new PluginIdentifier(groupId, artifactId);
    if( version != null )
    {
      this.version = new DefaultArtifactVersion( version );
    }
    else
    {
    	throw new IllegalArgumentException("Invalid version spec");
    }
  }

  public PluginIdentifier getIdentifier()
  {
    return identifier;
  }

  public ArtifactVersion getVersion()
  {
    return version;
  }
  
  public String toString()
  {
    String value = this.identifier.toString();
    if( this.version != null )
    {
      value += ":" + version.toString();
    }
    return value;
  }
  
  /**
   * @see java.lang.Object#hashCode()
   */
  public int hashCode()
  {
      return toString().hashCode();
  }
  
  public boolean equals( Object other )
  {
      if ( other instanceof PluginCoordinate )
      {
        PluginCoordinate otherCoord = (PluginCoordinate) other;

        return this.toString().equals( otherCoord.toString() );
      }

      return false;
  }
  
}
