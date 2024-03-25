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
package org.apache.maven.repository.internal;

import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.apache.maven.wagon.TransferFailedException;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.transfer.ArtifactTransferException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultArtifactDescriptorReaderTest extends AbstractRepositoryTestCase {

    private DefaultArtifactDescriptorReader reader;

    private RepositoryEventDispatcher eventDispatcher;

    private ArgumentCaptor<RepositoryEvent> event;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        reader = (DefaultArtifactDescriptorReader) container.lookup(ArtifactDescriptorReader.class);

        eventDispatcher = mock(RepositoryEventDispatcher.class);

        event = ArgumentCaptor.forClass(RepositoryEvent.class);

        Field field = DefaultArtifactDescriptorReader.class.getDeclaredField("repositoryEventDispatcher");
        field.setAccessible(true);
        field.set(reader, eventDispatcher);
    }

    @Test
    void testMng5459() throws Exception {
        // prepare
        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest();

        request.addRepository(newTestRepository());

        request.setArtifact(new DefaultArtifact("org.apache.maven.its", "dep-mng5459", "jar", "0.4.0-SNAPSHOT"));

        // execute
        reader.readArtifactDescriptor(session, request);

        // verify
        verify(eventDispatcher).dispatch(event.capture());

        boolean missingArtifactDescriptor = false;

        for (RepositoryEvent evt : event.getAllValues()) {
            if (EventType.ARTIFACT_DESCRIPTOR_MISSING.equals(evt.getType())) {
                assertEquals(
                        "Could not find artifact org.apache.maven.its:dep-mng5459:pom:0.4.0-20130404.090532-2 in repo ("
                                + newTestRepository().getUrl() + ")",
                        evt.getException().getMessage());
                missingArtifactDescriptor = true;
            }
        }

        assertTrue(
                missingArtifactDescriptor,
                "Expected missing artifact descriptor for org.apache.maven.its:dep-mng5459:pom:0.4.0-20130404.090532-2");
    }

    @Test
    void testNonexistentRepository() throws Exception {
        // prepare
        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest();

        // [MNG-6732] DefaultArtifactDescriptorReader.loadPom to check IGNORE_MISSING policy upon
        // ArtifactTransferException
        RemoteRepository nonexistentRepository =
                new RemoteRepository.Builder("repo", "default", "https://nonexistent.apache.org/maven/repo").build();
        request.addRepository(nonexistentRepository);

        DefaultArtifact artifact = new DefaultArtifact("org.apache.maven.its", "dep-mng6732", "jar", "0.0.1");
        request.setArtifact(artifact);

        // Prepare exception that happens accessing non-existent repository
        UnknownHostException unknownHostException =
                new UnknownHostException("nonexistent.apache.org: Name or service not known");
        TransferFailedException transferFailedException = new TransferFailedException(
                "Transfer failed for https://nonexistent.apache.org/maven/repo/org/apache/maven/its/dep-mng6732.pom",
                unknownHostException);
        ArtifactTransferException artifactTransferException =
                new ArtifactTransferException(artifact, null, transferFailedException);
        ArtifactResolutionException artifactResolutionException = new ArtifactResolutionException(
                new ArrayList<>(),
                "Could not transfer artifact org.apache.maven.its:dep-mng6732:jar:0.0.1",
                artifactTransferException);

        ArtifactResolver mockResolver = mock(ArtifactResolver.class);
        when(mockResolver.resolveArtifact(eq(this.session), any(ArtifactRequest.class)))
                .thenThrow(artifactResolutionException);

        Field field = DefaultArtifactDescriptorReader.class.getDeclaredField("artifactResolver");
        field.setAccessible(true);
        field.set(reader, mockResolver);

        // execute
        reader.readArtifactDescriptor(session, request);

        // verify
        verify(eventDispatcher).dispatch(event.capture());

        boolean artifactTransferExceptionFound = false;

        for (RepositoryEvent evt : event.getAllValues()) {
            if (EventType.ARTIFACT_DESCRIPTOR_MISSING.equals(evt.getType())) {
                assertEquals(
                        "Could not transfer artifact org.apache.maven.its:dep-mng6732:jar:0.0.1:"
                                + " Transfer failed for https://nonexistent.apache.org/maven/repo/org/apache/maven/its/dep-mng6732.pom",
                        evt.getException().getMessage());
                artifactTransferExceptionFound = true;
            }
        }

        if (!artifactTransferExceptionFound) {
            fail("Expected missing artifact descriptor for org.apache.maven.its:dep-mng6732:pom:0.0.1");
        }
    }
}
