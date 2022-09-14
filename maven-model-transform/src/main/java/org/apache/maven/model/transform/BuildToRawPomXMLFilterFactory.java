package org.apache.maven.model.transform;

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

import java.nio.file.Path;
import java.util.Optional;

import org.codehaus.plexus.util.xml.pull.XmlPullParser;

/**
 * Base implementation for providing the BuildToRawPomXML.
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
public class BuildToRawPomXMLFilterFactory
{
    private final boolean consume;

    public interface RelativePathMapper
    {
        Optional<RelativeProject> apply( Path from, Path path );
    }

    public interface DependencyKeyToVersionMapper
    {
        String apply( Path from, String g, String a );
    }

    public BuildToRawPomXMLFilterFactory()
    {
        this( false );
    }

    public BuildToRawPomXMLFilterFactory( boolean consume )
    {
        this.consume = consume;
    }

    /**
     *
     * @param projectFile will be used by ConsumerPomXMLFilter to get the right filter
     */
    public final XmlPullParser get( XmlPullParser orgParser, Path projectFile )

    {
        // Ensure that xs:any elements aren't touched by next filters
        XmlPullParser parser = orgParser instanceof FastForwardFilter
                ? orgParser : new FastForwardFilter( orgParser );

        if ( getDependencyKeyToVersionMapper() != null )
        {
            parser = new ReactorDependencyXMLFilter( parser, getDependencyKeyToVersionMapper(), projectFile );
        }

        if ( getRelativePathMapper() != null )
        {
            parser = new ParentXMLFilter( parser, getRelativePathMapper(), projectFile );
        }

        CiFriendlyXMLFilter ciFriendlyFilter = new CiFriendlyXMLFilter( parser, consume );
        getChangelist().ifPresent( ciFriendlyFilter::setChangelist  );
        getRevision().ifPresent( ciFriendlyFilter::setRevision );
        getSha1().ifPresent( ciFriendlyFilter::setSha1 );
        parser = ciFriendlyFilter;

        return parser;
    }

    /**
     * @return the mapper or {@code null} if relativePaths don't need to be mapped
     */
    protected RelativePathMapper getRelativePathMapper()
    {
        return null;
    }

    protected DependencyKeyToVersionMapper getDependencyKeyToVersionMapper()
    {
        return null;
    }

    // getters for the 3 magic properties of CIFriendly versions ( https://maven.apache.org/maven-ci-friendly.html )

    protected Optional<String> getChangelist()
    {
        return Optional.empty();
    }

    protected Optional<String> getRevision()
    {
        return Optional.empty();
    }

    protected Optional<String> getSha1()
    {
        return Optional.empty();
    }

}
