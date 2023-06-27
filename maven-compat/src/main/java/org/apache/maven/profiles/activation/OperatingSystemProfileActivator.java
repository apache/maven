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
package org.apache.maven.profiles.activation;

import java.util.Locale;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationOS;
import org.apache.maven.model.Profile;

/**
 * OperatingSystemProfileActivator
 */
@Deprecated
public class OperatingSystemProfileActivator implements ProfileActivator {

    public static final String FAMILY_DOS = "dos";

    public static final String FAMILY_MAC = "mac";

    public static final String FAMILY_NETWARE = "netware";

    public static final String FAMILY_OS2 = "os/2";

    public static final String FAMILY_TANDEM = "tandem";

    public static final String FAMILY_UNIX = "unix";

    public static final String FAMILY_WINDOWS = "windows";

    public static final String FAMILY_WIN9X = "win9x";

    public static final String FAMILY_ZOS = "z/os";

    public static final String FAMILY_OS400 = "os/400";

    public static final String FAMILY_OPENVMS = "openvms";

    private static final String PATH_SEP = System.getProperty("path.separator");

    public static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.US);

    public static final String OS_ARCH = System.getProperty("os.arch").toLowerCase(Locale.US);

    public static final String OS_VERSION = System.getProperty("os.version").toLowerCase(Locale.US);

    public boolean canDetermineActivation(Profile profile) {
        Activation activation = profile.getActivation();
        return activation != null && activation.getOs() != null;
    }

    public boolean isActive(Profile profile) {
        Activation activation = profile.getActivation();
        ActivationOS os = activation.getOs();

        boolean result = ensureAtLeastOneNonNull(os);

        if (result && os.getFamily() != null) {
            result = determineFamilyMatch(os.getFamily());
        }
        if (result && os.getName() != null) {
            result = determineNameMatch(os.getName());
        }
        if (result && os.getArch() != null) {
            result = determineArchMatch(os.getArch());
        }
        if (result && os.getVersion() != null) {
            result = determineVersionMatch(os.getVersion());
        }
        return result;
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

        if (reverse) {
            return !result;
        } else {
            return result;
        }
    }

    private boolean determineArchMatch(String arch) {
        String test = arch;
        boolean reverse = false;

        if (test.startsWith("!")) {
            reverse = true;
            test = test.substring(1);
        }

        boolean result = test.toLowerCase(Locale.US).equals(OS_ARCH);

        if (reverse) {
            return !result;
        } else {
            return result;
        }
    }

    private boolean determineNameMatch(String name) {
        String test = name;
        boolean reverse = false;

        if (test.startsWith("!")) {
            reverse = true;
            test = test.substring(1);
        }

        boolean result = test.toLowerCase(Locale.US).equals(OS_NAME);

        if (reverse) {
            return !result;
        } else {
            return result;
        }
    }

    private boolean determineFamilyMatch(String family) {
        String test = family;
        boolean reverse = false;

        if (test.startsWith("!")) {
            reverse = true;
            test = test.substring(1);
        }

        boolean result = isFamily(test);

        if (reverse) {
            return !result;
        } else {
            return result;
        }
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
