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
package org.apache.maven.jline;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import org.apache.maven.api.services.MavenException;
import org.jline.terminal.Attributes;
import org.jline.terminal.Cursor;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalExt;
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.ColorPalette;
import org.jline.utils.InfoCmp;
import org.jline.utils.NonBlockingReader;

public class FastTerminal implements TerminalExt {

    private final CompletableFuture<Terminal> terminal;

    public FastTerminal(Callable<Terminal> builder, Consumer<Terminal> consumer) {
        this.terminal = new CompletableFuture<>();
        new Thread(
                        () -> {
                            try {
                                Terminal term = builder.call();
                                consumer.accept(term);
                                terminal.complete(term);
                            } catch (Exception e) {
                                terminal.completeExceptionally(new MavenException(e));
                            }
                        },
                        "fast-terminal-thread")
                .start();
    }

    public TerminalExt getTerminal() {
        try {
            return (TerminalExt) terminal.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        return getTerminal().getName();
    }

    @Override
    public SignalHandler handle(Signal signal, SignalHandler signalHandler) {
        return getTerminal().handle(signal, signalHandler);
    }

    @Override
    public void raise(Signal signal) {
        getTerminal().raise(signal);
    }

    @Override
    public NonBlockingReader reader() {
        return getTerminal().reader();
    }

    @Override
    public PrintWriter writer() {
        return getTerminal().writer();
    }

    @Override
    public Charset encoding() {
        return getTerminal().encoding();
    }

    @Override
    public InputStream input() {
        return getTerminal().input();
    }

    @Override
    public OutputStream output() {
        return getTerminal().output();
    }

    @Override
    public boolean canPauseResume() {
        return getTerminal().canPauseResume();
    }

    @Override
    public void pause() {
        getTerminal().pause();
    }

    @Override
    public void pause(boolean b) throws InterruptedException {
        getTerminal().pause(b);
    }

    @Override
    public void resume() {
        getTerminal().resume();
    }

    @Override
    public boolean paused() {
        return getTerminal().paused();
    }

    @Override
    public Attributes enterRawMode() {
        return getTerminal().enterRawMode();
    }

    @Override
    public boolean echo() {
        return getTerminal().echo();
    }

    @Override
    public boolean echo(boolean b) {
        return getTerminal().echo(b);
    }

    @Override
    public Attributes getAttributes() {
        return getTerminal().getAttributes();
    }

    @Override
    public void setAttributes(Attributes attributes) {
        getTerminal().setAttributes(attributes);
    }

    @Override
    public Size getSize() {
        return getTerminal().getSize();
    }

    @Override
    public void setSize(Size size) {
        getTerminal().setSize(size);
    }

    @Override
    public int getWidth() {
        return getTerminal().getWidth();
    }

    @Override
    public int getHeight() {
        return getTerminal().getHeight();
    }

    @Override
    public Size getBufferSize() {
        return getTerminal().getBufferSize();
    }

    @Override
    public void flush() {
        getTerminal().flush();
    }

    @Override
    public String getType() {
        return getTerminal().getType();
    }

    @Override
    public boolean puts(InfoCmp.Capability capability, Object... objects) {
        return getTerminal().puts(capability, objects);
    }

    @Override
    public boolean getBooleanCapability(InfoCmp.Capability capability) {
        return getTerminal().getBooleanCapability(capability);
    }

    @Override
    public Integer getNumericCapability(InfoCmp.Capability capability) {
        return getTerminal().getNumericCapability(capability);
    }

    @Override
    public String getStringCapability(InfoCmp.Capability capability) {
        return getTerminal().getStringCapability(capability);
    }

    @Override
    public Cursor getCursorPosition(IntConsumer intConsumer) {
        return getTerminal().getCursorPosition(intConsumer);
    }

    @Override
    public boolean hasMouseSupport() {
        return getTerminal().hasMouseSupport();
    }

    @Override
    public MouseTracking getCurrentMouseTracking() {
        return getTerminal().getCurrentMouseTracking();
    }

    @Override
    public boolean trackMouse(MouseTracking mouseTracking) {
        return getTerminal().trackMouse(mouseTracking);
    }

    @Override
    public MouseEvent readMouseEvent() {
        return getTerminal().readMouseEvent();
    }

    @Override
    public MouseEvent readMouseEvent(IntSupplier intSupplier) {
        return getTerminal().readMouseEvent(intSupplier);
    }

    @Override
    public MouseEvent readMouseEvent(String prefix) {
        return getTerminal().readMouseEvent(prefix);
    }

    @Override
    public MouseEvent readMouseEvent(IntSupplier reader, String prefix) {
        return getTerminal().readMouseEvent(reader, prefix);
    }

    @Override
    public boolean hasFocusSupport() {
        return getTerminal().hasFocusSupport();
    }

    @Override
    public boolean trackFocus(boolean b) {
        return getTerminal().trackFocus(b);
    }

    @Override
    public ColorPalette getPalette() {
        return getTerminal().getPalette();
    }

    @Override
    public void close() throws IOException {
        getTerminal().close();
    }

    @Override
    public TerminalProvider getProvider() {
        return getTerminal().getProvider();
    }

    @Override
    public SystemStream getSystemStream() {
        return getTerminal().getSystemStream();
    }
}
