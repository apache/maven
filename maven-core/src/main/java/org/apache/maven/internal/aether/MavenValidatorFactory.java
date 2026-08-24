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
package org.apache.maven.internal.aether;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.spi.validator.Validator;
import org.eclipse.aether.spi.validator.ValidatorFactory;
import org.eclipse.aether.util.ConfigUtils;

@Named
@Singleton
public class MavenValidatorFactory implements ValidatorFactory {
    /**
     * Resolver validation control.
     * Can be <code>default</code> (full validation), <code>mild</code> (only uninterpolated placeholders) or
     * <code>off</code> (no validation, as in Maven 3.9.x).
     * This configuration provides "escape hatch" for those projects, that are forced to use non-conformant solutions.
     * Default value is {@code default}.
     *
     * @since 3.10.0
     */
    public static final String MAVEN_RESOLVER_VALIDATION = "maven.resolver.validation";

    public enum ValidationLevel {
        DEFAULT,
        MILD,
        OFF;
    }

    private final MavenValidator defaultValidator = new MavenValidator(true);
    private final MavenValidator mildValidator = new MavenValidator(false);
    private final Validator offValidator = NOOP;

    @Override
    public Validator newInstance(RepositorySystemSession session) {
        switch (ConfigUtils.getEnum(
                session, ValidationLevel.class, ValidationLevel.DEFAULT, MAVEN_RESOLVER_VALIDATION)) {
            case DEFAULT:
                return defaultValidator;
            case MILD:
                return mildValidator;
            case OFF:
                return offValidator;
            default:
                throw new IllegalArgumentException("Unknown validation level");
        }
    }
}
