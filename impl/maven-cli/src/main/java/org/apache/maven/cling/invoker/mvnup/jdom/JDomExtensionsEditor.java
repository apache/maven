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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.jdom2.Element;
import org.jdom2.Text;

/**
 * Methods editing JDom Extensions representation.
 */
public final class JDomExtensionsEditor {
    private static final JDomCfg JDOM_CONFIG = new JDomExtensionsCfg();

    /**
     * Helper for GA equality. To be used with extensions.
     */
    public static boolean equalsGA(Artifact artifact, Element element) {
        String groupId = element.getChildText("groupId", element.getNamespace());
        String artifactId = element.getChildText("artifactId", element.getNamespace());
        return Objects.equals(artifact.getGroupId(), groupId) && Objects.equals(artifact.getArtifactId(), artifactId);
    }

    public static List<Artifact> listExtensions(Element extensions) {
        ArrayList<Artifact> result = new ArrayList<>();
        if (extensions != null) {
            for (Element extension : extensions.getChildren("extension", extensions.getNamespace())) {
                result.add(new DefaultArtifact(
                        extension.getChildText("groupId", extension.getNamespace()),
                        extension.getChildText("artifactId", extension.getNamespace()),
                        "jar",
                        extension.getChildText("version", extension.getNamespace())));
            }
        }
        return result;
    }

    public static void updateExtension(Element extensions, Artifact a, boolean upsert) {
        if (extensions != null) {
            Element toUpdate = null;
            for (Element extension : extensions.getChildren("extension", extensions.getNamespace())) {
                if (equalsGA(a, extension)) {
                    toUpdate = extension;
                    break;
                }
            }
            if (upsert && toUpdate == null) {
                toUpdate = new Element("extension", extensions.getNamespace());
                toUpdate.addContent(new Text("\n  " + JDomUtils.detectIndentation(extensions)));
                JDomUtils.addElement(JDOM_CONFIG, toUpdate, extensions);
                JDomUtils.addElement(
                        JDOM_CONFIG,
                        new Element("groupId", extensions.getNamespace()).setText(a.getGroupId()),
                        toUpdate);
                JDomUtils.addElement(
                        JDOM_CONFIG,
                        new Element("artifactId", extensions.getNamespace()).setText(a.getArtifactId()),
                        toUpdate);
                JDomUtils.addElement(
                        JDOM_CONFIG,
                        new Element("version", extensions.getNamespace()).setText(a.getVersion()),
                        toUpdate);
                return;
            }
            if (toUpdate != null) {
                toUpdate.getChild("version", toUpdate.getNamespace()).setText(a.getVersion());
            }
        }
    }

    public static void deleteExtension(Element extensions, Artifact a) {
        if (extensions != null) {
            for (Element extension : extensions.getChildren("extension", extensions.getNamespace())) {
                if (equalsGA(a, extension)) {
                    JDomUtils.removeChildAndItsCommentFromContent(extensions, extension);
                }
            }
        }
    }
}
