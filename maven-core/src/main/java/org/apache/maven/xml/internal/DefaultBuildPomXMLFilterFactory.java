package org.apache.maven.xml.internal;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelCacheManager;
import org.apache.maven.xml.filter.BuildPomXMLFilterFactory;
import org.apache.maven.xml.filter.DependencyKey;
import org.apache.maven.xml.filter.RelativeProject;

/**
 * 
 * @author Robert Scholte
 * @since 3.7.0
 */
@Named
@Singleton
public class DefaultBuildPomXMLFilterFactory extends BuildPomXMLFilterFactory
{
    private MavenSession session;
    
    @Inject
    private ModelCacheManager rawModelCache; 
    
    @Inject
    public DefaultBuildPomXMLFilterFactory( MavenSession session )
    {
        this.session = session;
    }

    @Override
    protected Optional<String> getChangelist()
    {
        return Optional.ofNullable( session.getUserProperties().getProperty( "changelist" ) );
    }

    @Override
    protected Optional<String> getRevision()
    {
        return Optional.ofNullable( session.getUserProperties().getProperty( "revision" ) );
    }

    @Override
    protected Optional<String> getSha1()
    {
        return Optional.ofNullable( session.getUserProperties().getProperty( "sha1" ) );
    }

    @Override
    protected Function<Path, Optional<RelativeProject>> getRelativePathMapper()
    {
        return p -> Optional.ofNullable( rawModelCache.get( p ) ).map( m -> toRelativeProject( m ) );
    }
    
    @Override
    protected Function<DependencyKey, String> getDependencyKeyToVersionMapper()
    {
        return k -> Optional.ofNullable( rawModelCache.get( k ) )
                            .map( m -> toVersion( m ) )
                            .orElse( null );
    }

    private RelativeProject toRelativeProject( final Model m )
    {
        String groupId = m.getGroupId();
        if ( groupId == null && m.getParent() != null )
        {
            groupId = m.getParent().getGroupId();
        }

        String version = m.getVersion();
        if ( version == null && m.getParent() != null )
        {
            version = m.getParent().getVersion();
        }

        return new RelativeProject( groupId, m.getArtifactId(), version );
    }
    
    private String toVersion( final Model m )
    {
        String version = m.getVersion();
        if ( version == null && m.getParent() != null )
        {
            version = m.getParent().getVersion();
        }

        return version;
    }
}
