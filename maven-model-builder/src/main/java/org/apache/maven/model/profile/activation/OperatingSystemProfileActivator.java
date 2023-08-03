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

/**
 * Determines profile activation based on the operating system of the current runtime platform.
 *
 * @author Benjamin Bentmann
 * @see ActivationOS
 */
@Named("os")
@Singleton
public class OperatingSystemProfileActivator implements ProfileActivator {

    private static final String FAMILY_DOS = "dos";

    private static final String FAMILY_MAC = "mac";

    private static final String FAMILY_NETWARE = "netware";

    private static final String FAMILY_OS2 = "os/2";

    private static final String FAMILY_TANDEM = "tandem";

    private static final String FAMILY_UNIX = "unix";

    private static final String FAMILY_WINDOWS = "windows";

    private static final String FAMILY_WIN9X = "win9x";

    private static final String FAMILY_ZOS = "z/os";

    private static final String FAMILY_OS400 = "os/400";

    private static final String FAMILY_OPENVMS = "openvms";

    private static final String PATH_SEP = System.getProperty("path.separator");

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.US);

    private static final String OS_ARCH = System.getProperty("os.arch").toLowerCase(Locale.US);

    private static final String OS_VERSION = System.getProperty("os.version").toLowerCase(Locale.US);

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

        if (active && os.getFamily() != null) {
            active = determineFamilyMatch(os.getFamily());
        }
        if (active && os.getName() != null) {
            active = determineNameMatch(os.getName());
        }
        if (active && os.getArch() != null) {
            active = determineArchMatch(os.getArch());
        }
        if (active && os.getVersion() != null) {
            active = determineVersionMatch(os.getVersion());
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

        return os != null;
    }

    private boolean ensureAtLeastOneNonNull(ActivationOS os) {
        return os.getArch() != null || os.getFamily() != null || os.getName() != null || os.getVersion() != null;
    }

    private boolean determineVersionMatch(String version) {
        String test = version;
        boolean reverse = false;

        if (test.startsWith("!")) {
            reverse = true;
            test = test.substring(1);
        }

        boolean result = test.toLowerCase(Locale.US).equals(OS_VERSION);

        return reverse != result;
    }

    private boolean determineArchMatch(String arch) {
        String test = arch;
        boolean reverse = false;

        if (test.startsWith("!")) {
            reverse = true;
            test = test.substring(1);
        }

        boolean result = test.toLowerCase(Locale.US).equals(OS_ARCH);

        return reverse != result;
    }

    private boolean determineNameMatch(String name) {
        String test = name;
        boolean reverse = false;

        if (test.startsWith("!")) {
            reverse = true;
            test = test.substring(1);
        }

        boolean result = test.toLowerCase(Locale.US).equals(OS_NAME);

        return reverse != result;
    }

    private boolean determineFamilyMatch(String family) {
        String test = family;
        boolean reverse = false;

        if (test.startsWith("!")) {
            reverse = true;
            test = test.substring(1);
        }

        boolean result = isFamily(test);

        return reverse != result;
    }

    private boolean isFamily(String family) {
        boolean isFamily = true;
        if (family != null) {
            if (family.equalsIgnoreCase(FAMILY_WINDOWS)) {
                isFamily = OS_NAME.contains(FAMILY_WINDOWS);
            } else if (family.equalsIgnoreCase(FAMILY_OS2)) {
                isFamily = OS_NAME.contains(FAMILY_OS2);
            } else if (family.equalsIgnoreCase(FAMILY_NETWARE)) {
                isFamily = OS_NAME.contains(FAMILY_NETWARE);
            } else if (family.equalsIgnoreCase(FAMILY_DOS)) {
                isFamily = PATH_SEP.equals(";")
                        && !isFamily(FAMILY_NETWARE)
                        && !isFamily(FAMILY_WINDOWS)
                        && !isFamily(FAMILY_WIN9X);

            } else if (family.equalsIgnoreCase(FAMILY_MAC)) {
                isFamily = OS_NAME.contains(FAMILY_MAC);
            } else if (family.equalsIgnoreCase(FAMILY_TANDEM)) {
                isFamily = OS_NAME.contains("nonstop_kernel");
            } else if (family.equalsIgnoreCase(FAMILY_UNIX)) {
                isFamily = PATH_SEP.equals(":")
                        && !isFamily(FAMILY_OPENVMS)
                        && (!isFamily(FAMILY_MAC) || OS_NAME.endsWith("x"));
            } else if (family.equalsIgnoreCase(FAMILY_WIN9X)) {
                isFamily = isFamily(FAMILY_WINDOWS)
                        && (OS_NAME.contains("95")
                                || OS_NAME.contains("98")
                                || OS_NAME.contains("me")
                                || OS_NAME.contains("ce"));
            } else if (family.equalsIgnoreCase(FAMILY_ZOS)) {
                isFamily = OS_NAME.contains(FAMILY_ZOS) || OS_NAME.contains("os/390");
            } else if (family.equalsIgnoreCase(FAMILY_OS400)) {
                isFamily = OS_NAME.contains(FAMILY_OS400);
            } else if (family.equalsIgnoreCase(FAMILY_OPENVMS)) {
                isFamily = OS_NAME.contains(FAMILY_OPENVMS);
            } else {
                isFamily = OS_NAME.contains(family.toLowerCase(Locale.US));
            }
        }
        return isFamily;
    }
}
