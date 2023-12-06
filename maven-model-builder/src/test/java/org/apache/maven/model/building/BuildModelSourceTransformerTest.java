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
package org.apache.maven.model.building;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BuildModelSourceTransformerTest {

    Path pomFile;
    TransformerContext context = org.mockito.Mockito.mock(TransformerContext.class);

    @Test
    void testModelVersion() {
        Model initial = new Model(org.apache.maven.api.model.Model.newBuilder()
                .namespaceUri("http://maven.apache.org/POM/4.0.0")
                .build());
        Model expected = new Model(org.apache.maven.api.model.Model.newBuilder()
                .namespaceUri("http://maven.apache.org/POM/4.0.0")
                .modelVersion("4.0.0")
                .build());
        Model actual = transform(initial);
        assertTrue(equalsDeep(expected, actual));
    }

    @Test
    void testParent() {
        Path root = Paths.get(".").toAbsolutePath().normalize();
        pomFile = root.resolve("child/pom.xml");
        Model parent = new Model(org.apache.maven.api.model.Model.newBuilder()
                .groupId("GROUPID")
                .artifactId("ARTIFACTID")
                .version("1.0-SNAPSHOT")
                .build());
        Mockito.when(context.getRawModel(pomFile, root.resolve("pom.xml"))).thenReturn(parent);
        Mockito.when(context.locate(root)).thenReturn(root.resolve("pom.xml"));
        Model initial = new Model(org.apache.maven.api.model.Model.newBuilder()
                .parent(org.apache.maven.api.model.Parent.newBuilder()
                        .groupId("GROUPID")
                        .artifactId("ARTIFACTID")
                        .build())
                .build());
        Model expected = new Model(org.apache.maven.api.model.Model.newBuilder()
                .parent(org.apache.maven.api.model.Parent.newBuilder()
                        .groupId("GROUPID")
                        .artifactId("ARTIFACTID")
                        .version("1.0-SNAPSHOT")
                        .build())
                .build());
        Model actual = transform(initial);
        assertTrue(equalsDeep(expected, actual));
    }

    @Test
    void testReactorDependencies() {
        Model dep = new Model(org.apache.maven.api.model.Model.newBuilder()
                .groupId("GROUPID")
                .artifactId("ARTIFACTID")
                .version("1.0-SNAPSHOT")
                .build());
        Mockito.when(context.getRawModel(pomFile, "GROUPID", "ARTIFACTID")).thenReturn(dep);
        Model initial = new Model(org.apache.maven.api.model.Model.newBuilder()
                .dependencies(Collections.singleton(org.apache.maven.api.model.Dependency.newBuilder()
                        .groupId("GROUPID")
                        .artifactId("ARTIFACTID")
                        .build()))
                .build());
        Model expected = new Model(org.apache.maven.api.model.Model.newBuilder()
                .dependencies(Collections.singleton(org.apache.maven.api.model.Dependency.newBuilder()
                        .groupId("GROUPID")
                        .artifactId("ARTIFACTID")
                        .version("1.0-SNAPSHOT")
                        .build()))
                .build());
        Model actual = transform(initial);
        assertTrue(equalsDeep(expected, actual));
    }

    @Test
    void testCiFriendlyVersion() {
        Model initial = new Model(org.apache.maven.api.model.Model.newBuilder()
                .version("${revision}-${sha1}")
                .build());
        Mockito.when(context.getUserProperty("revision")).thenReturn("therev");
        Mockito.when(context.getUserProperty("sha1")).thenReturn("thesha");
        Model expected = new Model(org.apache.maven.api.model.Model.newBuilder()
                .version("therev-thesha")
                .build());
        Model actual = transform(initial);
        assertTrue(equalsDeep(expected, actual));
    }

    Model transform(Model model) {
        Model transformed = model.clone();
        new BuildModelSourceTransformer().transform(pomFile, context, transformed);
        return transformed;
    }

    public static boolean equalsDeep(Object m1, Object m2) {
        try {
            if (m1 == m2) {
                return true;
            }
            if (m1 == null || m2 == null) {
                return false;
            }
            if (!Objects.equals(m1.getClass(), m2.getClass())) {
                return false;
            }
            BeanInfo bean = Introspector.getBeanInfo(m1.getClass());
            for (PropertyDescriptor prop : bean.getPropertyDescriptors()) {
                if (("first".equals(prop.getName()) || "last".equals(prop.getName()))
                        && List.class.equals(prop.getReadMethod().getDeclaringClass())) {
                    continue;
                }
                Object p1 = prop.getReadMethod().invoke(m1);
                Object p2 = prop.getReadMethod().invoke(m2);
                if (!equalsDeep(p1, p2)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
