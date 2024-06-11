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
package org.apache.maven.cli.transfer;

import java.io.File;
import java.util.Objects;

import org.eclipse.aether.transfer.TransferResource;
import org.eclipse.sisu.Nullable;

/**
 * Immutable identifier of a {@link TransferResource}.
 * The {@link TransferResource} is not immutable and does not implement {@code Objects#equals} and {@code Objects#hashCode} methods,
 * making it not very suitable for usage in collections.
 */
final class TransferResourceIdentifier {

    private final String repositoryId;
    private final String repositoryUrl;
    private final String resourceName;

    @Nullable
    private final File file;

    private TransferResourceIdentifier(
            String repositoryId, String repositoryUrl, String resourceName, @Nullable File file) {
        this.repositoryId = repositoryId;
        this.repositoryUrl = repositoryUrl;
        this.resourceName = resourceName;
        this.file = file;
    }

    TransferResourceIdentifier(TransferResource resource) {
        this(resource.getRepositoryId(), resource.getRepositoryUrl(), resource.getResourceName(), resource.getFile());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        TransferResourceIdentifier that = (TransferResourceIdentifier) obj;
        return Objects.equals(this.repositoryId, that.repositoryId)
                && Objects.equals(this.repositoryUrl, that.repositoryUrl)
                && Objects.equals(this.resourceName, that.resourceName)
                && Objects.equals(this.file, that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repositoryId, repositoryUrl, resourceName, file);
    }
}
