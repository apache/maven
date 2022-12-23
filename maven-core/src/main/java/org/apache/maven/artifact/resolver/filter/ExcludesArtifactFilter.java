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
package org.apache.maven.artifact.resolver.filter;

import java.util.List;

import org.apache.maven.artifact.Artifact;

/**
 * Filter to exclude from a list of artifact patterns.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * TODO I think this is equiv. to exclusion set filter in maven-core
 */
public class ExcludesArtifactFilter extends IncludesArtifactFilter {
    public ExcludesArtifactFilter(List<String> patterns) {
        super(patterns);
    }

    public boolean include(Artifact artifact) {
        return !super.include(artifact);
    }
}
