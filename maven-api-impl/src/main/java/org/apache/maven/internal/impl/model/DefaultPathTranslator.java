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
package org.apache.maven.internal.impl.model;

import java.io.File;
import java.nio.file.Path;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.model.PathTranslator;

/**
 * Resolves relative paths against a specific base directory.
 *
 */
@Named
@Singleton
public class DefaultPathTranslator implements PathTranslator {

    @Override
    public String alignToBaseDirectory(String path, Path basedir) {
        String result = path;

        if (path != null && basedir != null) {
            path = path.replace('\\', File.separatorChar).replace('/', File.separatorChar);

            File file = new File(path);
            if (file.isAbsolute()) {
                // path was already absolute, just normalize file separator and we're done
                result = file.getPath();
            } else if (file.getPath().startsWith(File.separator)) {
                // drive-relative Windows path, don't align with project directory but with drive root
                result = file.getAbsolutePath();
            } else {
                // an ordinary relative path, align with project directory
                result = basedir.resolve(path).normalize().toString();
            }
        }

        return result;
    }
}
