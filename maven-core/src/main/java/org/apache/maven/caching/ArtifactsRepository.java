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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.caching.jaxb.CacheReportType;
import org.apache.maven.caching.xml.BuildInfo;
import org.apache.maven.execution.MavenSession;

import java.io.IOException;

/**
 * ArtifactsRepository
 */
public interface ArtifactsRepository
{

    BuildInfo findBuild( CacheContext context ) throws IOException;

    void saveBuildInfo( CacheResult cacheResult, BuildInfo buildInfo ) throws IOException;

    void saveArtifactFile( CacheResult cacheResult, Artifact artifact ) throws IOException;

    void saveCacheReport( String buildId, MavenSession session, CacheReportType cacheReport ) throws IOException;
}
