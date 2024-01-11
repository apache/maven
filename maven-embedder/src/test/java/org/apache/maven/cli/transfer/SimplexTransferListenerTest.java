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
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;
import org.eclipse.aether.transfer.TransferResource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SimplexTransferListenerTest {
    @Test
    void cancellation() throws InterruptedException {
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
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(h -> false); // no close handle

        // for technical reasons we cannot throw here, even if delegate does cancel transfer
        listener.transferInitiated(new TransferEvent.Builder(session, resource)
                .setType(TransferEvent.EventType.INITIATED)
                .build());

        Thread.sleep(500); // to make sure queue is processed, cancellation applied

        // subsequent call will cancel
        assertThrows(
                TransferCancelledException.class,
                () -> listener.transferStarted(new TransferEvent.Builder(session, resource)
                        .resetType(TransferEvent.EventType.STARTED)
                        .build()));
    }
}
