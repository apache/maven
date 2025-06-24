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

import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DefaultArtifactDescriptorReaderTest extends AbstractRepositoryTestCase {

    @Test
    void testMng5459() throws Exception {
        // prepare
        DefaultArtifactDescriptorReader reader =
                (DefaultArtifactDescriptorReader) getContainer().lookup(ArtifactDescriptorReader.class);

        RepositoryEventDispatcher eventDispatcher = mock(RepositoryEventDispatcher.class);

        ArgumentCaptor<RepositoryEvent> event = ArgumentCaptor.forClass(RepositoryEvent.class);

        Field field = DefaultArtifactDescriptorReader.class.getDeclaredField("repositoryEventDispatcher");
        field.setAccessible(true);
        field.set(reader, eventDispatcher);

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
}
