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

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;

/**
 * Test suite ordering that orders tests by prefix (gh-xxx, mng-xxx, it-xxx) in descending order.
 * This ensures newer tests (higher numbers) are run first, which is useful for fail-fast behavior
 * since newer tests are more likely to fail.
 */
public class TestSuiteOrdering implements ClassOrderer {

    private static final Pattern GH_PATTERN = Pattern.compile(".*MavenITgh(\\d+).*");
    private static final Pattern MNG_PATTERN = Pattern.compile(".*MavenITmng(\\d+).*");
    private static final Pattern IT_PATTERN = Pattern.compile(".*MavenIT(\\d+).*");

    private static void infoProperty(PrintStream info, String property) {
        info.println(property + ": " + System.getProperty(property));
    }

    static {
        try {
            // TODO: workaround for https://github.com/apache/maven-integration-testing/pull/232
            // The verifier currently uses system properties to configure itself, such as
            // maven.home (see
            // https://github.com/apache/maven-integration-testing/blob/ba72268198fb4c68890f11bfa0aac3f4889c79b9/core-it-suite/pom.xml#L509-L511)
            // or other properties to configure the maven that will be launched.  Using system properties
            // make impossible the detection whether a system property has been set by the maven being run
            // or by the code that wants to use the verifier to create a new embedded maven, which means
            // those properties can not be cleared by the verifier.  So clear those properties here, as
            // we do want to isolate the tests from the outside environment.
            System.clearProperty("maven.bootclasspath");
            System.clearProperty("maven.conf");
            System.clearProperty("classworlds.conf");

            // Set maven.version system property (needed by some tests)
            Verifier verifier = new Verifier("", false);
            String mavenVersion = verifier.getMavenVersion();
            System.setProperty("maven.version", mavenVersion);

            String basedir = System.getProperty("basedir", ".");
            try (PrintStream info = new PrintStream(Files.newOutputStream(Paths.get(basedir, "target/info.txt")))) {
                infoProperty(info, "maven.version");
                infoProperty(info, "java.version");
                infoProperty(info, "os.name");
                infoProperty(info, "os.version");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void orderClasses(ClassOrdererContext context) {
        context.getClassDescriptors()
                .sort(Comparator.comparing(this::getOrderKey).reversed());
    }

    private String getOrderKey(ClassDescriptor classDescriptor) {
        String className = classDescriptor.getTestClass().getSimpleName();

        // Check for gh- pattern first (highest priority)
        Matcher ghMatcher = GH_PATTERN.matcher(className);
        if (ghMatcher.matches()) {
            int number = Integer.parseInt(ghMatcher.group(1));
            return String.format("3-%08d", number); // Prefix with 3 for highest priority
        }

        // Check for mng- pattern (medium priority)
        Matcher mngMatcher = MNG_PATTERN.matcher(className);
        if (mngMatcher.matches()) {
            int number = Integer.parseInt(mngMatcher.group(1));
            return String.format("2-%08d", number); // Prefix with 2 for medium priority
        }

        // Check for it- pattern (lowest priority)
        Matcher itMatcher = IT_PATTERN.matcher(className);
        if (itMatcher.matches()) {
            int number = Integer.parseInt(itMatcher.group(1));
            return String.format("1-%08d", number); // Prefix with 3 for lowest priority
        }

        // For any other tests, use the class name as-is (will be sorted alphabetically)
        return "4-" + className;
    }
}
