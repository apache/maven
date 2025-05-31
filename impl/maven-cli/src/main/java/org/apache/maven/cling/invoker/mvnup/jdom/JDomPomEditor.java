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
package org.apache.maven.cling.invoker.mvnup.jdom;

import java.util.Objects;

import org.eclipse.aether.artifact.Artifact;
import org.jdom2.Element;
import org.jdom2.Text;

/**
 * Methods editing JDom POM representation.
 */
public final class JDomPomEditor {
    private static final JDomCfg POM_CONFIG = new JDomPomCfg();

    /**
     * Helper for GA equality. To be used with plugins.
     */
    public static boolean equalsGA(Artifact artifact, Element element) {
        String groupId = element.getChildText("groupId", element.getNamespace());
        String artifactId = element.getChildText("artifactId", element.getNamespace());
        return Objects.equals(artifact.getGroupId(), groupId) && Objects.equals(artifact.getArtifactId(), artifactId);
    }

    /**
     * Helper for GATC equality. To be used with dependencies.
     */
    public static boolean equalsGATC(Artifact artifact, Element element) {
        String groupId = element.getChildText("groupId", element.getNamespace());
        String artifactId = element.getChildText("artifactId", element.getNamespace());
        String type = element.getChildText("type", element.getNamespace());
        if (type == null) {
            type = "jar";
        }
        String classifier = element.getChildText("classifier", element.getNamespace());
        if (classifier == null) {
            classifier = "";
        }
        return Objects.equals(artifact.getGroupId(), groupId)
                && Objects.equals(artifact.getArtifactId(), artifactId)
                && Objects.equals(artifact.getClassifier(), classifier)
                && Objects.equals(artifact.getExtension(), type);
    }

    public static void setProperty(Element project, String key, String value, boolean upsert) {
        if (project != null) {
            Element properties = project.getChild("properties", project.getNamespace());
            if (upsert && properties == null) {
                properties = new Element("properties", project.getNamespace());
                JDomUtils.addElement(POM_CONFIG, properties, project);
            }
            if (properties != null) {
                Element property = properties.getChild(key, properties.getNamespace());
                if (upsert && property == null) {
                    property = new Element(key, properties.getNamespace());
                    JDomUtils.addElement(POM_CONFIG, property, properties);
                }
                if (property != null) {
                    property.setText(value);
                }
            }
        }
    }

    public static void setParent(Element project, Artifact a) {
        if (project != null) {
            Element parent = project.getChild("parent", project.getNamespace());
            if (parent == null) {
                parent = new Element("parent", project.getNamespace());
                parent.addContent(new Text("\n  " + JDomUtils.detectIndentation(project)));
                JDomUtils.addElement(POM_CONFIG, parent, project);
            }
            JDomUtils.rewriteElement(POM_CONFIG, "groupId", a.getGroupId(), parent);
            JDomUtils.rewriteElement(POM_CONFIG, "artifactId", a.getArtifactId(), parent);
            JDomUtils.rewriteElement(POM_CONFIG, "version", a.getVersion(), parent);

            Element groupId = project.getChild("groupId", project.getNamespace());
            if (groupId != null && groupId.getText().equals(a.getGroupId())) {
                JDomUtils.removeChildElement(project, groupId);
            }

            Element version = project.getChild("version", project.getNamespace());
            if (version != null && version.getText().equals(a.getVersion())) {
                JDomUtils.removeChildElement(project, version);
            }
        }
    }

    public static void setVersion(Element project, String version) {
        if (project != null) {
            Element element = project.getChild("version", project.getNamespace());
            if (element != null) {
                element.setText(version);
                return;
            }
            element = project.getChild("parent", project.getNamespace());
            if (element != null) {
                element = element.getChild("version", project.getNamespace());
                if (element != null) {
                    element.setText(version);
                    return;
                }
            }
            throw new IllegalStateException("Could not set version");
        }
    }

    public static void addSubProject(Element project, String subproject) {
        if (project != null) {
            Element modules = project.getChild("modules", project.getNamespace());
            if (modules == null) {
                modules = new Element("modules", project.getNamespace());
                modules.addContent(new Text("\n  " + JDomUtils.detectIndentation(project)));
                JDomUtils.addElement(POM_CONFIG, modules, project);
            }
            Element module = new Element("module", modules.getNamespace());
            module.setText(subproject);
            JDomUtils.addElement(POM_CONFIG, module, modules);
        }
    }

