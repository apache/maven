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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener that logs to {@link Logger}.
 * TODO: just a stub, expand this fully.
 */
public final class SummaryTransferListener extends AbstractTransferListener {

    private final boolean parallel;

    private final boolean colored;

    private final boolean logProgress;

    private final boolean verbose;

    /**
     * reqType -> repoId -> baseUrl -> resource
     */
    private final ConcurrentMap<TransferEvent.RequestType, ConcurrentMap<String, ConcurrentMap<String, Set<String>>>>
            data;

    private final Logger logger;

    public SummaryTransferListener(boolean parallel, boolean colored, boolean logProgress, boolean verbose) {
        this.parallel = parallel;
        this.colored = colored;
        this.logProgress = logProgress;
        this.verbose = verbose;
        this.data = new ConcurrentHashMap<>();
        this.logger = LoggerFactory.getLogger(SummaryTransferListener.class);
    }

    public void summarize() {
        logger.info("=========================================================================================");
        logger.info("TRANSFER SUMMARY");
        for (Map.Entry<TransferEvent.RequestType, ConcurrentMap<String, ConcurrentMap<String, Set<String>>>> rtentry :
                data.entrySet()) {
            logger.info("");
            if (rtentry.getKey() == TransferEvent.RequestType.GET) {
                logger.info("Resources downloaded");
            } else if (rtentry.getKey() == TransferEvent.RequestType.PUT) {
                logger.info("Resources uploaded");
            } else if (rtentry.getKey() == TransferEvent.RequestType.GET_EXISTENCE) {
                logger.info("Resource checked");
            } else {
                logger.info("{}", rtentry.getKey());
            }
            for (Map.Entry<String, ConcurrentMap<String, Set<String>>> repoentry :
                    rtentry.getValue().entrySet()) {
                logger.info("  {}", repoentry.getKey());
                for (Map.Entry<String, Set<String>> buentry :
                        repoentry.getValue().entrySet()) {
                    logger.info(
                            "    {}: {}", buentry.getKey(), buentry.getValue().size());
                }
            }
        }
    }

    @Override
    public void transferProgressed(TransferEvent event) {
        if (logProgress) {
            // TODO
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
        TransferEvent.RequestType requestType = event.getRequestType();
        String repositoryId = event.getResource().getRepositoryId();
        String baseUrl = event.getResource().getRepositoryUrl();
        String resource = event.getResource().getResourceName();

        data.computeIfAbsent(requestType, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(repositoryId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(baseUrl, k -> ConcurrentHashMap.newKeySet())
                .add(resource);
    }
}
