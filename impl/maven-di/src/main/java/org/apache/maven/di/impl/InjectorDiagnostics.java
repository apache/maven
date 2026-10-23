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
package org.apache.maven.di.impl;

import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.maven.di.Key;
import org.apache.maven.di.Scope;

/** Captures and serializes diagnostic output for one injector. */
final class InjectorDiagnostics {
    private static final String DEBUG_PROPERTY = "maven.di.debug";
    private static final long NO_CAPTURED_REVISION = -1;
    private static final String AMBIGUOUS = "ambiguous";

    private final InjectorImpl injector;
    private final Map<Key<?>, Set<Binding<?>>> bindings;
    private final Map<Class<? extends Annotation>, Supplier<Scope>> scopes;

    // Registration, as with the binding map itself, must not race with resolution.
    private volatile long registryRevision;
    private final Object diagnosticLock = new Object();
    private volatile DiagnosticState diagnosticState;
    private Deque<DiagnosticWork> diagnosticQueue;
    private Thread diagnosticWriter;

    InjectorDiagnostics(
            InjectorImpl injector,
            Map<Key<?>, Set<Binding<?>>> bindings,
            Map<Class<? extends Annotation>, Supplier<Scope>> scopes) {
        this.injector = injector;
        this.bindings = bindings;
        this.scopes = scopes;
    }

    void registryChanged() {
        registryRevision++;
    }

    void reset() {
        synchronized (diagnosticLock) {
            diagnosticState = null;
        }
    }

    String dumpBindings() {
        return BindingSnapshot.capture(bindings, scopes, new DiagnosticIdentity(), false, registryRevision)
                .render(injector, "explicit", false, null, injector.getBindingStyler());
    }

    void checkpoint(String reason, boolean partial) {
        DiagnosticMode mode = diagnosticMode();
        if (mode == DiagnosticMode.OFF) {
            if (diagnosticState != null) {
                reset();
            }
            return;
        }
        PrintStream output = System.out;
        DiagnosticState state = diagnosticState;
        if (!partial
                && state != null
                && state.mode == mode
                && state.capturedRevision == registryRevision
                && state.output.get() == output) {
            return;
        }
        writeBindings(output, reason, partial, mode);
    }

    private static DiagnosticMode diagnosticMode() {
        try {
            String property = System.getProperty(DEBUG_PROPERTY);
            if (property == null) {
                return DiagnosticMode.OFF;
            }
            if (property.isEmpty() || Boolean.parseBoolean(property)) {
                return DiagnosticMode.ALL;
            }
            return AMBIGUOUS.equalsIgnoreCase(property) ? DiagnosticMode.AMBIGUOUS : DiagnosticMode.OFF;
        } catch (RuntimeException e) {
            return DiagnosticMode.OFF;
        }
    }

    private void writeBindings(PrintStream output, String reason, boolean partial, DiagnosticMode mode) {
        DiagnosticWork work;
        synchronized (diagnosticLock) {
            DiagnosticState state = diagnosticState;
            if (state == null || state.mode != mode || state.output.get() != output) {
                state = new DiagnosticState(mode, output);
                diagnosticState = state;
            }
            long revision = registryRevision;
            if (!partial && state.capturedRevision == revision) {
                return;
            }
            try {
                BindingSnapshot snapshot = BindingSnapshot.capture(
                        bindings, scopes, state.identities, mode == DiagnosticMode.AMBIGUOUS, revision);
                work = new DiagnosticWork(state, snapshot, output, reason, partial);
                state.capturedRevision = revision;
                if (diagnosticQueue == null) {
                    diagnosticQueue = new ArrayDeque<>();
                }
                diagnosticQueue.addLast(work);
            } catch (RuntimeException e) {
                // Diagnostics must not replace a registration or resolution failure. Retry on the next checkpoint.
                return;
            }
            if (diagnosticWriter != null) {
                // A callback on the writer cannot wait for itself; its captured state is drained after this block.
                if (diagnosticWriter == Thread.currentThread()) {
                    return;
                }
                boolean interrupted = false;
                while (!work.done) {
                    try {
                        diagnosticLock.wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
            diagnosticWriter = Thread.currentThread();
        }
        drainDiagnostics();
    }

    private void drainDiagnostics() {
        try {
            while (true) {
                DiagnosticWork work;
                BindingSnapshot previous;
                synchronized (diagnosticLock) {
                    work = diagnosticQueue.pollFirst();
                    if (work == null) {
                        diagnosticWriter = null;
                        return;
                    }
                    if (diagnosticState != work.state) {
                        work.done = true;
                        diagnosticLock.notifyAll();
                        continue;
                    }
                    previous = work.state.emittedSnapshot;
                }
                try {
                    String dump = work.snapshot.render(
                            injector, work.reason, work.partial, previous, injector.getBindingStyler());
                    if (dump != null) {
                        injector.writeDump(work.output, dump);
                        synchronized (diagnosticLock) {
                            if (diagnosticState == work.state) {
                                work.state.emittedSnapshot = work.snapshot;
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    synchronized (diagnosticLock) {
                        work.state.emittedSnapshot = null;
                        work.state.capturedRevision = NO_CAPTURED_REVISION;
                    }
                } finally {
                    synchronized (diagnosticLock) {
                        work.done = true;
                        diagnosticLock.notifyAll();
                    }
                }
            }
        } finally {
            synchronized (diagnosticLock) {
                if (diagnosticWriter == Thread.currentThread()) {
                    diagnosticWriter = null;
                    diagnosticState = null;
                    diagnosticQueue.forEach(work -> work.done = true);
                    diagnosticQueue.clear();
                    diagnosticLock.notifyAll();
                }
            }
        }
    }

    private enum DiagnosticMode {
        OFF,
        ALL,
        AMBIGUOUS
    }

    private static final class DiagnosticWork {
        final DiagnosticState state;
        final BindingSnapshot snapshot;
        final PrintStream output;
        final String reason;
        final boolean partial;
        boolean done;

        DiagnosticWork(
                DiagnosticState state, BindingSnapshot snapshot, PrintStream output, String reason, boolean partial) {
            this.state = state;
            this.snapshot = snapshot;
            this.output = output;
            this.reason = reason;
            this.partial = partial;
        }
    }

    private static final class DiagnosticState {
        final DiagnosticIdentity identities = new DiagnosticIdentity();
        final DiagnosticMode mode;
        volatile long capturedRevision = NO_CAPTURED_REVISION;
        // A reused injector must not retain a previous embedded invocation's output stream.
        final WeakReference<PrintStream> output;
        BindingSnapshot emittedSnapshot;

        DiagnosticState(DiagnosticMode mode, PrintStream output) {
            this.mode = mode;
            this.output = new WeakReference<>(output);
        }
    }
}
