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
package org.apache.maven.internal.impl;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Objects;
import java.util.Optional;

import org.apache.maven.api.Language;
import org.apache.maven.api.services.LanguageManager;

/**
 * TODO: this is session scoped as SPI can contribute.
 */
@Named
@Singleton
public class DefaultLanguageManager implements LanguageManager {
    @Override
    public Optional<Language> lookupLanguageFamily(String id) {
        if (Objects.equals(Language.NONE.id(), id)) {
            return Optional.of(Language.NONE);
        }
        // TODO: this is now just a shortcut; elaborate this, probably with some SPI LanguageSupport
        if (Objects.equals(Language.JAVA_FAMILY.id(), id)) {
            return Optional.of(Language.JAVA_FAMILY);
        }
        return Optional.empty();
    }
}
