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
package org.apache.maven.resolver;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.maven.model.Model;
import org.apache.maven.repository.internal.MavenWorkspaceReader;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;

import static java.util.Objects.requireNonNull;

/**
 * A maven workspace reader that delegates to a chain of other readers, effectively aggregating their contents.
 * <p>
 * This class, while technically is not immutable, should be considered as such once set up. If not mutated, it is also
 * thread-safe. <em>The mutation of this class instances should happen beforehand their use in session</em>.
 */
public class MavenChainedWorkspaceReader implements MavenWorkspaceReader {

    protected List<WorkspaceReader> readers;
    protected WorkspaceRepository repository;

    /**
     * Creates a new workspace reader by chaining the specified readers.
     *
     * @param readers The readers to chain must not be {@code null}.
     */
    public MavenChainedWorkspaceReader(WorkspaceReader... readers) {
        setReaders(Arrays.asList(readers));
    }

    @Override
    public WorkspaceRepository getRepository() {
        return this.repository;
    }

    @Override
    public Model findModel(Artifact artifact) {
        requireNonNull(artifact, "artifact cannot be null");
        Model model = null;

        for (WorkspaceReader workspaceReader : readers) {
            if (workspaceReader instanceof MavenWorkspaceReader) {
                model = ((MavenWorkspaceReader) workspaceReader).findModel(artifact);
                if (model != null) {
                    break;
                }
            }
        }

        return model;
    }

    @Override
    public File findArtifact(Artifact artifact) {
        requireNonNull(artifact, "artifact cannot be null");
        File file = null;

        for (WorkspaceReader reader : readers) {
            file = reader.findArtifact(artifact);
            if (file != null) {
                break;
            }
        }

        return file;
    }

    @Override
    public List<String> findVersions(Artifact artifact) {
        requireNonNull(artifact, "artifact cannot be null");
        Collection<String> versions = new LinkedHashSet<>();

        for (WorkspaceReader reader : readers) {
            versions.addAll(reader.findVersions(artifact));
        }

        return Collections.unmodifiableList(new ArrayList<>(versions));
    }

    public void setReaders(Collection<WorkspaceReader> readers) {
        requireNonNull(readers, "readers");
        // skip possible null entries
        this.readers = Collections.unmodifiableList(
                new ArrayList<>(readers.stream().filter(Objects::nonNull).collect(Collectors.toList())));
        Key key = new Key(this.readers);
        this.repository = new WorkspaceRepository(key.getContentType(), key);
    }

    public List<WorkspaceReader> getReaders() {
        return readers;
    }

    public void addReader(WorkspaceReader workspaceReader) {
        requireNonNull(workspaceReader, "workspaceReader");
        ArrayList<WorkspaceReader> newReaders = new ArrayList<>(this.readers);
        newReaders.add(workspaceReader);
        setReaders(newReaders);
    }

    private static class Key {
        private final List<Object> keys;
        private final String type;

        Key(Collection<WorkspaceReader> readers) {
            keys = readers.stream().map(r -> r.getRepository().getKey()).collect(Collectors.toList());
            type = readers.stream().map(r -> r.getRepository().getContentType()).collect(Collectors.joining("+"));
        }

        public String getContentType() {
            return type;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else {
                return obj != null && this.getClass().equals(obj.getClass()) && this.keys.equals(((Key) obj).keys);
            }
        }

        public int hashCode() {
            return this.keys.hashCode();
        }
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
