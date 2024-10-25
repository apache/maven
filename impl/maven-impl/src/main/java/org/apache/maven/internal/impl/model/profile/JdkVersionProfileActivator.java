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
package org.apache.maven.internal.impl.model.profile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.apache.maven.api.services.model.ProfileActivator;

/**
 * Determines profile activation based on the version of the current Java runtime.
 *
 * @see Activation#getJdk()
 */
@Named("jdk-version")
@Singleton
public class JdkVersionProfileActivator implements ProfileActivator {

    private static final Pattern FILTER_1 = Pattern.compile("[^\\d._-]");
    private static final Pattern FILTER_2 = Pattern.compile("[._-]");
    private static final Pattern FILTER_3 = Pattern.compile("\\."); // used for split now

    @Override
    public boolean isActive(Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
        Activation activation = profile.getActivation();

        if (activation == null) {
            return false;
        }

        String jdk = activation.getJdk();

        if (jdk == null) {
            return false;
        }

        String version = context.getSystemProperties().get("java.version");

        if (version == null || version.isEmpty()) {
            problems.add(
                    BuilderProblem.Severity.ERROR,
                    ModelProblem.Version.BASE,
                    "Failed to determine Java version for profile " + profile.getId(),
                    activation.getLocation("jdk"));
            return false;
        }
        try {
            return isJavaVersionCompatible(jdk, version);
        } catch (NumberFormatException e) {
            problems.add(
                    BuilderProblem.Severity.WARNING,
                    ModelProblem.Version.BASE,
                    "Failed to determine JDK activation for profile " + profile.getId() + " due invalid JDK version: '"
                            + version + "'",
                    profile.getLocation("jdk"));
            return false;
        }
    }

    public static boolean isJavaVersionCompatible(String requiredJdkRange, String currentJavaVersion) {
        if (requiredJdkRange.startsWith("!")) {
            return !currentJavaVersion.startsWith(requiredJdkRange.substring(1));
        } else if (isRange(requiredJdkRange)) {
            return isInRange(currentJavaVersion, getRange(requiredJdkRange));
        } else {
            return currentJavaVersion.startsWith(requiredJdkRange);
        }
    }

    @Override
    public boolean presentInConfig(Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
        Activation activation = profile.getActivation();

        if (activation == null) {
            return false;
        }

        String jdk = activation.getJdk();

        return jdk != null;
    }

    private static boolean isInRange(String value, List<RangeValue> range) {
        int leftRelation = getRelationOrder(value, range.get(0), true);

        if (leftRelation == 0) {
            return true;
        }

        if (leftRelation < 0) {
            return false;
        }

        return getRelationOrder(value, range.get(1), false) <= 0;
    }

    private static int getRelationOrder(String value, RangeValue rangeValue, boolean isLeft) {
        if (rangeValue.value.isEmpty()) {
            return isLeft ? 1 : -1;
        }

        value = FILTER_1.matcher(value).replaceAll("");

        List<String> valueTokens = new ArrayList<>(Arrays.asList(FILTER_2.split(value)));
        List<String> rangeValueTokens = new ArrayList<>(Arrays.asList(FILTER_3.split(rangeValue.value)));

        addZeroTokens(valueTokens, 3);
        addZeroTokens(rangeValueTokens, 3);

        for (int i = 0; i < 3; i++) {
            int x = Integer.parseInt(valueTokens.get(i));
            int y = Integer.parseInt(rangeValueTokens.get(i));
            if (x < y) {
                return -1;
            } else if (x > y) {
                return 1;
            }
        }
        if (!rangeValue.closed) {
            return isLeft ? -1 : 1;
        }
        return 0;
    }

    private static void addZeroTokens(List<String> tokens, int max) {
        while (tokens.size() < max) {
            tokens.add("0");
        }
    }

    private static boolean isRange(String value) {
        return value.startsWith("[") || value.startsWith("(");
    }

    private static List<RangeValue> getRange(String range) {
        List<RangeValue> ranges = new ArrayList<>();

        for (String token : range.split(",")) {
            if (token.startsWith("[")) {
                ranges.add(new RangeValue(token.replace("[", ""), true));
            } else if (token.startsWith("(")) {
                ranges.add(new RangeValue(token.replace("(", ""), false));
            } else if (token.endsWith("]")) {
                ranges.add(new RangeValue(token.replace("]", ""), true));
            } else if (token.endsWith(")")) {
                ranges.add(new RangeValue(token.replace(")", ""), false));
            } else if (token.isEmpty()) {
                ranges.add(new RangeValue("", false));
            }
        }
        if (ranges.size() < 2) {
            ranges.add(new RangeValue("99999999", false));
        }
        return ranges;
    }

    private static class RangeValue {
        private String value;

        private boolean closed;

        RangeValue(String value, boolean closed) {
            this.value = value.trim();
            this.closed = closed;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
