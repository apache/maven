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

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectStepData;
import org.eclipse.aether.repository.LocalRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * UT for {@link ReverseTreeRepositoryListener}.
 */
class ReverseTreeRepositoryListenerTest {
    @Test
    void isLocalRepositoryArtifactTest() {
        File baseDir = new File("local/repository");
        LocalRepository localRepository = new LocalRepository(baseDir);
        RepositorySystemSession session = mock(RepositorySystemSession.class);
        when(session.getLocalRepository()).thenReturn(localRepository);

        Artifact localRepositoryArtifact = mock(Artifact.class);
        when(localRepositoryArtifact.getFile()).thenReturn(new File(baseDir, "some/path/within"));

        Artifact nonLocalReposioryArtifact = mock(Artifact.class);
        when(nonLocalReposioryArtifact.getFile()).thenReturn(new File("something/completely/different"));

        assertTrue(ReverseTreeRepositoryListener.isLocalRepositoryArtifactOrMissing(session, localRepositoryArtifact));
        assertFalse(
                ReverseTreeRepositoryListener.isLocalRepositoryArtifactOrMissing(session, nonLocalReposioryArtifact));
    }

    @Test
    void isMissingArtifactTest() {
        File baseDir = new File("local/repository");
        LocalRepository localRepository = new LocalRepository(baseDir);
        RepositorySystemSession session = mock(RepositorySystemSession.class);
        when(session.getLocalRepository()).thenReturn(localRepository);

        Artifact localRepositoryArtifact = mock(Artifact.class);
        when(localRepositoryArtifact.getFile()).thenReturn(null);

        assertTrue(ReverseTreeRepositoryListener.isLocalRepositoryArtifactOrMissing(session, localRepositoryArtifact));
    }

    @Test
    void lookupCollectStepDataTest() {
        RequestTrace doesNotHaveIt =
                RequestTrace.newChild(null, "foo").newChild("bar").newChild("baz");
        assertNull(ReverseTreeRepositoryListener.lookupCollectStepData(doesNotHaveIt));

        final CollectStepData data = mock(CollectStepData.class);

        RequestTrace haveItFirst = RequestTrace.newChild(null, data)
                .newChild("foo")
                .newChild("bar")
                .newChild("baz");
        assertSame(data, ReverseTreeRepositoryListener.lookupCollectStepData(haveItFirst));

        RequestTrace haveItLast = RequestTrace.newChild(null, "foo")
                .newChild("bar")
                .newChild("baz")
                .newChild(data);
        assertSame(data, ReverseTreeRepositoryListener.lookupCollectStepData(haveItLast));

        RequestTrace haveIt = RequestTrace.newChild(null, "foo")
                .newChild("bar")
                .newChild(data)
                .newChild("baz");
        assertSame(data, ReverseTreeRepositoryListener.lookupCollectStepData(haveIt));
    }
}
