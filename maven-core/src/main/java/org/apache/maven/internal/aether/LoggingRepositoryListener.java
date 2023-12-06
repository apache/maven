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

import java.io.FileNotFoundException;

import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.transfer.MetadataNotFoundException;
import org.slf4j.Logger;

/**
 */
class LoggingRepositoryListener extends AbstractRepositoryListener {

    private final Logger logger;

    LoggingRepositoryListener(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void artifactInstalling(RepositoryEvent event) {
        logger.info("Installing {} to {}", event.getArtifact().getFile(), event.getFile());
    }

    @Override
    public void metadataInstalling(RepositoryEvent event) {
        logger.debug("Installing {} to {}", event.getMetadata(), event.getFile());
    }

    @Override
    public void metadataResolved(RepositoryEvent event) {
        Exception e = event.getException();
        if (e != null) {
            if (e instanceof MetadataNotFoundException) {
                logger.debug(e.getMessage());
            } else if (logger.isDebugEnabled()) {
                logger.warn(e.getMessage(), e);
            } else {
                logger.warn(e.getMessage());
            }
        }
    }

    @Override
    public void metadataInvalid(RepositoryEvent event) {
        Exception exception = event.getException();

        Object metadata;
        if (event.getMetadata().getFile() != null) {
            metadata = event.getMetadata().getFile();
        } else {
            metadata = event.getMetadata();
        }

        String errorType = " is invalid";
        if (exception instanceof FileNotFoundException) {
            errorType = " is inaccessible";
        }

        String msg = "";
        if (exception != null) {
            msg = ": " + exception.getMessage();
        }

        if (logger.isDebugEnabled()) {
            logger.warn("The metadata {} {}{}", metadata, errorType, msg, exception);
        } else {
            logger.warn("The metadata {} {}{}", metadata, errorType, msg);
        }
    }

    @Override
    public void artifactDescriptorInvalid(RepositoryEvent event) {
        // The exception stack trace is not really interesting here
        // but the message itself may be quite details and span multiple
        // lines with errors in it, so only display it at debug level.
        String msg = "The POM for {} is invalid, transitive dependencies (if any) will not be available: {}";
        if (logger.isDebugEnabled()) {
            logger.warn(msg, event.getArtifact(), event.getException().getMessage());
        } else {
            logger.warn(msg, event.getArtifact(), "enable verbose output (-X) for more details");
        }
    }

    @Override
    public void artifactDescriptorMissing(RepositoryEvent event) {
        logger.warn("The POM for {} is missing, no dependency information available", event.getArtifact());
    }
}
