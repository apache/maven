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

import java.util.*;

import org.apache.maven.artifact.Artifact;

/**
 * Support for transition. Once {@link ArtifactFilter} is gone, just remove this class and make subclasses plain
 * {@link java.util.function.Predicate}.
 *
 * @since 2.0.0
 */
abstract class ArtifactFilterSupport implements ArtifactFilter {
    public final boolean include(Artifact artifact) {
        return test(artifact);
    }

    public abstract boolean test(Artifact artifact);
}
