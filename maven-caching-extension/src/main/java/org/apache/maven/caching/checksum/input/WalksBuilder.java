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
package org.apache.maven.caching.checksum.input;

import com.google.common.collect.Streams;
import org.apache.maven.caching.xml.config.PathSet;
import org.apache.maven.caching.xml.config.Selector;
import org.apache.maven.model.Build;
import org.apache.maven.model.FileSet;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * MavenProjectInput
 */
public class WalksBuilder
{

    private MavenProject project;
    private Selector defaultSelector;
    private List<Selector> additionalSelectors;

    public WalksBuilder( MavenProject project, Selector defaultSelector, List<Selector> additionalSelectors )
    {
        this.project = project;
        this.defaultSelector = defaultSelector;
        this.additionalSelectors = additionalSelectors;
    }

    public List<DirectorySpec> build()
    {

        Build build = project.getBuild();
        List<DirectorySpec> specs = new LinkedList<>();

        // default scan
        DirectorySpec defaultSpec = new DirectorySpec( project.getBasedir().toPath() );
        Path srcDir = project.getBasedir().toPath().resolve( "src" );
        defaultSpec.includeDirs( srcDir );
        defaultSpec.includeDirs(
                standardInputs( build ).filter( it -> !it.startsWith( srcDir ) ).toArray( Path[]::new )
        );
        defaultSpec.excludeDirs( standardOutputs( build ) );

        defaultSpec.addExclude( fetchMatchingExpressions( defaultSelector.getExclude() ) );
        defaultSpec.addInclude( fetchMatchingExpressions( defaultSelector.getInclude() ) );

        specs.add( defaultSpec );

        additionalSelectors.forEach( selector -> {
            DirectorySpec spec = new DirectorySpec( project.getBasedir().toPath() );
            spec.addExclude( fetchMatchingExpressions( selector.getExclude() ) );
            spec.addInclude( fetchMatchingExpressions( selector.getInclude() ) );
            specs.add( spec );
        } );
        return specs;
    }

    private Path[] standardOutputs( Build build )
    {
        return Stream.of( build.getDirectory(), build.getOutputDirectory(), build.getTestOutputDirectory() )
                .map( Paths::get ).toArray( Path[]::new );
    }

    private Stream<Path> standardInputs( Build build )
    {

        Stream<String> sourceDirs = Stream.of( build.getSourceDirectory(), build.getTestSourceDirectory(),
                build.getScriptSourceDirectory() );

        Stream<String> resourcesDirs = Streams.concat( build.getResources().stream(),
                build.getTestResources().stream() ).map( FileSet::getDirectory );

        return Streams.concat(  sourceDirs, resourcesDirs ).map( Paths::get );
    }

    private List<String> fetchMatchingExpressions( PathSet pathSet )
    {
        Stream<String> globExpression = pathSet.getGlobs().stream().map( glob -> "glob:" + glob );
        Stream<String> patternExpression = pathSet.getPatterns().stream().map( pattern -> "pattern:" + pattern );
        return Streams.concat( globExpression, patternExpression ).collect( Collectors.toList() );
    }

}
