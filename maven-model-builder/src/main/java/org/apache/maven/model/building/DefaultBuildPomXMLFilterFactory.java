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
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.maven.model.Model;
import org.apache.maven.model.transform.BuildToRawPomXMLFilterFactory;
import org.apache.maven.model.transform.RelativeProject;
import org.xml.sax.ext.LexicalHandler;

/**
 * A BuildPomXMLFilterFactory which is context aware
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
public class DefaultBuildPomXMLFilterFactory extends BuildToRawPomXMLFilterFactory
{
    private final TransformerContext context;

    /**
     *
     * @param context a set of data to extract values from as required for the build pom
     * @param lexicalHandlerConsumer the lexical handler consumer
     * @param consume {@code true} if this factory is being used for creating the consumer pom, otherwise {@code false}
     */
    public DefaultBuildPomXMLFilterFactory( TransformerContext context,
                                            Consumer<LexicalHandler> lexicalHandlerConsumer,
                                            boolean consume )
    {
        super( lexicalHandlerConsumer, consume );
        this.context = context;
    }

    @Override
    protected Function<Path, Optional<RelativeProject>> getRelativePathMapper()
    {
        return p -> Optional.ofNullable( context.getRawModel( p ) ).map( m -> toRelativeProject( m ) );
    }

    @Override
    protected BiFunction<String, String, String> getDependencyKeyToVersionMapper()
    {
        return ( g, a ) -> Optional.ofNullable( context.getRawModel( g, a ) )
                            .map( m -> toVersion( m ) )
                            .orElse( null );
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
