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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Benjamin Bentmann
 */
class ItUtils {

    public static String calcHash(File file, String algo) throws Exception {
        MessageDigest digester = MessageDigest.getInstance(algo);

        DigestInputStream dis;
        try (FileInputStream is = new FileInputStream(file)) {
            dis = new DigestInputStream(is, digester);

            for (byte[] buffer = new byte[1024 * 4]; dis.read(buffer) >= 0; ) {
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

    /**
     * @deprecated Use {@link Verifier#setUserHomeDirectory(Path)} instead.
     */
    @Deprecated
    public static void setUserHome(Verifier verifier, File file) {
        setUserHome(verifier, file.toPath());
    }

    /**
     * @deprecated Use {@link Verifier#setUserHomeDirectory(Path)} instead.
     */
    @Deprecated
    public static void setUserHome(Verifier verifier, Path home) {
        verifier.setUserHomeDirectory(home);
    }

    public static void assertCanonicalFileEquals(File expected, File actual) throws IOException {
        assertEquals(expected.getCanonicalFile(), actual.getCanonicalFile());
    }

    public static void assertCanonicalFileEquals(String expected, String actual, String message) throws IOException {
        assertEquals(new File(expected).getCanonicalFile(), new File(actual).getCanonicalFile(), message);
    }

    public static void assertCanonicalFileEquals(String expected, String actual) throws IOException {
        assertEquals(new File(expected).getCanonicalFile(), new File(actual).getCanonicalFile());
    }
}
