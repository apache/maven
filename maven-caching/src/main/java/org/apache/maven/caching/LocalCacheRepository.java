package org.apache.maven.caching;

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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.maven.caching.xml.Build;
import org.apache.maven.caching.xml.CacheSource;
import org.apache.maven.caching.xml.build.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;

/**
 * Local cache repository.
 */
public interface LocalCacheRepository extends CacheRepository
{

    void beforeSave( CacheContext environment ) throws IOException;

    Path getArtifactFile( CacheContext context, CacheSource source, Artifact artifact ) throws IOException;

    void clearCache( CacheContext context );

    Optional<Build> findBestMatchingBuild( MavenSession session, Dependency dependency ) throws IOException;

    Build findLocalBuild( CacheContext context ) throws IOException;
}
