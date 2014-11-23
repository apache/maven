package org.apache.maven.model.path;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;

/**
 * Resolves relative paths against a specific base directory.
 *
 * @author Jason van Zyl
 */
public interface PathTranslator
{

    /**
     * Resolves the specified path against the given base directory. The resolved path will be absolute and uses the
     * platform-specific file separator if a base directory is given. Otherwise, the input path will be returned
     * unaltered.
     *
     * @param path The path to resolve, may be {@code null}.
     * @param basedir The base directory to resolve relative paths against, may be {@code null}.
     * @return The resolved path or {@code null} if the input path was {@code null}.
     */
    String alignToBaseDirectory( String path, File basedir );

}
