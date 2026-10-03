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
package org.codehaus.classworlds;

/*
 * Copyright 2001-2010 Codehaus Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

/**
 * A compatibility wrapper for {@link org.codehaus.plexus.classworlds.launcher.Configurator}
 * provided for legacy code.
 *
 * <p><b>Note:</b> This is a legacy class provided for backward compatibility with Maven 2.
 * New code should use {@link org.codehaus.plexus.classworlds.launcher.Configurator}.</p>
 *
 * @author Andrew Williams
 * @deprecated Use {@link org.codehaus.plexus.classworlds.launcher.Configurator}
 */
@Deprecated
public class Configurator {
    private final ConfiguratorAdapter config;

    /** Construct.
     *
     *  @param launcher The launcher to configure.
     */
    public Configurator(Launcher launcher) {
        config = ConfiguratorAdapter.getInstance(
                new org.codehaus.plexus.classworlds.launcher.Configurator(launcher), launcher);
    }

    /** Construct.
     *
     *  @param world The classWorld to configure.
     */
    public Configurator(ClassWorld world) {
        config = ConfiguratorAdapter.getInstance(
                new org.codehaus.plexus.classworlds.launcher.Configurator(ClassWorldReverseAdapter.getInstance(world)),
                world);
    }

    /** set world.
     *  this setter is provided so you can use the same configurator to configure several "worlds"
     *
     *  @param world The classWorld to configure.
     */
    public void setClassWorld(ClassWorld world) {
        config.setClassWorld(world);
    }

    /**
     * Configure from a file.
     *
     * @param is The config input stream
     * @throws IOException             If an error occurs reading the config file.
     * @throws MalformedURLException   If the config file contains invalid URLs.
     * @throws ConfigurationException  If the config file is corrupt.
     * @throws DuplicateRealmException If the config file defines two realms with the same id.
     * @throws NoSuchRealmException    If the config file defines a main entry point in
     *                                 a non-existent realm.
     */
    public void configure(InputStream is)
            throws IOException, MalformedURLException, ConfigurationException, DuplicateRealmException,
                    NoSuchRealmException {
        config.configureAdapter(is);
    }

    /**
     * Associate parent realms with their children.
     */
    protected void associateRealms() {
        config.associateRealms();
    }

    /**
     * Load a glob into the specified classloader.
     *
     * @param line  The path configuration line.
     * @param realm The realm to populate
     * @throws MalformedURLException If the line does not represent
     *                               a valid path element.
     * @throws FileNotFoundException If the line does not represent
     *                               a valid path element in the filesystem.
     */
    protected void loadGlob(String line, ClassRealm realm) throws MalformedURLException, FileNotFoundException {
        loadGlob(line, realm, false);
    }

    /**
     * Load a glob into the specified classloader.
     *
     * @param line  The path configuration line.
     * @param realm The realm to populate
     * @param optionally Whether the path is optional or required
     * @throws MalformedURLException If the line does not represent
     *                               a valid path element.
     * @throws FileNotFoundException If the line does not represent
     *                               a valid path element in the filesystem.
     */
    @SuppressWarnings("RedundantThrows")
    protected void loadGlob(String line, ClassRealm realm, boolean optionally)
            throws MalformedURLException, FileNotFoundException {
        config.loadGlob(line, realm, optionally);
    }

    /**
     * Filter a string for system properties.
     *
     * @param text The text to filter.
     * @return The filtered text.
     * @throws ConfigurationException If the property does not
     *                                exist or if there is a syntax error.
     */
    @SuppressWarnings("RedundantThrows")
    protected String filter(String text) throws ConfigurationException {
        return config.filter(text);
    }
}
