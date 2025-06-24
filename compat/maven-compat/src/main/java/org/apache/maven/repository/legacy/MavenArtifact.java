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
package org.apache.maven.repository.legacy;

import org.apache.maven.repository.ArtifactTransferResource;
import org.apache.maven.wagon.resource.Resource;

@Deprecated
class MavenArtifact implements ArtifactTransferResource {

    private String repositoryUrl;

    private Resource resource;

    private long transferStartTime;

    MavenArtifact(String repositoryUrl, Resource resource) {
        if (repositoryUrl == null) {
            this.repositoryUrl = "";
        } else if (!repositoryUrl.endsWith("/") && !repositoryUrl.isEmpty()) {
            this.repositoryUrl = repositoryUrl + '/';
        } else {
            this.repositoryUrl = repositoryUrl;
        }
        this.resource = resource;

        this.transferStartTime = System.currentTimeMillis();
    }

    @Override
    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    @Override
    public String getName() {
        String name = resource.getName();

        if (name == null) {
            name = "";
        } else if (name.startsWith("/")) {
            name = name.substring(1);
        }

        return name;
    }

    @Override
    public String getUrl() {
        return getRepositoryUrl() + getName();
    }

    @Override
    public long getContentLength() {
        return resource.getContentLength();
    }

    @Override
    public long getTransferStartTime() {
        return transferStartTime;
    }

    @Override
    public String toString() {
        return getUrl();
    }
}
