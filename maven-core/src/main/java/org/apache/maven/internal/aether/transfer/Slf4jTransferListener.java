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
package org.apache.maven.internal.aether.transfer;

import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener that logs to {@link Logger}.
 *
 * TODO: just a stub, expand this fully.
 */
public final class Slf4jTransferListener extends AbstractTransferListener {

    private final boolean parallel;

    private final boolean colored;

    private final boolean logInitiated;

    private final boolean logProgress;

    private final boolean verbose;

    private final Logger logger;

    public Slf4jTransferListener(
            boolean parallel, boolean colored, boolean logInitiated, boolean logProgress, boolean verbose) {
        this.parallel = parallel;
        this.colored = colored;
        this.logInitiated = logInitiated;
        this.logProgress = logProgress;
        this.verbose = verbose;
        this.logger = LoggerFactory.getLogger(Slf4jTransferListener.class);
    }

    @Override
    public void transferInitiated(TransferEvent event) {
        if (logInitiated) {
            String action = event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploading" : "Downloading";
            String direction = event.getRequestType() == TransferEvent.RequestType.PUT ? "to" : "from";

            TransferResource resource = event.getResource();
            StringBuilder message = new StringBuilder();
            message.append(action).append(' ').append(direction).append(' ').append(resource.getRepositoryId());
            message.append(": ");
            message.append(resource.getRepositoryUrl()).append(resource.getResourceName());

            logger.info(message.toString());
        }
    }

    @Override
    public void transferCorrupted(TransferEvent event) {
        TransferResource resource = event.getResource();
        logger.warn(
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

        StringBuilder message = new StringBuilder();
        message.append(action).append(' ').append(direction).append(' ').append(resource.getRepositoryId());
        message.append(": ");
        message.append(resource.getRepositoryUrl()).append(resource.getResourceName());
        logger.info(message.toString());
    }
}
