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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;

/**
 * Filter to include from a list of artifact patterns.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class IncludesArtifactFilter implements ArtifactFilter {
    private final Set<String> patterns;

    public IncludesArtifactFilter(List<String> patterns) {
        this.patterns = new LinkedHashSet<>(patterns);
    }

    public boolean include(Artifact artifact) {
        String id = artifact.getGroupId() + ":" + artifact.getArtifactId();

        boolean matched = false;
        for (Iterator<String> i = patterns.iterator(); i.hasNext() & !matched; ) {
            // TODO what about wildcards? Just specifying groups? versions?
            if (id.equals(i.next())) {
                matched = true;
            }
        }
        return matched;
    }

    public List<String> getPatterns() {
        return new ArrayList<>(patterns);
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + patterns.hashCode();

        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        // make sure IncludesArtifactFilter is not equal ExcludesArtifactFilter!
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        IncludesArtifactFilter other = (IncludesArtifactFilter) obj;

        return patterns.equals(other.patterns);
    }
}
