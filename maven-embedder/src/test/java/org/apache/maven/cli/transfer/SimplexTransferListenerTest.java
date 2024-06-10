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

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;
import org.eclipse.aether.transfer.TransferResource;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.fail;

public class SimplexTransferListenerTest {
    @Test
    public void cancellation() throws InterruptedException {
        TransferListener delegate = new TransferListener() {
            @Override
            public void transferInitiated(TransferEvent event) throws TransferCancelledException {
                throw new TransferCancelledException();
            }

            @Override
            public void transferStarted(TransferEvent event) throws TransferCancelledException {
                throw new TransferCancelledException();
            }

            @Override
            public void transferProgressed(TransferEvent event) throws TransferCancelledException {
                throw new TransferCancelledException();
            }

            @Override
            public void transferCorrupted(TransferEvent event) throws TransferCancelledException {
                throw new TransferCancelledException();
            }

            @Override
            public void transferSucceeded(TransferEvent event) {}

            @Override
            public void transferFailed(TransferEvent event) {}
        };

        SimplexTransferListener listener = new SimplexTransferListener(delegate);

        TransferResource resource =
                new TransferResource(null, null, "http://maven.org/test/test-resource", new File("file"), null);
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();

        // for technical reasons we cannot throw here, even if delegate does cancel transfer
        listener.transferInitiated(event(session, resource, TransferEvent.EventType.INITIATED));

        Thread.sleep(500); // to make sure queue is processed, cancellation applied

        // subsequent call will cancel
        try {
            listener.transferStarted(new TransferEvent.Builder(session, resource)
                    .resetType(TransferEvent.EventType.STARTED)
                    .build());
            fail("should throw");
        } catch (TransferCancelledException e) {
            // good
        }
    }

    @Test
    public void handlesAbsentTransferSource() throws InterruptedException, TransferCancelledException {
        TransferResource resource = new TransferResource(null, null, "http://maven.org/test/test-resource", null, null);

        RepositorySystemSession session = Mockito.mock(RepositorySystemSession.class);
        TransferListener delegate = Mockito.mock(TransferListener.class);
        SimplexTransferListener listener = new SimplexTransferListener(delegate);

        TransferEvent transferInitiatedEvent = event(session, resource, TransferEvent.EventType.INITIATED);
        TransferEvent transferStartedEvent = event(session, resource, TransferEvent.EventType.STARTED);
        TransferEvent transferProgressedEvent = event(session, resource, TransferEvent.EventType.PROGRESSED);
        TransferEvent transferSucceededEvent = event(session, resource, TransferEvent.EventType.SUCCEEDED);

        listener.transferInitiated(transferInitiatedEvent);
        listener.transferStarted(transferStartedEvent);
        listener.transferProgressed(transferProgressedEvent);
        listener.transferSucceeded(transferSucceededEvent);

        Thread.sleep(500); // to make sure queue is processed, cancellation applied

        Mockito.verify(delegate).transferInitiated(transferInitiatedEvent);
        Mockito.verify(delegate).transferStarted(transferStartedEvent);
        Mockito.verify(delegate).transferProgressed(transferProgressedEvent);
        Mockito.verify(delegate).transferSucceeded(transferSucceededEvent);
    }

    private static TransferEvent event(
            RepositorySystemSession session, TransferResource resource, TransferEvent.EventType type) {
        return new TransferEvent.Builder(session, resource).setType(type).build();
    }
}