    public static void setPackaging(Element project, String value) {
        if (project != null) {
            Element packaging = project.getChild("packaging", project.getNamespace());
            if (packaging == null) {
                packaging = new Element("packaging", project.getNamespace());
                JDomUtils.addElement(POM_CONFIG, packaging, project);
            }
            if ("jar".equals(value)) {
                JDomUtils.removeChildAndItsCommentFromContent(project, packaging);
            } else {
                packaging.setText(value);
            }
        }
    }

    public static void updateManagedPlugin(Element project, Artifact a, boolean upsert) {
        if (project != null) {
            Element build = project.getChild("build", project.getNamespace());
            if (upsert && build == null) {
                build = new Element("build", project.getNamespace());
                build.addContent(new Text("\n  " + JDomUtils.detectIndentation(project)));
                JDomUtils.addElement(POM_CONFIG, build, project);
            }
            if (build != null) {
                Element pluginManagement = build.getChild("pluginManagement", build.getNamespace());
                if (upsert && pluginManagement == null) {
                    pluginManagement = new Element("pluginManagement", build.getNamespace());
                    pluginManagement.addContent(new Text("\n  " + JDomUtils.detectIndentation(build)));
                    JDomUtils.addElement(POM_CONFIG, pluginManagement, build);
                }
                if (pluginManagement != null) {
                    Element plugins = pluginManagement.getChild("plugins", pluginManagement.getNamespace());
                    if (upsert && plugins == null) {
                        plugins = new Element("plugins", pluginManagement.getNamespace());
                        plugins.addContent(new Text("\n  " + JDomUtils.detectIndentation(pluginManagement)));
                        JDomUtils.addElement(POM_CONFIG, plugins, pluginManagement);
                    }
                    if (plugins != null) {
                        Element toUpdate = null;
                        for (Element plugin : plugins.getChildren("plugin", plugins.getNamespace())) {
                            if (equalsGA(a, plugin)) {
                                toUpdate = plugin;
                                break;
                            }
                        }
                        if (upsert && toUpdate == null) {
                            toUpdate = new Element("plugin", plugins.getNamespace());
                            toUpdate.addContent(new Text("\n  " + JDomUtils.detectIndentation(plugins)));
                            JDomUtils.addElement(POM_CONFIG, toUpdate, plugins);
                            JDomUtils.addElement(
                                    POM_CONFIG,
                                    new Element("groupId", plugins.getNamespace()).setText(a.getGroupId()),
                                    toUpdate);
                            JDomUtils.addElement(
                                    POM_CONFIG,
                                    new Element("artifactId", plugins.getNamespace()).setText(a.getArtifactId()),
                                    toUpdate);
                            JDomUtils.addElement(
                                    POM_CONFIG,
                                    new Element("version", plugins.getNamespace()).setText(a.getVersion()),
                                    toUpdate);
                            return;
                        }
                        if (toUpdate != null) {
                            Element version = toUpdate.getChild("version", toUpdate.getNamespace());
                            if (version != null) {
                                String versionValue = version.getText();
                                if (versionValue.startsWith("${") && versionValue.endsWith("}")) {
                                    String propertyKey = versionValue.substring(2, versionValue.length() - 1);
                                    setProperty(project, propertyKey, a.getVersion(), true);
                                } else {
                                    version.setText(a.getVersion());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void deleteManagedPlugin(Element project, Artifact a) {
        if (project != null) {
            Element build = project.getChild("build", project.getNamespace());
            if (build != null) {
                Element pluginManagement = project.getChild("pluginManagement", project.getNamespace());
                if (pluginManagement != null) {
                    Element plugins = pluginManagement.getChild("plugins", pluginManagement.getNamespace());
                    if (plugins != null) {
                        for (Element plugin : plugins.getChildren("plugin", plugins.getNamespace())) {
                            if (equalsGA(a, plugin)) {
                                JDomUtils.removeChildAndItsCommentFromContent(plugins, plugin);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void updatePlugin(Element project, Artifact a, boolean upsert) {
        if (project != null) {
            Element build = project.getChild("build", project.getNamespace());
            if (upsert && build == null) {
                build = new Element("build", project.getNamespace());
                build.addContent(new Text("\n  " + JDomUtils.detectIndentation(project)));
                JDomUtils.addElement(POM_CONFIG, build, project);
            }
            if (build != null) {
                Element plugins = build.getChild("plugins", build.getNamespace());
                if (upsert && plugins == null) {
                    plugins = new Element("plugins", build.getNamespace());
                    plugins.addContent(new Text("\n  " + JDomUtils.detectIndentation(build)));
                    JDomUtils.addElement(POM_CONFIG, plugins, build);
                }
                if (plugins != null) {
                    Element toUpdate = null;
                    for (Element plugin : plugins.getChildren("plugin", plugins.getNamespace())) {
                        if (equalsGA(a, plugin)) {
                            toUpdate = plugin;
                            break;
                        }
                    }
                    if (upsert && toUpdate == null) {
                        toUpdate = new Element("plugin", plugins.getNamespace());
                        toUpdate.addContent(new Text("\n  " + JDomUtils.detectIndentation(plugins)));
                        JDomUtils.addElement(POM_CONFIG, toUpdate, plugins);
                        JDomUtils.addElement(
                                POM_CONFIG,
                                new Element("groupId", plugins.getNamespace()).setText(a.getGroupId()),
                                toUpdate);
                        JDomUtils.addElement(
                                POM_CONFIG,
                                new Element("artifactId", plugins.getNamespace()).setText(a.getArtifactId()),
                                toUpdate);
                        JDomUtils.addElement(
                                POM_CONFIG,
                                new Element("version", plugins.getNamespace()).setText(a.getVersion()),
                                toUpdate);
                        return;
                    }
                    if (toUpdate != null) {
                        Element version = toUpdate.getChild("version", plugins.getNamespace());
                        if (version != null) {
                            String versionValue = version.getText();
                            if (versionValue.startsWith("${") && versionValue.endsWith("}")) {
                                String propertyKey = versionValue.substring(2, versionValue.length() - 1);
                                setProperty(project, propertyKey, a.getVersion(), true);
                            } else {
                                version.setText(a.getVersion());
                            }
                        } else {
                            updateManagedPlugin(project, a, upsert);
                        }
                    }
                }
            }
        }
    }

    public static void deletePlugin(Element project, Artifact a) {
        if (project != null) {
            Element build = project.getChild("build", project.getNamespace());
            if (build != null) {
                Element plugins = build.getChild("plugins", build.getNamespace());
                if (plugins != null) {
                    for (Element plugin : plugins.getChildren("plugin", plugins.getNamespace())) {
                        if (equalsGA(a, plugin)) {
                            JDomUtils.removeChildAndItsCommentFromContent(plugins, plugin);
                        }
                    }
                }
            }
        }
    }

    public static void deletePluginVersion(Element project, Artifact a) {
        if (project != null) {
            Element build = project.getChild("build", project.getNamespace());
            if (build != null) {
                Element plugins = build.getChild("plugins", build.getNamespace());
                if (plugins != null) {
                    for (Element plugin : plugins.getChildren("plugin", plugins.getNamespace())) {
                        if (equalsGA(a, plugin)) {
                            Element version = plugin.getChild("version", plugin.getNamespace());
                            if (version != null) {
                                JDomUtils.removeChildAndItsCommentFromContent(plugin, version);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void updateExtension(Element project, Artifact a, boolean upsert) {
        if (project != null) {
            Element build = project.getChild("build", project.getNamespace());
            if (upsert && build == null) {
                build = new Element("build", project.getNamespace());
                build.addContent(new Text("\n  " + JDomUtils.detectIndentation(project)));
                JDomUtils.addElement(POM_CONFIG, build, project);
            }
            if (build != null) {
                Element extensions = build.getChild("extensions", build.getNamespace());
                if (upsert && extensions == null) {
                    extensions = new Element("extensions", build.getNamespace());
                    extensions.addContent(new Text("\n  " + JDomUtils.detectIndentation(build)));
                    JDomUtils.addElement(POM_CONFIG, extensions, build);
                }
                if (extensions != null) {
                    Element toUpdate = null;
                    for (Element plugin : extensions.getChildren("extension", extensions.getNamespace())) {
                        if (equalsGA(a, plugin)) {
                            toUpdate = plugin;
                            break;
                        }
                    }
                    if (upsert && toUpdate == null) {
                        toUpdate = new Element("plugin", extensions.getNamespace());
                        toUpdate.addContent(new Text("\n  " + JDomUtils.detectIndentation(extensions)));
                        JDomUtils.addElement(POM_CONFIG, toUpdate, extensions);
                        JDomUtils.addElement(
                                POM_CONFIG,
                                new Element("groupId", extensions.getNamespace()).setText(a.getGroupId()),
                                toUpdate);
                        JDomUtils.addElement(
                                POM_CONFIG,
                                new Element("artifactId", extensions.getNamespace()).setText(a.getArtifactId()),
                                toUpdate);
                        JDomUtils.addElement(
                                POM_CONFIG,
                                new Element("version", extensions.getNamespace()).setText(a.getVersion()),
                                toUpdate);
                        return;
                    }
                    if (toUpdate != null) {
                        Element version = toUpdate.getChild("version", extensions.getNamespace());
                        if (version != null) {
                            String versionValue = version.getText();
                            if (versionValue.startsWith("${") && versionValue.endsWith("}")) {
                                String propertyKey = versionValue.substring(2, versionValue.length() - 1);
                                setProperty(project, propertyKey, a.getVersion(), true);
                            } else {
                                version.setText(a.getVersion());
                            }
                        } else {
                            updateManagedPlugin(project, a, upsert);
                        }
                    }
                }
            }
        }
    }

    public static void deleteExtension(Element project, Artifact a) {
        if (project != null) {
            Element build = project.getChild("build", project.getNamespace());
            if (build != null) {
                Element extensions = build.getChild("extensions", build.getNamespace());
                if (extensions != null) {
                    for (Element extension : extensions.getChildren("extension", extensions.getNamespace())) {
                        if (equalsGA(a, extension)) {
                            JDomUtils.removeChildAndItsCommentFromContent(extensions, extension);
                        }
                    }
                }
            }
        }
    }

    public static void updateManagedDependency(Element project, Artifact a, boolean upsert) {
        if (project != null) {
            Element dependencyManagement = project.getChild("dependencyManagement", project.getNamespace());
            if (upsert && dependencyManagement == null) {
                dependencyManagement = new Element("dependencyManagement", project.getNamespace());
                dependencyManagement.addContent(new Text("\n  " + JDomUtils.detectIndentation(project)));
                JDomUtils.addElement(POM_CONFIG, dependencyManagement, project);
            }
            if (dependencyManagement != null) {
                Element dependencies =
                        dependencyManagement.getChild("dependencies", dependencyManagement.getNamespace());
                if (upsert && dependencies == null) {
                    dependencies = new Element("dependencies", dependencyManagement.getNamespace());
                    dependencies.addContent(new Text("\n  " + JDomUtils.detectIndentation(dependencyManagement)));
                    JDomUtils.addElement(POM_CONFIG, dependencies, dependencyManagement);
                }
                if (dependencies != null) {
                    Element toUpdate = null;
                    for (Element dependency : dependencies.getChildren("dependency", dependencies.getNamespace())) {
                        if (equalsGATC(a, dependency)) {
                            toUpdate = dependency;
                            break;
                        }
                    }
                    if (upsert && toUpdate == null) {
                        toUpdate = new Element("dependency", dependencies.getNamespace());
                        toUpdate.addContent(new Text("\n  " + JDomUtils.detectIndentation(dependencies)));
                        JDomUtils.addElement(POM_CONFIG, toUpdate, dependencies);
                        JDomUtils.addElement(
                                POM_CONFIG,
                                new Element("groupId", dependencies.getNamespace()).setText(a.getGroupId()),
                                toUpdate);
                        JDomUtils.addElement(
                                POM_CONFIG,
                                new Element("artifactId", dependencies.getNamespace()).setText(a.getArtifactId()),
                                toUpdate);
                        JDomUtils.addElement(
                                POM_CONFIG,
                                new Element("version", dependencies.getNamespace()).setText(a.getVersion()),
                                toUpdate);
                        if (!"jar".equals(a.getExtension())) {
                            JDomUtils.addElement(
                                    POM_CONFIG,
                                    new Element("type", dependencies.getNamespace()).setText(a.getExtension()),
                                    toUpdate);
                        }
                        if (!a.getClassifier().isEmpty()) {
                            JDomUtils.addElement(
                                    POM_CONFIG,
                                    new Element("classifier", dependencies.getNamespace()).setText(a.getClassifier()),
                                    toUpdate);
                        }
                        return;
                    }
                    if (toUpdate != null) {
                        Element version = toUpdate.getChild("version", dependencies.getNamespace());
                        if (version != null) {
                            String versionValue = version.getText();
                            if (versionValue.startsWith("${") && versionValue.endsWith("}")) {
                                String propertyKey = versionValue.substring(2, versionValue.length() - 1);
                                setProperty(project, propertyKey, a.getVersion(), true);
                            } else {
                                version.setText(a.getVersion());
                            }
                        } else {
                            updateManagedDependency(project, a, upsert);
                        }
                    }
                }
            }
        }
    }

    public static void deleteManagedDependency(Element project, Artifact a) {
        if (project != null) {
            Element dependencyManagement = project.getChild("dependencyManagement", project.getNamespace());
            if (dependencyManagement != null) {
                Element dependencies =
                        dependencyManagement.getChild("dependencies", dependencyManagement.getNamespace());
                if (dependencies != null) {
                    for (Element dependency : dependencies.getChildren("dependency", dependencies.getNamespace())) {
                        if (equalsGATC(a, dependency)) {
                            JDomUtils.removeChildAndItsCommentFromContent(dependencies, dependency);
                        }
                    }
                }
            }
        }
    }

    public static void updateDependency(Element project, Artifact a, boolean upsert) {
        if (project != null) {
            Element dependencies = project.getChild("dependencies", project.getNamespace());
            if (upsert && dependencies == null) {
                dependencies = new Element("dependencies", project.getNamespace());
                dependencies.addContent(new Text("\n  " + JDomUtils.detectIndentation(project)));
                JDomUtils.addElement(POM_CONFIG, dependencies, project);
            }
            if (dependencies != null) {
                Element toUpdate = null;
                for (Element dependency : dependencies.getChildren("dependency", dependencies.getNamespace())) {
                    if (equalsGATC(a, dependency)) {
                        toUpdate = dependency;
                        break;
                    }
                }
                if (upsert && toUpdate == null) {
                    toUpdate = new Element("dependency", dependencies.getNamespace());
                    toUpdate.addContent(new Text("\n  " + JDomUtils.detectIndentation(dependencies)));
                    JDomUtils.addElement(POM_CONFIG, toUpdate, dependencies);
                    JDomUtils.addElement(
                            POM_CONFIG,
                            new Element("groupId", dependencies.getNamespace()).setText(a.getGroupId()),
                            toUpdate);
                    JDomUtils.addElement(
                            POM_CONFIG,
                            new Element("artifactId", dependencies.getNamespace()).setText(a.getArtifactId()),
                            toUpdate);
                    JDomUtils.addElement(
                            POM_CONFIG,
                            new Element("version", dependencies.getNamespace()).setText(a.getVersion()),
                            toUpdate);
                    if (!"jar".equals(a.getExtension())) {
                        JDomUtils.addElement(
                                POM_CONFIG,
                                new Element("type", dependencies.getNamespace()).setText(a.getExtension()),
                                toUpdate);
                    }
                    if (!a.getClassifier().isEmpty()) {
                        JDomUtils.addElement(
                                POM_CONFIG,
                                new Element("classifier", dependencies.getNamespace()).setText(a.getClassifier()),
                                toUpdate);
                    }
                    return;
                }
                if (toUpdate != null) {
                    Element version = toUpdate.getChild("version", dependencies.getNamespace());
                    if (version != null) {
                        String versionValue = version.getText();
                        if (versionValue.startsWith("${") && versionValue.endsWith("}")) {
                            String propertyKey = versionValue.substring(2, versionValue.length() - 1);
                            setProperty(project, propertyKey, a.getVersion(), true);
                        } else {
                            version.setText(a.getVersion());
                        }
                    } else {
                        updateManagedDependency(project, a, upsert);
                    }
                }
            }
        }
    }

    public static void deleteDependency(Element project, Artifact a) {
        if (project != null) {
            Element dependencies = project.getChild("dependencies", project.getNamespace());
            if (dependencies != null) {
                for (Element dependency : dependencies.getChildren("dependency", dependencies.getNamespace())) {
                    if (equalsGATC(a, dependency)) {
                        JDomUtils.removeChildAndItsCommentFromContent(dependencies, dependency);
                    }
                }
            }
        }
    }
}
