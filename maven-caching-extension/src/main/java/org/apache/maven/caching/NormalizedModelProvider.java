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

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

/**
 * Calculate normalized model for project. The idea is to have model where not all properties resolved. In particular
 * ${project....} and ${pom....} replaced to some constant value. This helps to calculate correct checksum when changed
 * only project version for example build 1 : 1.0-SNAPSHOT, build 2 : 2.0-SNAPSHOT in this case 2nd build could be
 * completely restored from cache.
 */
public interface NormalizedModelProvider
{

    /**
     * @param project - the project which model will be calculated for
     * @return normalized model for project
     */
    Model normalizedModel( MavenProject project );

}
