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
package org.apache.maven.cling.transfer;

import java.io.File;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.api.MonotonicClock;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class Slf4jMavenTransferListenerTest {

    @Test
    void subSecondTransferReportsItsRate() {
        // Create the mock first: loading Mockito must not count towards the transfer time.
        Logger logger = mock(Logger.class);
        TransferResource resource = resourceStartedMillisAgo(250);
        TransferEvent event = new TransferEvent.Builder(new DefaultRepositorySystemSession(h -> false), resource)
                .setType(TransferEvent.EventType.SUCCEEDED)
                .setTransferredBytes(7_900)
                .build();

        new Slf4jMavenTransferListener(logger).transferSucceeded(event);

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(logger).info(message.capture());
        Matcher rate =
                Pattern.compile("\\(7\\.9 kB at (\\d+\\.\\d) (B|kB)/s\\)$").matcher(message.getValue());
        assertTrue(rate.find(), message.getValue());
        // The transfer took at least 250 ms, so 7.9 kB cannot have moved faster than 31.6 kB/s.
        double bytesPerSecond = Double.parseDouble(rate.group(1)) * ("kB".equals(rate.group(2)) ? 1000 : 1);
        assertTrue(bytesPerSecond <= 31_600, message.getValue());
    }

    private static TransferResource resourceStartedMillisAgo(long millis) {
        Clock previous = TransferResource.getClock();
        TransferResource.setClock(Clock.fixed(MonotonicClock.now().minusMillis(millis), ZoneOffset.UTC));
        try {
            return new TransferResource(
                    "central", "https://repo.maven.apache.org/maven2/", "a/b/1/b-1.pom", new File(""), null);
        } finally {
            TransferResource.setClock(previous);
        }
    }
}
