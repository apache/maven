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
                new DefaultType("bom", Language.NONE, "pom", null, false, false),
                new DefaultType("pom", Language.NONE, "pom", null, false, false),
                new DefaultType("maven-plugin", Language.JAVA_FAMILY, "jar", null, true, false),
                new DefaultType("jar", Language.JAVA_FAMILY, "jar", null, true, false),
                new DefaultType("ejb", Language.JAVA_FAMILY, "jar", null, true, false),
                new DefaultType("ejb-client", Language.JAVA_FAMILY, "jar", "client", true, false),
                new DefaultType("test-jar", Language.JAVA_FAMILY, "jar", "tests", true, false),
                new DefaultType("javadoc", Language.JAVA_FAMILY, "jar", "javadoc", true, false),
                new DefaultType("java-source", Language.JAVA_FAMILY, "jar", "sources", false, false),
                new DefaultType("war", Language.JAVA_FAMILY, "war", null, false, true),
                new DefaultType("ear", Language.JAVA_FAMILY, "ear", null, false, true),
                new DefaultType("rar", Language.JAVA_FAMILY, "rar", null, false, true),
                new DefaultType("par", Language.JAVA_FAMILY, "par", null, false, true));
    }
}
