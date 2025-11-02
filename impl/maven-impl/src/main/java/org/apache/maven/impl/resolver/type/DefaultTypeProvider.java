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
package org.apache.maven.impl.resolver.type;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.maven.api.JavaPathType;
import org.apache.maven.api.Language;
import org.apache.maven.api.Type;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.spi.TypeProvider;

@Named
public class DefaultTypeProvider implements TypeProvider {
    private final Map<String, DefaultType> providedTypes;

    public DefaultTypeProvider() {
        HashMap<String, DefaultType> types = new HashMap<>();
        // Maven types
        types.put(Type.POM, new DefaultType(Type.POM, Language.NONE, "pom", null, false));
        types.put(Type.BOM, new DefaultType(Type.BOM, Language.NONE, "pom", null, false));
        types.put(
                Type.MAVEN_PLUGIN,
                new DefaultType(Type.MAVEN_PLUGIN, Language.JAVA_FAMILY, "jar", null, false, JavaPathType.CLASSES));
        // Java types
        types.put(
                Type.JAR,
                new DefaultType(
                        Type.JAR,
                        Language.JAVA_FAMILY,
                        "jar",
                        null,
                        false,
                        JavaPathType.CLASSES,
                        JavaPathType.MODULES));
        types.put(
                Type.JAVADOC,
                new DefaultType(Type.JAVADOC, Language.JAVA_FAMILY, "jar", "javadoc", false, JavaPathType.CLASSES));
        types.put(Type.JAVA_SOURCE, new DefaultType(Type.JAVA_SOURCE, Language.JAVA_FAMILY, "jar", "sources", false));
        types.put(
                Type.TEST_JAR,
                new DefaultType(
                        Type.TEST_JAR,
                        Language.JAVA_FAMILY,
                        "jar",
                        "tests",
                        false,
                        JavaPathType.CLASSES,
                        JavaPathType.PATCH_MODULE));
        types.put(
                Type.TEST_JAVA_SOURCE,
                new DefaultType(Type.TEST_JAVA_SOURCE, Language.JAVA_FAMILY, "jar", "test-sources", false));
        types.put(
                Type.MODULAR_JAR,
                new DefaultType(Type.MODULAR_JAR, Language.JAVA_FAMILY, "jar", null, false, JavaPathType.MODULES));
        types.put(
                Type.CLASSPATH_JAR,
                new DefaultType(Type.CLASSPATH_JAR, Language.JAVA_FAMILY, "jar", null, false, JavaPathType.CLASSES));
        types.put(
                Type.PROCESSOR,
                new DefaultType(
                        Type.PROCESSOR,
                        Language.JAVA_FAMILY,
                        "jar",
                        null,
                        false,
                        true,
                        deriveMapping(),
                        JavaPathType.PROCESSOR_CLASSES,
                        JavaPathType.PROCESSOR_MODULES));
        types.put(
                Type.CLASSPATH_PROCESSOR,
                new DefaultType(
                        Type.CLASSPATH_PROCESSOR,
                        Language.JAVA_FAMILY,
                        "jar",
                        null,
                        false,
                        true,
                        deriveMapping(),
                        JavaPathType.PROCESSOR_CLASSES));
        types.put(
                Type.MODULAR_PROCESSOR,
                new DefaultType(
                        Type.MODULAR_PROCESSOR,
                        Language.JAVA_FAMILY,
                        "jar",
                        null,
                        false,
                        true,
                        deriveMapping(),
                        JavaPathType.PROCESSOR_MODULES));
        // j2ee types
        types.put("ejb", new DefaultType("ejb", Language.JAVA_FAMILY, "jar", null, false, JavaPathType.CLASSES));
        types.put(
                "ejb-client",
                new DefaultType("ejb-client", Language.JAVA_FAMILY, "jar", "client", false, JavaPathType.CLASSES));
        types.put("war", new DefaultType("war", Language.JAVA_FAMILY, "war", null, true));
        types.put("ear", new DefaultType("ear", Language.JAVA_FAMILY, "ear", null, true));
        types.put("rar", new DefaultType("rar", Language.JAVA_FAMILY, "rar", null, true));
        types.put("par", new DefaultType("par", Language.JAVA_FAMILY, "par", null, true));

        this.providedTypes = Map.copyOf(types);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Collection<Type> provides() {
        return (Collection) types();
    }

    public Collection<DefaultType> types() {
        return providedTypes.values();
    }

    /**
     * Performs mapping: accepts "starting" type, "current" node  type, and should return mapped type.
     */
    @SuppressWarnings("MissingSwitchDefault")
    private BiFunction<Type, Type, Type> deriveMapping() {
        // TODO: impl this
        return (t1, t2) -> {
            if (t1.needsDerive()) {
                if (t1.id().equals(Type.PROCESSOR)) {
                    switch (t2.id()) {
                        case Type.JAR:
                            t2 = providedTypes.get(Type.PROCESSOR);
                            break;
                        case Type.MODULAR_JAR:
                            t2 = providedTypes.get(Type.MODULAR_PROCESSOR);
                            break;
                        case Type.CLASSPATH_JAR:
                            t2 = providedTypes.get(Type.CLASSPATH_PROCESSOR);
                            break;
                    }
                }
            }
            return t2;
        };
    }
}
