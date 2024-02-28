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
package org.apache.maven.repository.internal.scopes;

import java.util.Map;

import org.apache.maven.repository.internal.artifact.MavenArtifactProperties;
import org.eclipse.aether.SystemScopeHandler;
import org.eclipse.aether.artifact.Artifact;

/**
 * A system scope handler.
 *
 * @since 4.0.0
 */
public final class MavenSystemScopeHandler implements SystemScopeHandler {
    @Override
    public boolean isSystemScope(String scope) {
        return MavenDependencyScopes.SYSTEM.equals(scope);
    }

    @Override
    public String getSystemPath(Artifact artifact) {
        return artifact.getProperty(MavenArtifactProperties.LOCAL_PATH, null);
    }

    @Override
    public void setSystemPath(Map<String, String> properties, String systemPath) {
        if (systemPath == null) {
            properties.remove(MavenArtifactProperties.LOCAL_PATH);
        } else {
            properties.put(MavenArtifactProperties.LOCAL_PATH, systemPath);
        }
    }
}
