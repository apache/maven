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
package org.apache.maven.repository.internal.type;

import javax.inject.Named;

import java.util.Arrays;
import java.util.Collection;

import org.apache.maven.api.JavaPathType;
import org.apache.maven.api.Language;
import org.apache.maven.api.Type;
import org.apache.maven.api.spi.TypeProvider;

@Named
public class DefaultTypeProvider implements TypeProvider {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Collection<Type> provides() {
        return (Collection) types();
    }

    public Collection<DefaultType> types() {
        return Arrays.asList(
                // Maven types
                new DefaultType(Type.POM, Language.NONE, "pom", null, false),
                new DefaultType(Type.BOM, Language.NONE, "pom", null, false),
                new DefaultType(Type.MAVEN_PLUGIN, Language.JAVA_FAMILY, "jar", null, false, JavaPathType.CLASSES),
                // Java types
                new DefaultType(
                        Type.JAR, Language.JAVA_FAMILY, "jar", null, false, JavaPathType.CLASSES, JavaPathType.MODULES),
                new DefaultType(Type.JAVADOC, Language.JAVA_FAMILY, "jar", "javadoc", false, JavaPathType.CLASSES),
                new DefaultType(Type.JAVA_SOURCE, Language.JAVA_FAMILY, "jar", "sources", false),
                new DefaultType(
                        Type.TEST_JAR,
                        Language.JAVA_FAMILY,
                        "jar",
                        "tests",
                        false,
                        JavaPathType.CLASSES,
                        JavaPathType.PATCH_MODULE),
                new DefaultType(Type.MODULAR_JAR, Language.JAVA_FAMILY, "jar", null, false, JavaPathType.MODULES),
                new DefaultType(Type.CLASSPATH_JAR, Language.JAVA_FAMILY, "jar", null, false, JavaPathType.CLASSES),
                new DefaultType(
                        Type.FATJAR,
                        Language.JAVA_FAMILY,
                        "jar",
                        null,
                        true,
                        JavaPathType.CLASSES),
                // j2ee types
                new DefaultType("ejb", Language.JAVA_FAMILY, "jar", null, false, JavaPathType.CLASSES),
                new DefaultType("ejb-client", Language.JAVA_FAMILY, "jar", "client", false, JavaPathType.CLASSES),
                new DefaultType("war", Language.JAVA_FAMILY, "war", null, true),
                new DefaultType("ear", Language.JAVA_FAMILY, "ear", null, true),
                new DefaultType("rar", Language.JAVA_FAMILY, "rar", null, true),
                new DefaultType("par", Language.JAVA_FAMILY, "par", null, true));
    }
}
