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
package org.apache.maven.toolchain.java;

import org.apache.maven.toolchain.Toolchain;

/**
 * JDK toolchain interface.
 *
 * @author Jason van Zyl
 * @author Milos Kleint
 * @since 2.0.9, renamed from JavaToolChain in 3.2.4
 */
public interface JavaToolchain extends Toolchain {
    //    /**
    //     * Returns a list of {@link java.io.File}s which represents the bootstrap libraries for the
    //     * runtime environment. The Bootstrap libraries include libraries in JRE's
    //     * extension directory, if there are any.
    //     *
    //     * @return List
    //     */
    //    List getBootstrapLibraries();
    //
    //    /**
    //     * Returns a list of {@link java.io.File}s which represent the libraries recognized by
    //     * default by the platform. Usually it corresponds to contents of CLASSPATH
    //     * environment variable.
    //     *
    //     * @return List
    //     */
    //    List getStandardLibraries();
    //
    //    /**
    //     * Returns a {@link java.io.File}s which represent the locations of the source of the JDK,
    //     * or an empty collection when the location is not set or is invalid.
    //     *
    //     * @return List
    //     */
    //    List getSourceDirectories();
    //
    //    /**
    //     * Returns a {@link java.io.File}s which represent the locations of the Javadoc for this platform,
    //     * or empty collection if the location is not set or invalid
    //     *
    //     * @return List
    //     */
    //    List getJavadocDirectories();
}
