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
package org.apache.maven.impl.model.profile;

import org.apache.maven.api.Constants;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.ActivationProperty;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.VersionParserException;
import org.apache.maven.api.services.model.ModelVersionParser;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.apache.maven.api.services.model.ProfileActivator;

/**
 * Determines profile activation based on the existence or value of some execution property.
 * <p>
 * The property lookup order is: user properties ({@code -D} flags) → system properties
 * ({@code java.version}, {@code os.name}, …) → model properties (the POM's own
 * {@code <properties>} section).  In external model builds (dependency POMs, parent POMs,
 * imported BOMs), user properties are suppressed by the caller via a sandboxed
 * {@link ProfileActivationContext}, so only system and model properties drive activation.
 * This makes profile activation in published artifacts deterministic: it depends on the
 * consumer's platform and the artifact's declared properties, not on the consumer's
 * {@code -D} flags.
 *
 * @see ActivationProperty
 */
@Named("property")
@Singleton
public class PropertyProfileActivator implements ProfileActivator {

    private final ModelVersionParser versionParser;

    @Inject
    public PropertyProfileActivator(ModelVersionParser versionParser) {
        this.versionParser = versionParser;
    }

    @Override
    public boolean isActive(Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
        Activation activation = profile.getActivation();

        if (activation == null) {
            return false;
        }

        ActivationProperty property = activation.getProperty();

        if (property == null) {
            return false;
        }

        String name = property.getName();
        boolean reverseName = false;

        if (name != null && name.startsWith("!")) {
            reverseName = true;
            name = name.substring(1);
        }

        if (name == null || name.isEmpty()) {
            problems.add(
                    BuilderProblem.Severity.ERROR,
                    ModelProblem.Version.BASE,
                    "The property name is required to activate the profile " + profile.getId(),
                    property.getLocation(""));
            return false;
        }

        // Lookup order: user (-D) → system (java.version, os.name, …) → model <properties>.
        // In external model builds the caller suppresses user properties via a sandboxed context,
        // so consumer -D flags cannot activate dependency profiles. Model properties are always
        // consulted because they are part of the artifact's published identity.
        String sysValue = context.getUserProperty(name);
        if (sysValue == null && "packaging".equals(name)) {
            sysValue = context.getModelPackaging();
        }
        if (sysValue == null) {
            sysValue = context.getSystemProperty(name);
        }
        if (sysValue == null) {
            sysValue = context.getModelProperty(name);
        }

        String propValue = property.getValue();
        if (propValue != null && !propValue.isEmpty()) {
            boolean reverseValue = false;
            if (propValue.startsWith("!")) {
                reverseValue = true;
                propValue = propValue.substring(1);
            }

            boolean result;
            if (Constants.MAVEN_VERSION.equals(name) && isVersionRange(propValue)) {
                if (sysValue == null || sysValue.isEmpty()) {
                    result = false;
                } else {
                    try {
                        result = versionParser
                                .parseVersionRange(propValue)
                                .contains(versionParser.parseVersion(sysValue));
                    } catch (VersionParserException e) {
                        problems.add(
                                BuilderProblem.Severity.WARNING,
                                ModelProblem.Version.BASE,
                                "Failed to determine Maven version activation for profile " + profile.getId()
                                        + " due to invalid version range: '" + propValue + "'",
                                property.getLocation("value"),
                                e);
                        return false;
                    }
                }
            } else {
                // we have a value, so it has to match the system value...
                result = propValue.equals(sysValue);
            }
            return reverseValue != result;
        } else {
            return reverseName != (sysValue != null && !sysValue.isEmpty());
        }
    }

    private static boolean isVersionRange(String value) {
        return value.startsWith("[") || value.startsWith("(");
    }

    @Override
    public boolean presentInConfig(Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
        Activation activation = profile.getActivation();

        if (activation == null) {
            return false;
        }

        ActivationProperty property = activation.getProperty();

        return property != null;
    }
}
