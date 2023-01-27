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
package org.apache.maven.extension.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.project.ExtensionDescriptor;
import org.apache.maven.project.ExtensionDescriptorBuilder;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

/**
 * Provides information about artifacts (identified by groupId:artifactId string key) and classpath elements exported by
 * Maven core itself or a Maven core extension.
 *
 * @since 3.3.0
 */
public class CoreExtensionEntry {
    private final ClassRealm realm;

    private final Set<String> artifacts;

    private final Set<String> packages;

    public CoreExtensionEntry(ClassRealm realm, Collection<String> artifacts, Collection<String> packages) {
        this.realm = realm;
        this.artifacts = Collections.unmodifiableSet(new HashSet<>(artifacts));
        this.packages = Collections.unmodifiableSet(new HashSet<>(packages));
    }

    /**
     * Returns ClassLoader used to load extension classes.
     */
    public ClassRealm getClassRealm() {
        return realm;
    }

    /**
     * Returns artifacts exported by the extension, identified by groupId:artifactId string key.
     */
    public Set<String> getExportedArtifacts() {
        return artifacts;
    }

    /**
     * Returns classpath elements exported by the extension.
     */
    public Set<String> getExportedPackages() {
        return packages;
    }

    private static final ExtensionDescriptorBuilder BUILDER = new ExtensionDescriptorBuilder();

    public static CoreExtensionEntry discoverFrom(ClassRealm loader) {
        Set<String> artifacts = new LinkedHashSet<>();
        Set<String> packages = new LinkedHashSet<>();

        try {
            Enumeration<URL> urls = loader.getResources(BUILDER.getExtensionDescriptorLocation());
            while (urls.hasMoreElements()) {

                try (InputStream is = urls.nextElement().openStream()) {
                    ExtensionDescriptor descriptor = BUILDER.build(is);
                    artifacts.addAll(descriptor.getExportedArtifacts());
                    packages.addAll(descriptor.getExportedPackages());
                }
            }
        } catch (IOException ignored) {
            // exports descriptors are entirely optional
        }

        return new CoreExtensionEntry(loader, artifacts, packages);
    }

    public static CoreExtensionEntry discoverFrom(ClassRealm loader, Collection<File> classpath) {
        Set<String> artifacts = new LinkedHashSet<>();
        Set<String> packages = new LinkedHashSet<>();

        try {
            for (File entry : classpath) {
                ExtensionDescriptor descriptor = BUILDER.build(entry);
                if (descriptor != null) {
                    artifacts.addAll(descriptor.getExportedArtifacts());
                    packages.addAll(descriptor.getExportedPackages());
                }
            }
        } catch (IOException ignored) {
            // exports descriptors are entirely optional
        }

        return new CoreExtensionEntry(loader, artifacts, packages);
    }
}
