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

import java.io.PrintStream;
import java.util.Locale;

import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;

/**
 * AbstractMavenTransferListener
 */
public abstract class AbstractMavenTransferListener extends AbstractTransferListener {
    public static final String STYLE = ".transfer:-faint";

    protected final MessageBuilderFactory messageBuilderFactory;
    protected final PrintStream out;

    protected AbstractMavenTransferListener(MessageBuilderFactory messageBuilderFactory, PrintStream out) {
        this.messageBuilderFactory = messageBuilderFactory;
        this.out = out;
    }

    @Override
    public void transferInitiated(TransferEvent event) {
        String action = event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploading" : "Downloading";
        String direction = event.getRequestType() == TransferEvent.RequestType.PUT ? "to" : "from";

        TransferResource resource = event.getResource();
        MessageBuilder message = messageBuilderFactory.builder();
        message.style(STYLE).append(action).append(' ').append(direction).append(' ');
        message.resetStyle().append(resource.getRepositoryId());
        message.style(STYLE).append(": ").append(resource.getRepositoryUrl());
        message.resetStyle().append(resource.getResourceName());

        out.println(message.toString());
    }

    @Override
    public void transferCorrupted(TransferEvent event) throws TransferCancelledException {
        TransferResource resource = event.getResource();
        // TODO This needs to be colorized
        out.println("[WARNING] " + event.getException().getMessage() + " from " + resource.getRepositoryId() + " for "
                + resource.getRepositoryUrl() + resource.getResourceName());
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        String action = (event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploaded" : "Downloaded");
        String direction = event.getRequestType() == TransferEvent.RequestType.PUT ? "to" : "from";

        TransferResource resource = event.getResource();
        long contentLength = event.getTransferredBytes();
        FileSizeFormat format = new FileSizeFormat(Locale.ENGLISH);

        MessageBuilder message = messageBuilderFactory.builder();
        message.append(action).style(STYLE).append(' ').append(direction).append(' ');
        message.resetStyle().append(resource.getRepositoryId());
        message.style(STYLE).append(": ").append(resource.getRepositoryUrl());
        message.resetStyle().append(resource.getResourceName());
        message.style(STYLE).append(" (").append(format.format(contentLength));

        long duration = System.currentTimeMillis() - resource.getTransferStartTime();
        if (duration > 0L) {
            double bytesPerSecond = contentLength / (duration / 1000.0);
            message.append(" at ");
            format.format(message, (long) bytesPerSecond);
            message.append("/s");
        }

        message.append(')').resetStyle();
        out.println(message.toString());
    }
}
