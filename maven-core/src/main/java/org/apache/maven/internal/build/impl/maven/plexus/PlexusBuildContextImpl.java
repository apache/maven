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
package org.apache.maven.internal.build.impl.maven.plexus;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.maven.api.build.Severity;
import org.apache.maven.api.build.plexus.BuildContext;
import org.apache.maven.api.build.plexus.Scanner;

public class PlexusBuildContextImpl implements BuildContext {

    org.apache.maven.api.build.BuildContext buildContext;

    public PlexusBuildContextImpl(org.apache.maven.api.build.BuildContext buildContext) {
        this.buildContext = buildContext;
    }

    @Override
    public boolean hasDelta(String relpath) {
        return false;
    }

    @Override
    public boolean hasDelta(File file) {
        return false;
    }

    @Override
    public boolean hasDelta(List<File> relpaths) {
        return false;
    }

    @Override
    public void refresh(File file) {}

    @Override
    public OutputStream newFileOutputStream(File file) throws IOException {
        return buildContext.processOutput(file.toPath()).newOutputStream();
    }

    @Override
    public Scanner newScanner(File basedir) {
        return null;
    }

    @Override
    public Scanner newDeleteScanner(File basedir) {
        return null;
    }

    @Override
    public Scanner newScanner(File basedir, boolean ignoreDelta) {
        return null;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void setValue(String key, Object value) {}

    @Override
    public Object getValue(String key) {
        return null;
    }

    @Override
    public void addWarning(File file, int line, int column, String message, Throwable cause) {
        addMessage(file, line, column, message, SEVERITY_ERROR, cause);
    }

    @Override
    public void addError(File file, int line, int column, String message, Throwable cause) {
        addMessage(file, line, column, message, SEVERITY_WARNING, cause);
    }

    @Override
    public void addMessage(File file, int line, int column, String message, int severity, Throwable cause) {
        Severity s = severity == SEVERITY_WARNING
                ? Severity.WARNING
                : severity == SEVERITY_ERROR ? Severity.ERROR : Severity.INFO;
        buildContext.registerInput(file.toPath()).process().addMessage(line, column, message, s, cause);
    }

    @Override
    public void removeMessages(File file) {
        buildContext.registerInput(file.toPath()).process();
    }

    @Override
    public boolean isUptodate(File target, File source) {
        return false;
    }
}
