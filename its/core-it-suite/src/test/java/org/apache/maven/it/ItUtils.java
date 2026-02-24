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
package org.apache.maven.it;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import org.codehaus.plexus.util.FileUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Benjamin Bentmann
 */
class ItUtils {

    public static String calcHash(Path file, String algo) throws Exception {
        MessageDigest digester = MessageDigest.getInstance(algo);

        try (InputStream is = new DigestInputStream(Files.newInputStream(file), digester)) {
            byte[] buffer = new byte[1024 * 4];
            while (is.read(buffer) >= 0) {
                // just read it
            }
        }

        byte[] digest = digester.digest();
        StringBuilder hash = new StringBuilder(digest.length * 2);
        for (byte aDigest : digest) {
            int b = aDigest & 0xFF;
            if (b < 0x10) {
                hash.append('0');
            }
            hash.append(Integer.toHexString(b));
        }

        return hash.toString();
    }

    public static void assertCanonicalFileEquals(Path expected, Path actual) throws IOException {
        assertEquals(canonicalPath(expected), canonicalPath(actual));
    }

    public static void createFile(Path path) throws IOException {
        Files.newInputStream(path, StandardOpenOption.CREATE).close();
    }

    public static long lastModified(Path path) throws IOException {
        return Files.getLastModifiedTime(path).toMillis();
    }

    public static void lastModified(Path path, long millis) throws IOException {
        Files.setLastModifiedTime(path, FileTime.fromMillis(millis));
    }

    public static void deleteDirectory(Path path) throws IOException {
        FileUtils.deleteDirectory(path.toFile());
    }

    public static void copyDirectoryStructure(Path src, Path dest) throws IOException {
        FileUtils.copyDirectory(src.toFile(), dest.toFile());
    }

    public static String canonicalPath(Path path) throws IOException {
        return path.toFile().getCanonicalPath();
    }
}
