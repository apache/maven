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
package org.apache.maven.internal.build.impl.maven;

import javax.inject.Inject;

import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.stream.Stream;

import org.apache.maven.api.Project;
import org.apache.maven.api.build.Status;
import org.apache.maven.api.build.spi.FileState;
import org.apache.maven.api.build.spi.Workspace;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.internal.build.impl.FilesystemWorkspace;
import org.eclipse.sisu.Typed;

/**
 * Eclipse Workspace implementation is scoped to a project and does not "see" resources outside
 * project basedir. This implementation dispatches Workspace calls to either Eclipse implementation
 * or Filesystem workspace implementation, depending on whether requested resource is inside or
 * outside of project basedir.
 */
@Typed(ProjectWorkspace.class)
@MojoExecutionScoped
public class ProjectWorkspace implements Workspace {

    private final Workspace workspace;

    private final Project project;

    private final Path basedir;

    private final FilesystemWorkspace filesystem;

    @Inject
    public ProjectWorkspace(Project project, Workspace workspace, FilesystemWorkspace filesystem) {
        this.project = project;
        this.basedir = project.getBasedir().get().normalize();
        this.workspace = workspace;
        this.filesystem = filesystem;
    }

    protected Workspace getWorkspace(Path file) {
        if (file.normalize().startsWith(basedir)) {
            return workspace;
        }
        return filesystem;
    }

    @Override
    public Mode getMode() {
        return Mode.NORMAL;
    }

    @Override
    public Workspace escalate() {
        return new ProjectWorkspace(project, workspace.escalate(), filesystem);
    }

    @Override
    public boolean isPresent(Path file) {
        return getWorkspace(file).isPresent(file);
    }

    @Override
    public boolean isRegularFile(Path file) {
        return getWorkspace(file).isRegularFile(file);
    }

    @Override
    public boolean isDirectory(Path file) {
        return getWorkspace(file).isDirectory(file);
    }

    @Override
    public void deleteFile(Path file) {
        getWorkspace(file).deleteFile(file);
    }

    @Override
    public void processOutput(Path path) {
        getWorkspace(path).processOutput(path);
    }

    @Override
    public OutputStream newOutputStream(Path path) {
        return getWorkspace(path).newOutputStream(path);
    }

    @Override
    public Status getResourceStatus(Path file, FileTime lastModified, long size) {
        return getWorkspace(file).getResourceStatus(file, lastModified, size);
    }

    @Override
    public Stream<FileState> walk(Path basedir) {
        return getWorkspace(basedir).walk(basedir);
    }
}
