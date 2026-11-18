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
import java.lang.module.Configuration;
import java.lang.module.FindException;
import java.lang.module.ModuleFinder;
import java.lang.module.ResolutionException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private static final System.Logger LOGGER = System.getLogger(Configurator.class.getName());

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

    private List<Path> modulePaths = new ArrayList<>();

    /**
     * Module JAR paths that could not be loaded into a JPMS layer and must be added to the
     * first realm that gets created instead (used when the {@code module} directive in the
     * configuration file precedes any {@code [section]} header, so no realm exists yet when
     * the fallback fires).
     */
    private List<Path> pendingModuleClasspathFallback = new ArrayList<>();

    private List<String[]> exportDirectives = new ArrayList<>();

    private List<String[]> openDirectives = new ArrayList<>();

    private List<String[]> readDirectives = new ArrayList<>();

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
     * @throws DuplicateRealmException If the config file defines two realms with the same id.
     * @throws NoSuchRealmException    If the config file defines a main entry point in
     *                                 a non-existent realm.
     */
    public void configure(InputStream is)
            throws IOException, ConfigurationException, DuplicateRealmException, NoSuchRealmException {
        if (world == null) {
            world = new ClassWorld();
        }
        curRealm = null;
        foreignClassLoader = null;
        modulePaths = new ArrayList<>();
        exportDirectives = new ArrayList<>();
        openDirectives = new ArrayList<>();
        readDirectives = new ArrayList<>();
        if (this.launcher != null) {
            foreignClassLoader = this.launcher.getSystemClassLoader();
        }
        ConfigurationParser parser = new ConfigurationParser(this, System.getProperties());
        parser.parse(is);
        createModuleLayer();
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
        // If the 'module' directive failed to create a JPMS layer before any section was
        // declared, drain the deferred classpath fallback into this first realm.
        if (!pendingModuleClasspathFallback.isEmpty()) {
            for (Path path : pendingModuleClasspathFallback) {
                try {
                    curRealm.addURL(path.toUri().toURL());
                } catch (MalformedURLException e) {
                    // ignore
                }
            }
            pendingModuleClasspathFallback.clear();
        }
        // Stash the configured realm for subsequent association processing.
        configuredRealms.put(realmName, curRealm);
    }

    public void addModuleFile(File file) {
        modulePaths.add(file.toPath());
    }

    public List<Path> getModulePaths() {
        return modulePaths;
    }

    public void addExports(String module, String pkg, String target) {
        exportDirectives.add(new String[] {module, pkg, target});
    }

    public void addOpens(String module, String pkg, String target) {
        openDirectives.add(new String[] {module, pkg, target});
    }

    public void addReads(String source, String target) {
        readDirectives.add(new String[] {source, target});
    }

    private void createModuleLayer() {
        if (modulePaths.isEmpty()
                && exportDirectives.isEmpty()
                && openDirectives.isEmpty()
                && readDirectives.isEmpty()) {
            return;
        }
        if (!modulePaths.isEmpty()) {
            try {
                Set<String> bootModuleNames = ModuleLayer.boot().modules().stream()
                        .map(Module::getName)
                        .collect(Collectors.toSet());

                // Partition module paths into two groups:
                //   - "infrastructure" modules (e.g. JLine) that plexus.core needs but plugins must NOT see
                //   - "api" modules (maven-api-*) that form the plugin-visible API surface
                // Both groups become siblings under ModuleLayer.boot() so that plugin layers can
                // declare the api layer as their sole JPMS parent and thereby be denied access to
                // the infrastructure layer.
                List<Path> apiPaths = new ArrayList<>();
                List<Path> infraPaths = new ArrayList<>();
                for (Path path : modulePaths) {
                    ModuleFinder singleFinder = ModuleFinder.of(path);
                    boolean isApi = singleFinder.findAll().stream()
                            .anyMatch(ref -> ref.descriptor().name().startsWith("org.apache.maven.api"));
                    if (isApi) {
                        apiPaths.add(path);
                    } else {
                        infraPaths.add(path);
                    }
                }

                ClassLoader baseParent =
                        foreignClassLoader != null ? foreignClassLoader : ClassLoader.getSystemClassLoader();

                // --- Infrastructure layer (JLine, etc.) — sibling of boot, NOT parent of plugin layers ---
                ModuleLayer infraLayer = ModuleLayer.boot();
                ClassLoader infraLoader = baseParent;
                if (!infraPaths.isEmpty()) {
                    ModuleFinder infraFinder = ModuleFinder.of(infraPaths.toArray(new Path[0]));
                    Set<String> infraModules = infraFinder.findAll().stream()
                            .map(ref -> ref.descriptor().name())
                            .filter(name -> !bootModuleNames.contains(name))
                            .collect(Collectors.toSet());
                    if (!infraModules.isEmpty()) {
                        Configuration infraCf = ModuleLayer.boot()
                                .configuration()
                                .resolve(infraFinder, ModuleFinder.of(), infraModules);
                        ModuleLayer.Controller infraController = ModuleLayer.defineModulesWithOneLoader(
                                infraCf, List.of(ModuleLayer.boot()), baseParent);
                        infraLayer = infraController.layer();
                        infraLoader =
                                infraLayer.findLoader(infraModules.iterator().next());
                        LOGGER.log(
                                System.Logger.Level.DEBUG,
                                "Created infrastructure JPMS layer with modules: {0}",
                                infraModules);
                    }
                }

                // --- Maven API layer — sibling of boot (and infra layer), parent of plugin layers ---
                // Its ClassLoader parent is infraLoader so that plexus.core (via foreignClassLoader)
                // can reach both infra (JLine) and api classes through normal ClassLoader delegation.
                // However, its JPMS parent is only ModuleLayer.boot(), NOT the infra layer, so plugin
                // layers that declare this as their sole JPMS parent cannot resolve infra modules.
                if (!apiPaths.isEmpty()) {
                    ModuleFinder apiFinder = ModuleFinder.of(apiPaths.toArray(new Path[0]));
                    Set<String> apiModules = apiFinder.findAll().stream()
                            .map(ref -> ref.descriptor().name())
                            .filter(name -> !bootModuleNames.contains(name))
                            .collect(Collectors.toSet());
                    if (!apiModules.isEmpty()) {
                        Configuration apiCf =
                                ModuleLayer.boot().configuration().resolve(apiFinder, ModuleFinder.of(), apiModules);
                        ModuleLayer.Controller apiController =
                                ModuleLayer.defineModulesWithOneLoader(apiCf, List.of(ModuleLayer.boot()), infraLoader);
                        ModuleLayer apiLayer = apiController.layer();
                        ClassLoader oldForeignClassLoader = foreignClassLoader;
                        foreignClassLoader =
                                apiLayer.findLoader(apiModules.iterator().next());
                        // Store the api layer in ClassWorld — DefaultClassRealmManager uses it as
                        // the JPMS parent when creating modular plugin layers, ensuring plugins can
                        // require maven-api-* but cannot require infra modules (e.g. JLine).
                        world.setModuleLayer(apiLayer, apiController);
                        LOGGER.log(
                                System.Logger.Level.DEBUG,
                                "Created Maven API JPMS layer with modules: {0}",
                                apiModules);
                        // Retroactively update realms that used the old foreignClassLoader so they
                        // inherit both infra and api classes via the new loader chain.
                        for (ClassRealm realm : configuredRealms.values()) {
                            if (realm.getParentClassLoader() == oldForeignClassLoader) {
                                realm.setParentClassLoader(foreignClassLoader);
                            }
                        }
                    }
                } else if (infraLayer != ModuleLayer.boot()) {
                    // No api JARs, but infra layer was created: store the infra layer for backward
                    // compatibility (pre-api-layer deployments, embedders, tests).
                    ClassLoader oldForeignClassLoader = foreignClassLoader;
                    foreignClassLoader = infraLoader;
                    world.setModuleLayer(infraLayer, null);
                    for (ClassRealm realm : configuredRealms.values()) {
                        if (realm.getParentClassLoader() == oldForeignClassLoader) {
                            realm.setParentClassLoader(foreignClassLoader);
                        }
                    }
                }
            } catch (FindException | ResolutionException | LayerInstantiationException e) {
                LOGGER.log(
                        System.Logger.Level.DEBUG,
                        "Failed to create JPMS module layer, falling back to classpath mode: " + e.getMessage());
                addModulePathsToClasspath();
            }
        }
        Module unnamedTarget = getClass().getModule();
        for (String[] directive : exportDirectives) {
            Module target = resolveTargetModule(directive[2], unnamedTarget);
            if (target != null) {
                world.addExports(directive[0], directive[1], target);
            }
        }
        for (String[] directive : openDirectives) {
            Module target = resolveTargetModule(directive[2], unnamedTarget);
            if (target != null) {
                world.addOpens(directive[0], directive[1], target);
            }
        }
        for (String[] directive : readDirectives) {
            Module target = resolveTargetModule(directive[1], unnamedTarget);
            if (target != null) {
                world.addReads(directive[0], target);
            }
        }
    }

    private void addModulePathsToClasspath() {
        if (curRealm != null) {
            for (Path path : modulePaths) {
                try {
                    curRealm.addURL(path.toUri().toURL());
                } catch (MalformedURLException e) {
                    // ignore
                }
            }
        } else {
            // The 'module' directive appeared before any [section]: no realm exists yet.
            // Defer adding these JARs to the classpath of the first realm to be created.
            pendingModuleClasspathFallback.addAll(modulePaths);
        }
    }

    private Module resolveTargetModule(String targetName, Module unnamedTarget) {
        if ("ALL-UNNAMED".equals(targetName)) {
            return unnamedTarget;
        }
        ModuleLayer layer = world.getModuleLayer();
        if (layer != null) {
            Module m = layer.findModule(targetName).orElse(null);
            if (m != null) {
                return m;
            }
        }
        return ModuleLayer.boot().findModule(targetName).orElse(null);
    }

    public void setAppMain(String mainClassName, String mainRealmName) {
        if (this.launcher != null) {
            this.launcher.setAppMain(mainClassName, mainRealmName);
        }
    }
}
