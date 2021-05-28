package org.apache.maven.artifact.handler;

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

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import org.codehaus.plexus.testing.PlexusTest;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

@PlexusTest
public class ArtifactHandlerTest
{
    @Inject
    PlexusContainer container;

    @Test
    public void testAptConsistency()
        throws Exception
    {
        File apt = getTestFile( "src/site/apt/artifact-handlers.apt" );

        List<String> lines = FileUtils.loadFile( apt );

        for ( String line : lines )
        {
            if ( line.startsWith( "||" ) )
            {
                String[] cols = line.split( "\\|\\|" );
                String[] expected =
                    new String[] { "", "type", "classifier", "extension", "packaging", "language", "added to classpath",
                        "includesDependencies", "" };

                int i = 0;
                for ( String col : cols )
                {
                    assertEquals( expected[i++], col.trim(), "Wrong column header" );
                }
            }
            else if ( line.startsWith( "|" ) )
            {
                String[] cols = line.split( "\\|" );

                String type = trimApt( cols[1] );
                String classifier = trimApt( cols[2] );
                String extension = trimApt( cols[3], type );
                String packaging = trimApt( cols[4], type );
                String language = trimApt( cols[5] );
                String addedToClasspath = trimApt( cols[6] );
                String includesDependencies = trimApt( cols[7] );

                ArtifactHandler handler = container.lookup( ArtifactHandler.class, type );
                assertEquals( handler.getExtension(), extension, type + " extension" );
                assertEquals( handler.getPackaging(), packaging, type + " packaging" );
                assertEquals( handler.getClassifier(), classifier, type + " classifier" );
                assertEquals( handler.getLanguage(), language, type + " language" );
                assertEquals( handler.isAddedToClasspath() ? "true" : null, addedToClasspath, type + " addedToClasspath" );
                assertEquals( handler.isIncludesDependencies() ? "true" : null, includesDependencies, type + " includesDependencies" );
            }
        }
    }

    private String trimApt( String content, String type )
    {
        String value = trimApt( content );
        return "= type".equals( value ) ? type : value;
    }

    private String trimApt( String content )
    {
        content = content.replace( '<', ' ' ).replace( '>', ' ' ).trim();

        return ( content.length() == 0 ) ? null : content;
    }
}
