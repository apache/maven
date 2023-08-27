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
package org.apache.maven.utils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Condition that tests the OS type.
 *
 * @author Stefan Bodewig
 * @author Magesh Umasankar
 * @author Brian Fox
 *
 */
public class Os {
    /**
     * define the families for easier reference
     */
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

    /**
     * store the valid families
     */
    private static final Set<String> VALID_FAMILIES = setValidFamilies();

    /**
     * get the current info
     */
    private static final String PATH_SEP = System.getProperty("path.separator");

    public static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.US);

    public static final String OS_ARCH = System.getProperty("os.arch").toLowerCase(Locale.US);

    public static final String OS_VERSION = System.getProperty("os.version").toLowerCase(Locale.US);

    /**
     * Make sure this method is called after static fields it depends on have been set!
     */
    public static final String OS_FAMILY = getOsFamily();

    private String family;

    private String name;

    private String version;

    private String arch;

    /**
     * Default constructor
     */
    public Os() {}

    /**
     * Constructor that sets the family attribute
     *
     * @param family a String value
     */
    public Os(String family) {
        setFamily(family);
    }

    /**
     * Initializes the set of valid families.
     */
    private static Set<String> setValidFamilies() {
        Set<String> valid = new HashSet<String>();
        valid.add(FAMILY_DOS);
        valid.add(FAMILY_MAC);
        valid.add(FAMILY_NETWARE);
        valid.add(FAMILY_OS2);
        valid.add(FAMILY_TANDEM);
        valid.add(FAMILY_UNIX);
        valid.add(FAMILY_WINDOWS);
        valid.add(FAMILY_WIN9X);
        valid.add(FAMILY_ZOS);
        valid.add(FAMILY_OS400);
        valid.add(FAMILY_OPENVMS);

        return valid;
    }

    /**
     * Sets the desired OS family type
     *
     * @param f The OS family type desired<br>
     *            Possible values:
     *            <ul>
     *            <li>dos</li>
     *            <li>mac</li>
     *            <li>netware</li>
     *            <li>os/2</li>
     *            <li>tandem</li>
     *            <li>unix</li>
     *            <li>windows</li>
     *            <li>win9x</li>
     *            <li>z/os</li>
     *            <li>os/400</li>
     *            <li>openvms</li>
     *            </ul>
     */
    public void setFamily(String f) {
        family = f.toLowerCase(Locale.US);
    }

    /**
     * Sets the desired OS name
     *
     * @param name The OS name
     */
    public void setName(String name) {
        this.name = name.toLowerCase(Locale.US);
    }

    /**
     * Sets the desired OS architecture
     *
     * @param arch The OS architecture
     */
    public void setArch(String arch) {
        this.arch = arch.toLowerCase(Locale.US);
    }

    /**
     * Sets the desired OS version
     *
     * @param version The OS version
     */
    public void setVersion(String version) {
        this.version = version.toLowerCase(Locale.US);
    }

    /**
     * @return Determines if the current OS matches the type of that set in setFamily.
     *
     * @see Os#setFamily(String)
     * @throws Exception any errir
     */
    public boolean eval() throws Exception {
        return isOs(family, name, arch, version);
    }

    /**
     * Determines if the current OS matches the given OS family.
     *
     * @param family the family to check for
     * @return true if the OS matches
     * @since 1.0
     */
    public static boolean isFamily(String family) {
        return isOs(family, null, null, null);
    }

    /**
     * Determines if the current OS matches the given OS name.
     *
     * @param name the OS name to check for
     * @return true if the OS matches
     * @since 1.0
     */
    public static boolean isName(String name) {
        return isOs(null, name, null, null);
    }

    /**
     * Determines if the current OS matches the given OS architecture.
     *
     * @param arch the OS architecture to check for
     * @return true if the OS matches
     * @since 1.0
     */
    public static boolean isArch(String arch) {
        return isOs(null, null, arch, null);
    }

    /**
     * Determines if the current OS matches the given OS version.
     *
     * @param version the OS version to check for
     * @return true if the OS matches
     * @since 1.0
     */
    public static boolean isVersion(String version) {
        return isOs(null, null, null, version);
    }

    /**
     * Determines if the current OS matches the given OS family, name, architecture and version. The name, architecture
     * and version are compared to the System properties os.name, os.version and os.arch in a case-independent way.
     *
     * @param family The OS family
     * @param name The OS name
     * @param arch The OS architecture
     * @param version The OS version
     * @return true if the OS matches
     * @since 1.0
     */
    public static boolean isOs(String family, String name, String arch, String version) {
        boolean retValue = false;

        if (family != null || name != null || arch != null || version != null) {

            boolean isFamily = true;
            boolean isName = true;
            boolean isArch = true;
            boolean isVersion = true;

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
            if (name != null) {
                isName = name.toLowerCase(Locale.US).equals(OS_NAME);
            }
            if (arch != null) {
                isArch = arch.toLowerCase(Locale.US).equals(OS_ARCH);
            }
            if (version != null) {
                isVersion = version.toLowerCase(Locale.US).equals(OS_VERSION);
            }
            retValue = isFamily && isName && isArch && isVersion;
        }
        return retValue;
    }

    /**
     * Helper method to determine the current OS family.
     *
     * @return name of current OS family.
     * @since 1.4.2
     */
    private static String getOsFamily() {
        // in case the order of static initialization is
        // wrong, get the list
        // safely.
        Set<String> families = null;
        if (!VALID_FAMILIES.isEmpty()) {
            families = VALID_FAMILIES;
        } else {
            families = setValidFamilies();
        }
        for (String fam : families) {
            if (Os.isFamily(fam)) {
                return fam;
            }
        }
        return null;
    }

    /**
     * Helper method to check if the given family is in the following list:
     * <ul>
     * <li>dos</li>
     * <li>mac</li>
     * <li>netware</li>
     * <li>os/2</li>
     * <li>tandem</li>
     * <li>unix</li>
     * <li>windows</li>
     * <li>win9x</li>
     * <li>z/os</li>
     * <li>os/400</li>
     * <li>openvms</li>
     * </ul>
     *
     * @param theFamily the family to check.
     * @return true if one of the valid families.
     * @since 1.4.2
     */
    public static boolean isValidFamily(String theFamily) {
        return (VALID_FAMILIES.contains(theFamily));
    }

    /**
     * @return a copy of the valid families
     * @since 1.4.2
     */
    public static Set<String> getValidFamilies() {
        return new HashSet<String>(VALID_FAMILIES);
    }
}
