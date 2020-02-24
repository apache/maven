package org.apache.maven.model.building;

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
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.maven.model.Model;
import org.apache.maven.xml.sax.filter.BuildPomXMLFilterFactory;
import org.apache.maven.xml.sax.filter.RelativeProject;

/**
 * 
 * @author Robert Scholte
 * @since 3.7.0
 */
public class DefaultBuildPomXMLFilterFactory extends BuildPomXMLFilterFactory
{
    private final TransformerContext context;
    
    public DefaultBuildPomXMLFilterFactory( TransformerContext context )
    {
        this.context = context;
    }
    
    @Override
    protected Optional<String> getChangelist()
    {
        return Optional.ofNullable( context.getUserProperty( "changelist" ) );
    }

    @Override
    protected Optional<String> getRevision()
    {
        return Optional.ofNullable( context.getUserProperty( "revision" ) );
    }

    @Override
    protected Optional<String> getSha1()
    {
        return Optional.ofNullable( context.getUserProperty( "sha1" ) );
    }

    @Override
    protected Function<Path, Optional<RelativeProject>> getRelativePathMapper()
    {
        return p -> Optional.ofNullable( context.getRawModel( p ) ).map( m -> toRelativeProject( m ) );
    }
    
    @Override
    protected BiFunction<String, String, String> getDependencyKeyToVersionMapper()
    {
        return (g,a) -> Optional.ofNullable( context.getRawModel( g, a ) )
                            .map( m -> toVersion( m ) )
                            .orElse( null );
    }

    private static RelativeProject toRelativeProject( final Model m )
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
    
    private static String toVersion( final Model m )
    {
        String version = m.getVersion();
        if ( version == null && m.getParent() != null )
        {
            version = m.getParent().getVersion();
        }

        return version;
    }
}
