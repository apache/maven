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
package org.apache.maven.internal.impl.types;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.maven.api.JavaPathType;
import org.apache.maven.api.Type;
import org.apache.maven.internal.impl.DefaultType;

/**
 * Type provider for a Maven plugin.
 *
 * @see Type#MAVEN_PLUGIN
 */
@Named(MavenPluginTypeProvider.NAME)
@Singleton
public class MavenPluginTypeProvider implements Provider<Type> {
    public static final String NAME = Type.MAVEN_PLUGIN;

    private final Type type;

    public MavenPluginTypeProvider() {
        this.type = new DefaultType(NAME, Type.LANGUAGE_JAVA, "jar", null, JavaPathType.CLASSES);
    }

    @Override
    public Type get() {
        return type;
    }
}
