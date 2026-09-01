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

import java.io.PrintWriter;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.maven.api.MonotonicClock;
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
    protected final PrintWriter out;

    protected AbstractMavenTransferListener(MessageBuilderFactory messageBuilderFactory, PrintWriter out) {
        this.messageBuilderFactory = messageBuilderFactory;
        this.out = out;
    }

    /**
     * Escapes control characters so that transfer messages render literally on a terminal.
     * Tab and newline are preserved; every other C0 control, DEL and the C1 controls are escaped.
     *
     * @param str the string to escape, may be {@code null}
     * @return the escaped string, or {@code null} if the input was {@code null}
     */
    static String sanitize(String str) {
        if (str == null) {
            return null;
        }
        StringBuilder sb = null;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            boolean escape = (c < 0x20 && c != '\t' && c != '\n') || (c >= 0x7F && c <= 0x9F);
            if (escape) {
                if (sb == null) {
                    sb = new StringBuilder(str.length() + 8);
                    sb.append(str, 0, i);
                }
                sb.append(String.format("\\u%04x", (int) c));
            } else if (sb != null) {
                sb.append(c);
            }
        }
        return sb != null ? sb.toString() : str;
    }

    @Override
    public void transferInitiated(TransferEvent event) {
        String action = event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploading" : "Downloading";
        String direction = event.getRequestType() == TransferEvent.RequestType.PUT ? "to" : "from";

        TransferResource resource = event.getResource();
        MessageBuilder message = messageBuilderFactory.builder();
        message.style(STYLE).append(action).append(' ').append(direction).append(' ');
        message.resetStyle().append(sanitize(resource.getRepositoryId()));
        message.style(STYLE).append(": ").append(sanitize(resource.getRepositoryUrl()));
        message.resetStyle().append(sanitize(resource.getResourceName()));

        out.println(message);
    }

    @Override
    public void transferCorrupted(TransferEvent event) throws TransferCancelledException {
        TransferResource resource = event.getResource();
        // TODO This needs to be colorized
        out.println("[WARNING] " + sanitize(event.getException().getMessage()) + " from "
                + sanitize(resource.getRepositoryId()) + " for " + sanitize(resource.getRepositoryUrl())
                + sanitize(resource.getResourceName()));
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        String action = (event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploaded" : "Downloaded");
        String direction = event.getRequestType() == TransferEvent.RequestType.PUT ? "to" : "from";

        TransferResource resource = event.getResource();
        long contentLength = event.getTransferredBytes();
        FileSizeFormat format = new FileSizeFormat();

        MessageBuilder message = messageBuilderFactory.builder();
        message.append(action).style(STYLE).append(' ').append(direction).append(' ');
        message.resetStyle().append(sanitize(resource.getRepositoryId()));
        message.style(STYLE).append(": ").append(sanitize(resource.getRepositoryUrl()));
        message.resetStyle().append(sanitize(resource.getResourceName()));
        message.style(STYLE).append(" (").append(format.format(contentLength));

        Duration duration = Duration.between(resource.getStartTime(), MonotonicClock.now());
        long nanos = duration.toNanos();
        if (nanos > 0) {
            double seconds = nanos / (double) TimeUnit.SECONDS.toNanos(1); // Convert to fractional seconds
            double bytesPerSecond = contentLength / seconds;
            message.append(" at ");
            format.formatRate(message, bytesPerSecond);
        }

        message.append(')').resetStyle();
        out.println(message);
    }
}
