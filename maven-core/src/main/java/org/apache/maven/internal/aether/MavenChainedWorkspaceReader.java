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
package org.apache.maven.internal.aether;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.repository.internal.MavenWorkspaceReader;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.util.repository.ChainedWorkspaceReader;

/**
 * A maven workspace reader that delegates to a chain of other readers, effectively aggregating their contents.
 */
public final class MavenChainedWorkspaceReader implements MavenWorkspaceReader {

    private ChainedWorkspaceReader delegate;

    private WorkspaceReader[] readers;

    /**
     * Creates a new workspace reader by chaining the specified readers.
     *
     * @param readers The readers to chain must not be {@code null}.
     */
    private MavenChainedWorkspaceReader(WorkspaceReader... readers) {
        this.delegate = new ChainedWorkspaceReader(readers);
        this.readers = readers;
    }

    @Override
    public Model findModel(Artifact artifact) {
        for (WorkspaceReader workspaceReader : readers) {
            if (workspaceReader instanceof MavenWorkspaceReader) {
                Model model = ((MavenWorkspaceReader) workspaceReader).findModel(artifact);
                if (model != null) {
                    return model;
                }
            }
        }
        return null;
    }

    @Override
    public WorkspaceRepository getRepository() {
        return delegate.getRepository();
    }

    @Override
    public File findArtifact(Artifact artifact) {
        return delegate.findArtifact(artifact);
    }

    @Override
    public List<String> findVersions(Artifact artifact) {
        return delegate.findVersions(artifact);
    }

    /**
     * chains a collection of {@link WorkspaceReader}s
     * @param workspaceReaderCollection the collection of readers, might be empty but never <code>null</code>
     * @return if the collection contains only one item returns the single item, otherwise creates a new
     *         {@link MavenChainedWorkspaceReader} chaining all readers in the order of the given collection.
     */
    public static WorkspaceReader of(Collection<WorkspaceReader> workspaceReaderCollection) {
        WorkspaceReader[] readers = workspaceReaderCollection.toArray(new WorkspaceReader[0]);
        if (readers.length == 1) {
            return readers[0];
        }
        return new MavenChainedWorkspaceReader(readers);
    }
}
