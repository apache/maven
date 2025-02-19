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
package org.apache.maven.cling.invoker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class Directory implements Supplier<Path>, Function<String, Path> {
    protected Path directory;

    public Directory(Path directory) {
        this.directory = Utils.getCanonicalPath(directory);
    }

    @Override
    public Path get() {
        return directory;
    }

    @Override
    public Path apply(String s) {
        return Utils.getCanonicalPath(directory.resolve(s));
    }

    public static class MutableDirectory extends Directory {
        public MutableDirectory(Path directory) {
            super(directory);
        }

        public void changeDirectory(String seg) {
            requireNonNull(seg, "seg");
            if ("..".equals(seg)) {
                Path parent = directory.getParent();
                if (parent == null) {
                    throw new IllegalArgumentException("Non existent parent directory of " + directory);
                }
                this.directory = parent;
            } else {
                Path newWorkingDirectory = Utils.getCanonicalPath(directory.resolve(seg));
                if (Files.isDirectory(newWorkingDirectory)) {
                    directory = newWorkingDirectory;
                } else {
                    throw new IllegalArgumentException("Non existent directory '" + seg + "'");
                }
            }
        }
    }
}
