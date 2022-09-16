package org.apache.maven.model.building.transform;

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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Parent;

public class BuildToRawTransformer
{
    public interface RelativePathVersionMapper
    {
        String apply( Path from, Path path );
    }

    public interface DependencyKeyToVersionMapper
    {
        String apply( Path from, String g, String a );
    }

    public Model transform( Model model,
                            RelativePathVersionMapper relativePathVersionMapper,
                            DependencyKeyToVersionMapper dependencyKeyToVersionMapper )
    {
        Path from = model.getPomFile();
        // Parent version
        if ( relativePathVersionMapper != null )
        {
            Parent parent = model.getParent();
            if ( parent != null && parent.getVersion() == null )
            {
                String relPath = parent.getRelativePath();
                if ( relPath == null || relPath.isEmpty() )
                {
                    relPath = "../pom.xml";
                }
                String version = relativePathVersionMapper.apply( from, from.resolveSibling( relPath ) );
                if ( version != null )
                {
                    model = model.withParent( parent.withVersion( version ) );
                }
            }
        }
        // Reactor dependencies
        if ( dependencyKeyToVersionMapper != null )
        {
            List<Dependency> dependencies = null;
            for ( int index = 0; index < model.getDependencies().size(); index++ )
            {
                Dependency dependency = model.getDependencies().get( index );
                if ( dependency.getVersion() == null )
                {
                    String version = dependencyKeyToVersionMapper.apply(
                            from, dependency.getGroupId(), dependency.getArtifactId() );
                    if ( version != null )
                    {
                        if ( dependencies == null )
                        {
                            dependencies = new ArrayList<>( model.getDependencies() );
                        }
                        dependencies.set( index, dependency.withVersion( version ) );
                    }
                }
            }
            if ( dependencies != null )
            {
                model = model.withDependencies( dependencies );
            }
        }
        return model;
    }
}
