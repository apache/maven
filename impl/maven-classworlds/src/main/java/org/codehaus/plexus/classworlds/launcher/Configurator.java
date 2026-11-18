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

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

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
import java.util.LinkedHashMap;
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

    /** The launcher to configure. */
    private Launcher launcher;

    private ClassWorld world;

    /** Processed realms, keyed by realm name. */
    private Map<String, ClassRealm> configuredRealms;

    /** Current realm being configured (set by {@code [realmName]} sections). */
    private ClassRealm curRealm;

    private ClassLoader foreignClassLoader = null;

    /** Current layer being configured (set by {@code [layer name]} sections), or {@code null} if in a realm section. */
    private String curLayerName;

    /** Named JPMS module layers: layer name → list of module-path JARs. Insertion order defines the classloader chain. */
    private final LinkedHashMap<String, List<Path>> namedLayerPaths = new LinkedHashMap<>();

    /** Deferred realm-to-layer parent assignments: realm id → layer name. Applied after layers are created. */
    private final Map<String, String> realmParentLayers = new HashMap<>();

    /** Deferred import-exported directives: [targetRealmId, sourceRealmId]. Applied after realms are created. */
    private final List<String[]> realmExportedImports = new ArrayList<>();

    private List<String[]> exportDirectives = new ArrayList<>();

    private List<String[]> openDirectives = new ArrayList<>();

    private List<String[]> readDirectives = new ArrayList<>();

    /** ClassLoaders created for each named layer, keyed by layer name. */
    private final Map<String, ClassLoader> layerClassLoaders = new LinkedHashMap<>();

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
     * Set the ClassWorld to configure.
     * This setter is provided so you can use the same configurator to configure several "worlds".
     *
     * @param world The classWorld to configure.
     */
    public void setClassWorld(ClassWorld world) {
        this.world = world;
        configuredRealms = new HashMap<>();
    }

    /**
     * Configure from a stream.
     *
     * @param is The config input stream.
     * @throws IOException             If an error occurs reading the config file.
     * @throws MalformedURLException   If the config file contains invalid URLs.
     * @throws ConfigurationException  If the config file is corrupt.
     * @throws DuplicateRealmException If the config file defines two realms with the same id.
     * @throws NoSuchRealmException    If the config file references a non-existent realm.
     */
    public void configure(InputStream is)
            throws IOException, ConfigurationException, DuplicateRealmException, NoSuchRealmException {
        if (world == null) {
            world = new ClassWorld();
        }
        curRealm = null;
        curLayerName = null;
        foreignClassLoader = null;
        namedLayerPaths.clear();
        realmParentLayers.clear();
        realmExportedImports.clear();
        exportDirectives = new ArrayList<>();
        openDirectives = new ArrayList<>();
        readDirectives = new ArrayList<>();
        layerClassLoaders.clear();
        if (this.launcher != null) {
            foreignClassLoader = this.launcher.getSystemClassLoader();
        }
        ConfigurationParser parser = new ConfigurationParser(this, System.getProperties());
        parser.parse(is);
        createModuleLayers();
        applyRealmParentLayers();
        applyExportedImports();
        associateRealms();
        if (this.launcher != null) {
            this.launcher.setWorld(world);
        }
    }

    /**
     * Associate parent realms with their children based on realm name hierarchy.
     * Realms that have an explicit {@code parent layer} directive are skipped —
     * their parent classloader was already set by {@link #applyRealmParentLayers()}.
     */
    // TODO return this to protected when the legacy wrappers can be removed.
    public void associateRealms() {
        List<String> sortRealmNames = new ArrayList<>(configuredRealms.keySet());
        sortRealmNames.sort(String::compareTo);
        for (String realmName : sortRealmNames) {
            if (realmParentLayers.containsKey(realmName)) {
                continue;
            }
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

    @Override
    public void addLayer(String layerName) {
        curLayerName = layerName;
        curRealm = null;
        namedLayerPaths.computeIfAbsent(layerName, k -> new ArrayList<>());
    }

    @Override
    public void addImportFrom(String realmName, String importSpec) throws NoSuchRealmException {
        curRealm.importFrom(realmName, importSpec);
    }

    @Override
    public void addLoadFile(File file) {
        try {
            curRealm.addURL(file.toURI().toURL());
        } catch (MalformedURLException e) {
            // can't really happen
        }
    }

    @Override
    public void addLoadURL(URL url) {
        curRealm.addURL(url);
    }

    @Override
    public void addRealm(String realmName) throws DuplicateRealmException {
        curLayerName = null;
        curRealm = world.newRealm(realmName, foreignClassLoader);
        configuredRealms.put(realmName, curRealm);
    }

    @Override
    public void addModuleFile(File file) {
        if (curLayerName != null) {
            namedLayerPaths.get(curLayerName).add(file.toPath());
        }
    }

    @Override
    public void setRealmParentLayer(String layerName) {
        if (curRealm != null) {
            realmParentLayers.put(curRealm.getId(), layerName);
        }
    }

    @Override
    public void importExportedFrom(String realmName) throws NoSuchRealmException {
        if (curRealm != null) {
            realmExportedImports.add(new String[] {curRealm.getId(), realmName});
        }
    }

    public List<Path> getModulePaths() {
        List<Path> all = new ArrayList<>();
        namedLayerPaths.values().forEach(all::addAll);
        return all;
    }

    @Override
    public void addExports(String module, String pkg, String target) {
        exportDirectives.add(new String[] {module, pkg, target});
    }

    @Override
    public void addOpens(String module, String pkg, String target) {
        openDirectives.add(new String[] {module, pkg, target});
    }

    @Override
    public void addReads(String source, String target) {
        readDirectives.add(new String[] {source, target});
    }

    private void createModuleLayers() {
        if (namedLayerPaths.isEmpty()
                && exportDirectives.isEmpty()
                && openDirectives.isEmpty()
                && readDirectives.isEmpty()) {
            return;
        }

        Set<String> bootModuleNames =
                ModuleLayer.boot().modules().stream().map(Module::getName).collect(Collectors.toSet());

        ClassLoader baseParent = foreignClassLoader != null ? foreignClassLoader : ClassLoader.getSystemClassLoader();
        ClassLoader chainedLoader = baseParent;

        for (Map.Entry<String, List<Path>> entry : namedLayerPaths.entrySet()) {
            String layerName = entry.getKey();
            List<Path> paths = entry.getValue();
            if (paths.isEmpty()) {
                continue;
            }

            try {
                ModuleFinder finder = ModuleFinder.of(paths.toArray(new Path[0]));
                Set<String> newModules = finder.findAll().stream()
                        .map(ref -> ref.descriptor().name())
                        .filter(name -> !bootModuleNames.contains(name))
                        .collect(Collectors.toSet());

                if (newModules.isEmpty()) {
                    continue;
                }

                // All layers are siblings under boot — JPMS parent is always boot.
                // ClassLoader parent chains through previous layers so plexus.core
                // can reach all layer classes via delegation.
                Configuration cf = ModuleLayer.boot().configuration().resolve(finder, ModuleFinder.of(), newModules);
                ModuleLayer.Controller controller =
                        ModuleLayer.defineModulesWithOneLoader(cf, List.of(ModuleLayer.boot()), chainedLoader);
                ModuleLayer layer = controller.layer();
                ClassLoader layerLoader = layer.findLoader(newModules.iterator().next());

                world.setModuleLayer(layerName, layer, controller);
                layerClassLoaders.put(layerName, layerLoader);
                chainedLoader = layerLoader;

                LOGGER.log(
                        System.Logger.Level.DEBUG,
                        "Created JPMS layer ''{0}'' with modules: {1}",
                        layerName,
                        newModules);

            } catch (FindException | ResolutionException | LayerInstantiationException e) {
                LOGGER.log(
                        System.Logger.Level.DEBUG,
                        "Failed to create JPMS layer '" + layerName + "', falling back to classpath: "
                                + e.getMessage());
                addLayerPathsToClasspath(paths);
            }
        }

        // Update foreignClassLoader to the topmost layer's loader
        if (chainedLoader != baseParent) {
            foreignClassLoader = chainedLoader;
        }

        // Apply module access directives
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

    private void applyRealmParentLayers() {
        for (Map.Entry<String, String> entry : realmParentLayers.entrySet()) {
            String realmId = entry.getKey();
            String layerName = entry.getValue();
            ClassLoader layerLoader = layerClassLoaders.get(layerName);
            if (layerLoader != null) {
                ClassRealm realm = configuredRealms.get(realmId);
                if (realm != null) {
                    realm.setParentClassLoader(layerLoader);
                }
            }
        }
    }

    private void applyExportedImports() throws NoSuchRealmException {
        for (String[] pair : realmExportedImports) {
            String targetRealmId = pair[0];
            String sourceRealmId = pair[1];
            ClassRealm targetRealm = configuredRealms.get(targetRealmId);
            ClassRealm sourceRealm = configuredRealms.get(sourceRealmId);
            if (targetRealm == null) {
                throw new NoSuchRealmException(world, targetRealmId);
            }
            if (sourceRealm == null) {
                throw new NoSuchRealmException(world, sourceRealmId);
            }
            // Scan the source realm for META-INF/maven/extension.xml exported packages
            discoverAndImportExported(targetRealm, sourceRealm);
        }
    }

    private void discoverAndImportExported(ClassRealm targetRealm, ClassRealm sourceRealm) {
        try {
            var urls = sourceRealm.getResources("META-INF/maven/extension.xml");
            XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
            xmlFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            while (urls.hasMoreElements()) {
                var url = urls.nextElement();
                try (var is = url.openStream()) {
                    XMLStreamReader reader = xmlFactory.createXMLStreamReader(is);
                    boolean inPackages = false;
                    while (reader.hasNext()) {
                        int event = reader.next();
                        if (event == XMLStreamConstants.START_ELEMENT) {
                            if ("exportedPackages".equals(reader.getLocalName())) {
                                inPackages = true;
                            } else if (inPackages && "exportedPackage".equals(reader.getLocalName())) {
                                String pkg = reader.getElementText().trim();
                                if (!pkg.isEmpty()) {
                                    try {
                                        targetRealm.importFrom(sourceRealm.getId(), pkg);
                                    } catch (NoSuchRealmException e) {
                                        LOGGER.log(
                                                System.Logger.Level.DEBUG,
                                                "Cannot import ''{0}'' from realm ''{1}''",
                                                pkg,
                                                sourceRealm.getId());
                                    }
                                }
                            }
                        } else if (event == XMLStreamConstants.END_ELEMENT) {
                            if ("exportedPackages".equals(reader.getLocalName())) {
                                inPackages = false;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(
                    System.Logger.Level.DEBUG,
                    "Failed to read extension descriptors from realm " + sourceRealm.getId(),
                    e);
        }
    }

    private void addLayerPathsToClasspath(List<Path> paths) {
        if (curRealm != null) {
            for (Path path : paths) {
                try {
                    curRealm.addURL(path.toUri().toURL());
                } catch (MalformedURLException e) {
                    // ignore
                }
            }
        }
    }

    private Module resolveTargetModule(String targetName, Module unnamedTarget) {
        if ("ALL-UNNAMED".equals(targetName)) {
            return unnamedTarget;
        }
        for (ModuleLayer layer : world.getModuleLayers().values()) {
            Module m = layer.findModule(targetName).orElse(null);
            if (m != null) {
                return m;
            }
        }
        return ModuleLayer.boot().findModule(targetName).orElse(null);
    }

    @Override
    public void setAppMain(String mainClassName, String mainRealmName) {
        if (this.launcher != null) {
            this.launcher.setAppMain(mainClassName, mainRealmName);
        }
    }
}
