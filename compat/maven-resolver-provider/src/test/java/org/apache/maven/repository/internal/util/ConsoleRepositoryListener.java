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
package org.apache.maven.repository.internal.util;

import java.io.PrintStream;

import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;

public class ConsoleRepositoryListener extends AbstractRepositoryListener {

    private PrintStream out;

    public ConsoleRepositoryListener() {
        this(null);
    }

    public ConsoleRepositoryListener(PrintStream out) {
        this.out = (out != null) ? out : System.out;
    }

    @Override
    public void artifactDeployed(RepositoryEvent event) {
        println("artifactDeployed", event.getArtifact() + " to " + event.getRepository());
    }

    @Override
    public void artifactDeploying(RepositoryEvent event) {
        println("artifactDeploying", event.getArtifact() + " to " + event.getRepository());
    }

    @Override
    public void artifactDescriptorInvalid(RepositoryEvent event) {
        println(
                "artifactDescriptorInvalid",
                "for " + event.getArtifact() + ": " + event.getException().getMessage());
    }

    @Override
    public void artifactDescriptorMissing(RepositoryEvent event) {
        println("artifactDescriptorMissing", "for " + event.getArtifact());
    }

    @Override
    public void artifactInstalled(RepositoryEvent event) {
        println("artifactInstalled", event.getArtifact() + " to " + event.getFile());
    }

    @Override
    public void artifactInstalling(RepositoryEvent event) {
        println("artifactInstalling", event.getArtifact() + " to " + event.getFile());
    }

    @Override
    public void artifactResolved(RepositoryEvent event) {
        println("artifactResolved", event.getArtifact() + " from " + event.getRepository());
    }

    @Override
    public void artifactDownloading(RepositoryEvent event) {
        println("artifactDownloading", event.getArtifact() + " from " + event.getRepository());
    }

    @Override
    public void artifactDownloaded(RepositoryEvent event) {
        println("artifactDownloaded", event.getArtifact() + " from " + event.getRepository());
    }

    @Override
    public void artifactResolving(RepositoryEvent event) {
        println("artifactResolving", event.getArtifact().toString());
    }

    @Override
    public void metadataDeployed(RepositoryEvent event) {
        println("metadataDeployed", event.getMetadata() + " to " + event.getRepository());
    }

    @Override
    public void metadataDeploying(RepositoryEvent event) {
        println("metadataDeploying", event.getMetadata() + " to " + event.getRepository());
    }

    @Override
    public void metadataInstalled(RepositoryEvent event) {
        println("metadataInstalled", event.getMetadata() + " to " + event.getFile());
    }

    @Override
    public void metadataInstalling(RepositoryEvent event) {
        println("metadataInstalling", event.getMetadata() + " to " + event.getFile());
    }

    @Override
    public void metadataInvalid(RepositoryEvent event) {
        println("metadataInvalid", event.getMetadata().toString());
    }

    @Override
    public void metadataResolved(RepositoryEvent event) {
        println("metadataResolved", event.getMetadata() + " from " + event.getRepository());
    }

    @Override
    public void metadataResolving(RepositoryEvent event) {
        println("metadataResolving", event.getMetadata() + " from " + event.getRepository());
    }

    private void println(String event, String message) {
        out.println("Aether Repository - " + event + ": " + message);
    }
}
