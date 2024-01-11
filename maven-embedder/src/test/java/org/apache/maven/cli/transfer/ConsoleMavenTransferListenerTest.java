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
import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.maven.cli.jline.JLineMessageBuilderFactory;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;
import org.eclipse.aether.transfer.TransferResource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleMavenTransferListenerTest {

    private CountDownLatch startLatch;
    private CountDownLatch endLatch;

    @Test
    void testTransferProgressedWithPrintResourceNames() throws Exception {
        int size = 1000;
        ExecutorService service = Executors.newFixedThreadPool(size * 2);
        startLatch = new CountDownLatch(size);
        endLatch = new CountDownLatch(size);
        Map<String, String> output = new ConcurrentHashMap<String, String>();

        TransferListener listener = new SimplexTransferListener(new ConsoleMavenTransferListener(
                new JLineMessageBuilderFactory(),
                new PrintStream(System.out) {

                    @Override
                    public void print(Object o) {

                        String string = o.toString();
                        int i = string.length() - 1;
                        while (i >= 0) {
                            char c = string.charAt(i);
                            if (c == '\n' || c == '\r' || c == ' ') i--;
                            else break;
                        }

                        string = string.substring(0, i + 1).trim();
                        output.put(string, string);
                        System.out.print(o);
                    }
                },
                true));
        TransferResource resource =
                new TransferResource(null, null, "http://maven.org/test/test-resource", new File(""), null);
        resource.setContentLength(size - 1);

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(h -> false); // no close handle

        // warm up
        test(listener, session, resource, 0);

        for (int i = 1; i < size; i++) {
            final int bytes = i;

            service.execute(() -> {
                test(listener, session, resource, bytes);
            });
        }

        // start all threads at once
        try {
            startLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // wait for all thread to end
        try {
            endLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // despite all are back, we need to make sure all the events are processed (are async)
        // this one should block until all processed
        listener.transferSucceeded(new TransferEvent.Builder(session, resource)
                .setType(TransferEvent.EventType.SUCCEEDED)
                .build());

        StringBuilder message = new StringBuilder("Messages [");
        boolean test = true;
        for (int i = 0; i < 999; i++) {
            boolean ok = output.containsKey("Progress (1): test-resource (" + i + "/999 B)");
            if (!ok) {
                System.out.println("false : " + i);
                message.append(i + ",");
            }
            test = test & ok;
        }
        assertTrue(test, message + "] are missing in " + output);
    }

    private void test(
            TransferListener listener,
            DefaultRepositorySystemSession session,
            TransferResource resource,
            final int bytes) {
        TransferEvent event = new TransferEvent.Builder(session, resource)
                .setType(TransferEvent.EventType.PROGRESSED)
                .setTransferredBytes(bytes)
                .build();
        startLatch.countDown();
        try {
            listener.transferProgressed(event);
        } catch (TransferCancelledException e) {
        }
        endLatch.countDown();
    }
}
