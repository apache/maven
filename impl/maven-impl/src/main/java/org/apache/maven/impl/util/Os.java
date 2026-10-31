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
package org.apache.maven.impl.util;

/**
 * @deprecated Use {@link org.apache.maven.utils.Os} instead.
 */
@Deprecated
public class Os {
    public static final String OS_NAME = org.apache.maven.utils.Os.OS_NAME;
    public static final String OS_ARCH = org.apache.maven.utils.Os.OS_ARCH;
    public static final String OS_VERSION = org.apache.maven.utils.Os.OS_VERSION;

    public static final String OS_FAMILY = org.apache.maven.utils.Os.OS_FAMILY;

    public static final boolean IS_WINDOWS = org.apache.maven.utils.Os.IS_WINDOWS;
    public static final String FAMILY_NT = org.apache.maven.utils.Os.FAMILY_NT;

    private Os() {}

    public static boolean isFamily(String family) {
        return org.apache.maven.utils.Os.isFamily(family);
    }

    public static boolean isFamily(String family, String actualOsName) {
        return org.apache.maven.utils.Os.isFamily(family, actualOsName);
    }
}
