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
package org.apache.maven.internal.build.impl.maven.digest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// CHECKSTYLE_OFF: LineLength

/**
 * TODO: replace somehow with
 * <a href="https://github.com/apache/maven-build-cache-extension/blob/master/src/main/java/org/apache/maven/buildcache/hash/HashFactory.java">org.apache.maven.buildcache.hash.HashFactory</a>
 */
public class SHA1Digester {

    public static MessageDigest newInstance() {
        try {
            return MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unsupported JVM", e);
        }
    }

    //
    // convenient helpers
    //

    public static BytesHash digest(String string) {
        MessageDigest digest = newInstance();
        if (string != null) {
            digest.update(string.getBytes(StandardCharsets.UTF_8));
        }
        return new BytesHash(digest.digest());
    }
}
