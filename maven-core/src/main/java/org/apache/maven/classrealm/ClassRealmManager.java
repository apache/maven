package org.apache.maven.classrealm;

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

import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.artifact.Artifact;

/**
 * Manages the class realms used by Maven. <strong>Warning:</strong> This is an internal utility interface that is only
 * public for technical reasons, it is not part of the public API. In particular, this interface can be changed or
 * deleted without prior notice.
 *
 * @author Benjamin Bentmann
 */
public interface ClassRealmManager
{

    /**
     * Gets the class realm hosting the Maven core.
     *
     * @return The class realm hosting the Maven core, never {@code null}.
     */
    ClassRealm getCoreRealm();

    /**
     * Gets the class realm exposing the Maven API. This is basically a restricted view on the Maven core realm.
     *
     * @return The class realm exposing the Maven API, never {@code null}.
     */
    ClassRealm getMavenApiRealm();

    /**
     * Creates a new class realm for the specified project and its build extensions.
     *
     * @param model The model of the project for which to create a realm, must not be {@code null}.
     * @param artifacts The artifacts to add to the class realm, may be {@code null}. Unresolved artifacts (i.e. with a
     *            missing file) will automatically be excluded from the realm.
     * @return The new project realm, never {@code null}.
     */
    ClassRealm createProjectRealm( Model model, List<Artifact> artifacts );

    /**
     * Creates a new class realm for the specified build extension.
     *
     * @param extension The extension plugin for which to create a realm, must not be {@code null}.
     * @param artifacts The artifacts to add to the class realm, may be {@code null}. Unresolved artifacts (i.e. with a
     *            missing file) will automatically be excluded from the realm.
     * @return The new extension realm, never {@code null}.
     */
    ClassRealm createExtensionRealm( Plugin extension, List<Artifact> artifacts );

    /**
     * Creates a new class realm for the specified plugin.
     *
     * @param plugin The plugin for which to create a realm, must not be {@code null}.
     * @param parent The parent realm for the new realm, may be {@code null}.
     * @param parentImports The packages/types to import from the parent realm, may be {@code null}.
     * @param foreignImports The packages/types to import from foreign realms, may be {@code null}.
     * @param artifacts The artifacts to add to the class realm, may be {@code null}. Unresolved artifacts (i.e. with a
     *            missing file) will automatically be excluded from the realm.
     * @return The new plugin realm, never {@code null}.
     */
    ClassRealm createPluginRealm( Plugin plugin, ClassLoader parent, List<String> parentImports,
                                  Map<String, ClassLoader> foreignImports, List<Artifact> artifacts );

}
