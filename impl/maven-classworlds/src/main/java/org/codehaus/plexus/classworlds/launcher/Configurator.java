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
package org.codehaus.plexus.classworlds.launcher;

/*
 * Copyright 2001-2006 Codehaus Foundation.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;

/**
 * <code>Launcher</code> configurator.
 *
 * @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 * @author Jason van Zyl
 */
public class Configurator implements ConfigurationHandler {
    /**
     * The launcher to configure.
     */
    private Launcher launcher;

    private ClassWorld world;

    /**
     * Processed Realms.
     */
    private Map<String, ClassRealm> configuredRealms;

    /**
     * Current Realm.
     */
    private ClassRealm curRealm;

    private ClassLoader foreignClassLoader = null;

    /**
     * Construct.
     *
     * @param launcher The launcher to configure.
     */
    public Configurator(Launcher launcher) {
        this.launcher = launcher;

        configuredRealms = new HashMap<>();

        if (launcher != null) {
            this.foreignClassLoader = launcher.getSystemClassLoader();
        }
    }

    /**
     * Construct.
     *
     * @param world The classWorld to configure.
     */
    public Configurator(ClassWorld world) {
        setClassWorld(world);
    }

    /**
     * set world.
     * this setter is provided so you can use the same configurator to configure several "worlds"
     *
     * @param world The classWorld to configure.
     */
    public void setClassWorld(ClassWorld world) {
        this.world = world;

        configuredRealms = new HashMap<>();
    }

    /**
     * Configure from a file.
     *
     * @param is The config input stream
     * @throws IOException             If an error occurs reading the config file.
     * @throws MalformedURLException   If the config file contains invalid URLs.
     * @throws ConfigurationException  If the config file is corrupt.
     * @throws org.codehaus.plexus.classworlds.realm.DuplicateRealmException If the config file defines two realms with the same id.
     * @throws org.codehaus.plexus.classworlds.realm.NoSuchRealmException    If the config file defines a main entry point in
     *                                 a non-existent realm.
     */
    public void configure(InputStream is)
            throws IOException, ConfigurationException, DuplicateRealmException, NoSuchRealmException {
        if (world == null) {
            world = new ClassWorld();
        }

        curRealm = null;

        foreignClassLoader = null;

        if (this.launcher != null) {
            foreignClassLoader = this.launcher.getSystemClassLoader();
        }

        ConfigurationParser parser = new ConfigurationParser(this, System.getProperties());

        parser.parse(is);

        // Associate child realms to their parents.
        associateRealms();

        if (this.launcher != null) {
            this.launcher.setWorld(world);
        }
    }

    // TODO return this to protected when the legacy wrappers can be removed.
    /**
     * Associate parent realms with their children.
     */
    public void associateRealms() {
        List<String> sortRealmNames = new ArrayList<>(configuredRealms.keySet());

        // sort by name
        sortRealmNames.sort(String::compareTo);

        // So now we have something like the following for defined
        // realms:
        //
        // root
        // root.maven
        // root.maven.plugin
        //
        // Now if the name of a realm is a superset of an existing realm
        // the we want to make child/parent associations.

        for (String realmName : sortRealmNames) {
            int j = realmName.lastIndexOf('.');

            if (j > 0) {
                String parentRealmName = realmName.substring(0, j);

                ClassRealm parentRealm = configuredRealms.get(parentRealmName);

                if (parentRealm != null) {
                    ClassRealm realm = configuredRealms.get(realmName);

                    realm.setParentRealm(parentRealm);
                }
            }
        }
    }

    public void addImportFrom(String relamName, String importSpec) throws NoSuchRealmException {
        curRealm.importFrom(relamName, importSpec);
    }

    public void addLoadFile(File file) {
        try {
            curRealm.addURL(file.toURI().toURL());
        } catch (MalformedURLException e) {
            // can't really happen... or can it?
        }
    }

    public void addLoadURL(URL url) {
        curRealm.addURL(url);
    }

    public void addRealm(String realmName) throws DuplicateRealmException {
        curRealm = world.newRealm(realmName, foreignClassLoader);

        // Stash the configured realm for subsequent association processing.
        configuredRealms.put(realmName, curRealm);
    }

    public void setAppMain(String mainClassName, String mainRealmName) {
        if (this.launcher != null) {
            this.launcher.setAppMain(mainClassName, mainRealmName);
        }
    }
}
