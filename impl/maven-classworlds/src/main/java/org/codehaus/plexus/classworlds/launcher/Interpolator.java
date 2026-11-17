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
package org.codehaus.plexus.classworlds.launcher;

import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * String interpolator supporting {@code ${var}}, {@code ${var:-default}} and {@code ${var:+alternate}}
 * syntax. Copied from {@code org.apache.maven.impl.model.DefaultInterpolator} to keep maven-classworlds
 * free of any maven-impl dependency.
 */
class Interpolator {

    private static final char ESCAPE_CHAR = '\\';
    private static final String DELIM_START = "${";
    private static final String DELIM_STOP = "}";
    private static final String MARKER = "$__";
    private static final String OP_DEFAULT = ":-";
    private static final String OP_ALTERNATE = ":+";

    private Interpolator() {}

    /**
     * Interpolates {@code val}, resolving {@code ${name}} placeholders via {@code callback}.
     * Supports {@code ${var:-default}} (use default when var is absent/empty) and
     * {@code ${var:+alternate}} (use alternate when var is present and non-empty).
     *
     * @param val      the string to interpolate
     * @param callback function that resolves a variable name to its value, or {@code null}
     * @return the interpolated string
     */
    static String interpolate(String val, UnaryOperator<String> callback) {
        return unescape(doSubstVars(val, null, null, callback));
    }

    private static String doSubstVars(
            String val, String currentKey, Set<String> cycleMap, UnaryOperator<String> callback) {
        if (val == null || val.isEmpty()) {
            return val;
        }
        if (cycleMap == null) {
            cycleMap = new HashSet<>();
        }
        if (currentKey != null) {
            cycleMap.add(currentKey);
        }
        int startDelim;
        int stopDelim = -1;
        do {
            stopDelim = val.indexOf(DELIM_STOP, stopDelim + 1);
            while (stopDelim > 0 && val.charAt(stopDelim - 1) == ESCAPE_CHAR) {
                stopDelim = val.indexOf(DELIM_STOP, stopDelim + 1);
            }
            startDelim = val.indexOf(DELIM_START);
            while (stopDelim >= 0) {
                int idx = val.indexOf(DELIM_START, startDelim + DELIM_START.length());
                if ((idx < 0) || (idx > stopDelim)) {
                    break;
                } else if (idx < stopDelim) {
                    startDelim = idx;
                }
            }
        } while (startDelim >= 0 && stopDelim >= 0 && stopDelim < startDelim + DELIM_START.length());
        if ((startDelim < 0) || (stopDelim < 0)) {
            cycleMap.remove(currentKey);
            return val;
        }
        String variable = val.substring(startDelim + DELIM_START.length(), stopDelim);
        String substValue = processSubstitution(variable, cycleMap, callback);
        val = val.substring(0, startDelim) + substValue + val.substring(stopDelim + DELIM_STOP.length());
        val = doSubstVars(val, currentKey, cycleMap, callback);
        cycleMap.remove(currentKey);
        return val;
    }

    private static String processSubstitution(String variable, Set<String> cycleMap, UnaryOperator<String> callback) {
        int startIdx = 0;
        String substValue = null;
        while (startIdx < variable.length()) {
            int idx1 = variable.indexOf(OP_DEFAULT, startIdx);
            int idx2 = variable.indexOf(OP_ALTERNATE, startIdx);
            int idx = idx1 >= 0 ? (idx2 >= 0 ? Math.min(idx1, idx2) : idx1) : idx2;
            if (idx < 0) {
                if (substValue == null) {
                    substValue = resolveVariable(variable.substring(startIdx), cycleMap, callback);
                }
                break;
            }
            String varPart = variable.substring(startIdx, idx);
            if (substValue == null) {
                substValue = resolveVariable(varPart, cycleMap, callback);
            }
            int nextIdx1 = variable.indexOf(OP_DEFAULT, idx + 2);
            int nextIdx2 = variable.indexOf(OP_ALTERNATE, idx + 2);
            int nextIdx = nextIdx1 >= 0 ? (nextIdx2 >= 0 ? Math.min(nextIdx1, nextIdx2) : nextIdx1) : nextIdx2;
            String op = variable.substring(idx, idx + 2);
            String opValue = variable.substring(idx + 2, nextIdx >= 0 ? nextIdx : variable.length());
            String processedOpValue = doSubstVars(opValue, variable, cycleMap, callback);
            if (OP_ALTERNATE.equals(op)) {
                if (substValue != null && !substValue.isEmpty()) {
                    substValue = processedOpValue;
                    break;
                }
            } else {
                // OP_DEFAULT
                if (substValue == null || substValue.isEmpty()) {
                    substValue = processedOpValue;
                    break;
                }
            }
            startIdx = nextIdx >= 0 ? nextIdx : variable.length();
        }
        // When the variable is unresolvable (no value, no default), preserve the placeholder
        // so that filter() can detect it and throw ConfigurationException.
        if (substValue == null) {
            substValue = MARKER + "{" + variable + "}";
        }
        return substValue;
    }

    private static String resolveVariable(String variable, Set<String> cycleMap, UnaryOperator<String> callback) {
        if (!cycleMap.add(variable)) {
            throw new IllegalArgumentException("Recursive variable reference: " + variable);
        }
        String value = callback != null ? callback.apply(variable) : null;
        if (value != null) {
            value = doSubstVars(value, variable, cycleMap, callback);
        }
        cycleMap.remove(variable);
        return value;
    }

    private static String unescape(String val) {
        if (val == null || val.isEmpty()) {
            return val;
        }
        if (val.contains(MARKER)) {
            val = val.replace(MARKER, "$");
        }
        int escape = val.indexOf(ESCAPE_CHAR);
        while (escape >= 0 && escape < val.length() - 1) {
            char c = val.charAt(escape + 1);
            if (c == '{' || c == '}') {
                val = val.substring(0, escape) + val.substring(escape + 1);
            }
            escape = val.indexOf(ESCAPE_CHAR, escape + 1);
        }
        return val;
    }
}
