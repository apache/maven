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

import org.apache.maven.cli.jansi.MessageUtils;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;

/**
 * AbstractMavenTransferListener
 */
public abstract class AbstractMavenTransferListener extends AbstractTransferListener {

    private static final String ESC = "\u001B";
    private static final String ANSI_DARK_SET = ESC + "[90m";
    private static final String ANSI_DARK_RESET = ESC + "[0m";

    protected PrintStream out;

    protected AbstractMavenTransferListener(PrintStream out) {
        this.out = out;
    }

    @Override
    public void transferInitiated(TransferEvent event) {
        String darkOn = MessageUtils.isColorEnabled() ? ANSI_DARK_SET : "";
        String darkOff = MessageUtils.isColorEnabled() ? ANSI_DARK_RESET : "";

        String action = event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploading" : "Downloading";
        String direction = event.getRequestType() == TransferEvent.RequestType.PUT ? "to" : "from";

        TransferResource resource = event.getResource();
        StringBuilder message = new StringBuilder();
        message.append(darkOn).append(action).append(' ').append(direction).append(' ');
        message.append(darkOff).append(resource.getRepositoryId());
        message.append(darkOn).append(": ").append(resource.getRepositoryUrl());
        message.append(darkOff).append(resource.getResourceName());

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
        String darkOn = MessageUtils.isColorEnabled() ? ANSI_DARK_SET : "";
        String darkOff = MessageUtils.isColorEnabled() ? ANSI_DARK_RESET : "";

        String action = (event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploaded" : "Downloaded");
        String direction = event.getRequestType() == TransferEvent.RequestType.PUT ? "to" : "from";

        TransferResource resource = event.getResource();
        long contentLength = event.getTransferredBytes();
        StringBuilder message = new StringBuilder();
        FileSizeFormat format = new FileSizeFormat(Locale.ENGLISH);

        message.append(action).append(darkOn).append(' ').append(direction).append(' ');
        message.append(darkOff).append(resource.getRepositoryId());
        message.append(darkOn).append(": ").append(resource.getRepositoryUrl());
        message.append(darkOff).append(resource.getResourceName());
        message.append(darkOn).append(" (");
        format.format(message, contentLength);

        long duration = System.currentTimeMillis() - resource.getTransferStartTime();
        if (duration > 0L) {
            double bytesPerSecond = contentLength / (duration / 1000.0);
            message.append(" at ");
            format.format(message, (long) bytesPerSecond);
            message.append("/s");
        }

        message.append(')').append(darkOff);
        out.println(message.toString());
    }
}
