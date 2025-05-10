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
package org.apache.maven.its.mng8572.extension;

import java.util.Collection;
import java.util.Collections;

import org.apache.maven.api.JavaPathType;
import org.apache.maven.api.Language;
import org.apache.maven.api.Type;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.spi.TypeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a custom artifact type handler using the Maven API DI system.
 */
@Named
public class CustomTypeProvider implements TypeProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomTypeProvider.class);

    @Override
    public Collection<Type> provides() {
        System.out.println("[MNG-8572] Registering custom type handler for type: custom-type");
        LOGGER.info("[MNG-8572] Registering custom type handler for type: custom-type");

        // Create a custom type that will be used for dependencies with type="custom-type"
        CustomType customType = new CustomType(
                "custom-type", // type id
                Language.JAVA_FAMILY, // language
                "custom", // extension
                null, // classifier
                false, // includesDependencies
                JavaPathType.CLASSES // pathTypes - add to classpath
                );

        return Collections.singleton(customType);
    }
}
