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

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.TransferListenerConfiguration;
import org.apache.maven.execution.TransferListenerFactory;
import org.apache.maven.internal.aether.transfer.Slf4jTransferListener;
import org.apache.maven.internal.aether.transfer.SummaryTransferListener;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferListener;

/**
 * Maven internal component that bridges container "shut down" to {@link RepositorySystem#shutdown()}.
 *
 * @since 3.9.0
 */
@Named
@Singleton
public final class DefaultTransferListenerFactory extends AbstractEventSpy implements TransferListenerFactory {

    private static final String HANDLE_KEY = DefaultTransferListenerFactory.class.getName() + ".handle";

    private static final TransferListener QUIET = new AbstractTransferListener() {};

    @Override
    public void onEvent(Object event) {
        if (event instanceof ExecutionEvent) {
            ExecutionEvent executionEvent = (ExecutionEvent) event;
            if (executionEvent.getType() == ExecutionEvent.Type.SessionEnded) {
                reportTransportSummary(executionEvent.getSession().getRepositorySession());
            }
        }
    }

    @Override
    public TransferListener createTransferListener(
            RepositorySystemSession session, TransferListenerConfiguration transferListenerConfiguration) {
        return (TransferListener) session.getData().computeIfAbsent(HANDLE_KEY, () -> {
            if (transferListenerConfiguration.getMode() == TransferListenerConfiguration.Mode.QUIET) {
                return QUIET;
            } else if (transferListenerConfiguration.getMode() == TransferListenerConfiguration.Mode.CLASSIC) {
                return new Slf4jTransferListener(
                        transferListenerConfiguration.isParallel(),
                        transferListenerConfiguration.isColored(),
                        true,
                        transferListenerConfiguration.isProgress(),
                        transferListenerConfiguration.isVerbose());
            } else if (transferListenerConfiguration.getMode() == TransferListenerConfiguration.Mode.CLASSIC_LIGHT) {
                return new Slf4jTransferListener(
                        transferListenerConfiguration.isParallel(),
                        transferListenerConfiguration.isColored(),
                        false,
                        transferListenerConfiguration.isProgress(),
                        transferListenerConfiguration.isVerbose());
            } else if (transferListenerConfiguration.getMode() == TransferListenerConfiguration.Mode.SUMMARY) {
                return new SummaryTransferListener(
                        transferListenerConfiguration.isParallel(),
                        transferListenerConfiguration.isColored(),
                        transferListenerConfiguration.isProgress(),
                        transferListenerConfiguration.isVerbose());
            } else {
                throw new IllegalArgumentException("Unsupported config: " + transferListenerConfiguration);
            }
        });
    }

    private void reportTransportSummary(RepositorySystemSession session) {
        Object listener = session.getData().get(HANDLE_KEY);
        if (listener instanceof SummaryTransferListener) {
            ((SummaryTransferListener) listener).summarize();
        }
    }
}
