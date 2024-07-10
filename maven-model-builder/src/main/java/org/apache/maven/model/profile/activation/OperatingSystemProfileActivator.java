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
package org.apache.maven.model.profile.activation;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Locale;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationOS;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.utils.Os;

/**
 * Determines profile activation based on the operating system of the current runtime platform.
 *
 * @author Benjamin Bentmann
 * @see ActivationOS
 */
@Named("os")
@Singleton
public class OperatingSystemProfileActivator implements ProfileActivator {

    private static final String REGEX_PREFIX = "regex:";

    @Override
    public boolean isActive(Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
        Activation activation = profile.getActivation();

        if (activation == null) {
            return false;
        }

        ActivationOS os = activation.getOs();

        if (os == null) {
            return false;
        }

        boolean active = ensureAtLeastOneNonNull(os);

        String actualOsName = context.getSystemProperties()
                .getOrDefault("os.name", Os.OS_NAME)
                .toLowerCase(Locale.ENGLISH);
        String actualOsArch = context.getSystemProperties()
                .getOrDefault("os.arch", Os.OS_ARCH)
                .toLowerCase(Locale.ENGLISH);
        String actualOsVersion = context.getSystemProperties()
                .getOrDefault("os.version", Os.OS_VERSION)
                .toLowerCase(Locale.ENGLISH);

        if (active && os.getFamily() != null) {
            active = determineFamilyMatch(os.getFamily(), actualOsName);
        }
        if (active && os.getName() != null) {
            active = determineNameMatch(os.getName(), actualOsName);
        }
        if (active && os.getArch() != null) {
            active = determineArchMatch(os.getArch(), actualOsArch);
        }
        if (active && os.getVersion() != null) {
            active = determineVersionMatch(os.getVersion(), actualOsVersion);
        }

        return active;
    }

    @Override
    public boolean presentInConfig(Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
        Activation activation = profile.getActivation();

        if (activation == null) {
            return false;
        }

        ActivationOS os = activation.getOs();

        if (os == null) {
            return false;
        }
        return true;
    }

    private boolean ensureAtLeastOneNonNull(ActivationOS os) {
        return os.getArch() != null || os.getFamily() != null || os.getName() != null || os.getVersion() != null;
    }

    private boolean determineVersionMatch(String expectedVersion, String actualVersion) {
        String test = expectedVersion;
        boolean reverse = false;
        final boolean result;
        if (test.startsWith(REGEX_PREFIX)) {
            result = actualVersion.matches(test.substring(REGEX_PREFIX.length()));
        } else {
            if (test.startsWith("!")) {
                reverse = true;
                test = test.substring(1);
            }
            result = actualVersion.equalsIgnoreCase(test);
        }

        return reverse != result;
    }

    private boolean determineArchMatch(String expectedArch, String actualArch) {
        String test = expectedArch.toLowerCase(Locale.ENGLISH);
        boolean reverse = false;

        if (test.startsWith("!")) {
            reverse = true;
            test = test.substring(1);
        }

        boolean result = actualArch.equals(test);

        return reverse ? !result : result;
    }

    private boolean determineNameMatch(String expectedName, String actualName) {
        String test = expectedName.toLowerCase(Locale.ENGLISH);
        boolean reverse = false;

        if (test.startsWith("!")) {
            reverse = true;
            test = test.substring(1);
        }

        boolean result = actualName.equals(test);

        return reverse ? !result : result;
    }

    private boolean determineFamilyMatch(String family, String actualName) {
        String test = family.toLowerCase(Locale.ENGLISH);
        boolean reverse = false;

        if (test.startsWith("!")) {
            reverse = true;
            test = test.substring(1);
        }

        boolean result = Os.isFamily(test, actualName);

        return reverse ? !result : result;
    }
}
