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

import java.util.Locale;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.spi.validator.Validator;
import org.eclipse.aether.spi.validator.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link MavenValidatorFactory}.
 */
class MavenValidatorFactoryTest {

    private static final DefaultArtifact SLASH_CLASSIFIER = new DefaultArtifact("g:a:jar:lib/foo.jar:1.0");

    private final MavenValidatorFactory factory = new MavenValidatorFactory();

    private Validator newValidator(String level) {
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(h -> false);
        if (level != null) {
            session.setConfigProperty(MavenValidatorFactory.MAVEN_RESOLVER_VALIDATION, level);
        }
        return factory.newInstance(session);
    }

    @Test
    void defaultWhenUnset() {
        Validator validator = newValidator(null);
        assertInstanceOf(MavenValidator.class, validator);
        assertThrows(IllegalArgumentException.class, () -> validator.validateArtifact(SLASH_CLASSIFIER));
    }

    @ParameterizedTest
    @ValueSource(strings = {"default", "DEFAULT", "Default"})
    void defaultLevelIgnoresCase(String level) {
        Validator validator = newValidator(level);
        assertInstanceOf(MavenValidator.class, validator, level);
        assertThrows(IllegalArgumentException.class, () -> validator.validateArtifact(SLASH_CLASSIFIER), level);
    }

    @ParameterizedTest
    @ValueSource(strings = {"mild", "MILD", "Mild", " mild "})
    void mildLevelIgnoresCase(String level) {
        Validator validator = newValidator(level);
        assertInstanceOf(MavenValidator.class, validator);
        assertDoesNotThrow(() -> validator.validateArtifact(SLASH_CLASSIFIER));
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateArtifact(new DefaultArtifact("g:a:jar:${classifier}:1.0")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"off", "OFF", "Off"})
    void offLevelIgnoresCase(String level) {
        assertSame(ValidatorFactory.NOOP, newValidator(level), level);
    }

    @Test
    void invalidLevel() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> newValidator("strict"));
        assertTrue(e.getMessage().contains(MavenValidatorFactory.MAVEN_RESOLVER_VALIDATION), e.getMessage());
        assertTrue(e.getMessage().contains("strict"), e.getMessage());
        for (MavenValidatorFactory.ValidationLevel level : MavenValidatorFactory.ValidationLevel.values()) {
            assertTrue(e.getMessage().contains(level.name().toLowerCase(Locale.ROOT)), e.getMessage());
        }
    }
}
