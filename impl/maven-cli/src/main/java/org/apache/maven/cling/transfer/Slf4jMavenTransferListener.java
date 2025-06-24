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

import java.time.Duration;
import java.time.Instant;

import org.apache.maven.api.MonotonicClock;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Slf4jMavenTransferListener
 */
public class Slf4jMavenTransferListener extends AbstractTransferListener {

    protected final Logger out;

    public Slf4jMavenTransferListener() {
        this.out = LoggerFactory.getLogger(Slf4jMavenTransferListener.class);
    }

    // TODO should we deprecate?
    public Slf4jMavenTransferListener(Logger out) {
        this.out = out;
    }

    @Override
    public void transferInitiated(TransferEvent event) {
        String action = event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploading" : "Downloading";
        String direction = event.getRequestType() == TransferEvent.RequestType.PUT ? "to" : "from";

        TransferResource resource = event.getResource();
        StringBuilder message = new StringBuilder();
        message.append(action).append(' ').append(direction).append(' ').append(resource.getRepositoryId());
        message.append(": ");
        message.append(resource.getRepositoryUrl()).append(resource.getResourceName());

        out.info(message.toString());
    }

    @Override
    public void transferCorrupted(TransferEvent event) throws TransferCancelledException {
        TransferResource resource = event.getResource();
        out.warn(
                "{} from {} for {}{}",
                event.getException().getMessage(),
                resource.getRepositoryId(),
                resource.getRepositoryUrl(),
                resource.getResourceName());
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        String action = (event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploaded" : "Downloaded");
        String direction = event.getRequestType() == TransferEvent.RequestType.PUT ? "to" : "from";

        TransferResource resource = event.getResource();
        long contentLength = event.getTransferredBytes();
        FileSizeFormat format = new FileSizeFormat();

        StringBuilder message = new StringBuilder();
        message.append(action).append(' ').append(direction).append(' ').append(resource.getRepositoryId());
        message.append(": ");
        message.append(resource.getRepositoryUrl())
                .append(resource.getResourceName())
                .append(" (");
        format.format(message, contentLength);

        Duration duration =
                Duration.between(Instant.ofEpochMilli(resource.getTransferStartTime()), MonotonicClock.now());
        if ((duration.getSeconds() | duration.getNano()) > 0) { // duration.isPositive()
            long bytesPerSecond = Math.round(contentLength / (double) duration.toSeconds());
            message.append(" at ");
            format.format(message, bytesPerSecond);
            message.append("/s");
        }

        message.append(')');
        out.info(message.toString());
    }
}
