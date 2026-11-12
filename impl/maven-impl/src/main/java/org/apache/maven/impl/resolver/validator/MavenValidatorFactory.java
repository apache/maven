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
package org.apache.maven.impl.resolver.validator;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.maven.api.Constants;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.spi.validator.Validator;
import org.eclipse.aether.spi.validator.ValidatorFactory;
import org.eclipse.aether.util.ConfigUtils;

@Named
@Singleton
public class MavenValidatorFactory implements ValidatorFactory {
    public enum ValidationLevel {
        DEFAULT,
        MILD,
        OFF;
    }

    private final MavenValidator defaultValidator = new MavenValidator(true);
    private final MavenValidator mildValidator = new MavenValidator(false);

    @Override
    public Validator newInstance(RepositorySystemSession session) {
        return switch (getValidationLevel(session)) {
            case DEFAULT -> defaultValidator;
            case MILD -> mildValidator;
            case OFF -> NOOP;
        };
    }

    private static ValidationLevel getValidationLevel(RepositorySystemSession session) {
        String value =
                ConfigUtils.getString(session, ValidationLevel.DEFAULT.name(), Constants.MAVEN_RESOLVER_VALIDATION);
        try {
            return ValidationLevel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid value '" + value + "' for " + Constants.MAVEN_RESOLVER_VALIDATION
                            + ", supported values are: "
                            + Arrays.stream(ValidationLevel.values())
                                    .map(level -> level.name().toLowerCase(Locale.ROOT))
                                    .collect(Collectors.joining(", ")),
                    e);
        }
    }
}
