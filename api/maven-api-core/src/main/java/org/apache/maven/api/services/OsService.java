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
package org.apache.maven.api.services;

import org.apache.maven.api.Service;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Service for detecting and providing information about the operating system (OS)
 * on which the application is running.
 * <p>
 * This service provides a platform-independent way to:
 * <ul>
 * <li>Query basic OS information like name, architecture, and version</li>
 * <li>Determine the OS family (e.g., Windows, Unix, Mac)</li>
 * <li>Check if the current OS is Windows-based</li>
 * </ul>
 * <p>
 * The service implementation uses system properties to detect OS characteristics:
 * <ul>
 * <li>os.name: The operating system name</li>
 * <li>os.arch: The operating system architecture</li>
 * <li>os.version: The operating system version</li>
 * </ul>
 * <p>
 * Supported OS families include:
 * <ul>
 * <li>windows: All Windows variants</li>
 * <li>win9x: Windows 95, 98, ME, CE</li>
 * <li>winnt: Windows NT variants</li>
 * <li>unix: Unix-like systems (including Linux)</li>
 * <li>mac: macOS (including Darwin)</li>
 * <li>os/2: OS/2 variants</li>
 * <li>netware: Novell NetWare</li>
 * <li>dos: DOS variants</li>
 * <li>tandem: Tandem systems</li>
 * <li>openvms: OpenVMS</li>
 * <li>z/os: z/OS and OS/390</li>
 * <li>os/400: OS/400</li>
 * </ul>
 *
 * @since 4.0.0
 */
@Experimental
public interface OsService extends Service {
    /**
     * Returns the OS full name as reported by the system property "os.name".
     * The value is converted to lowercase for consistency.
     *
     * @return the operating system name (never null)
     */
    @Nonnull
    String name();

    /**
     * Returns the OS architecture as reported by the system property "os.arch".
     * The value is converted to lowercase for consistency.
     *
     * @return the operating system architecture (never null)
     */
    @Nonnull
    String arch();

    /**
     * Returns the OS version as reported by the system property "os.version".
     * The value is converted to lowercase for consistency.
     *
     * @return the operating system version (never null)
     */
    @Nonnull
    String version();

    /**
     * Returns the OS family name based on OS detection rules.
     * This categorizes the OS into one of the supported families
     * (e.g., "windows", "unix", "mac").
     *
     * @return the operating system family name (never null)
     */
    @Nonnull
    String family();

    /**
     * Checks if the current operating system belongs to the Windows family.
     * This includes all Windows variants (95, 98, ME, NT, 2000, XP, Vista, 7, 8, 10, 11).
     *
     * @return true if the current OS is any Windows variant, false otherwise
     */
    boolean isWindows();
}
