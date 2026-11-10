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
package org.apache.maven.its.gh13306;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.maven.api.JavaPathType;
import org.apache.maven.api.Language;
import org.apache.maven.api.PathType;
import org.apache.maven.api.Type;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.spi.TypeProvider;

@Named
public class CustomTypeProvider implements TypeProvider {

    @Override
    public Collection<Type> provides() {
        return List.of(new CustomType());
    }

    static class CustomType implements Type {

        @Override
        public String id() {
            return "app-client";
        }

        @Override
        public Language getLanguage() {
            return Language.JAVA_FAMILY;
        }

        @Override
        public String getExtension() {
            return "jar";
        }

        @Override
        public String getClassifier() {
            return null;
        }

        @Override
        public boolean isIncludesDependencies() {
            return false;
        }

        @Override
        public Set<PathType> getPathTypes() {
            return Set.of(JavaPathType.CLASSES);
        }
    }
}
