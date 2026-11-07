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
package org.apache.maven.logging.slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Converts SLF4J {@code {}}-style message templates to positional {@code {0}}-style,
 * which is the template format used by Maven's {@link org.apache.maven.api.logging.Logger} API.
 * <p>
 * Results are cached in a {@link java.util.concurrent.ConcurrentHashMap}. The cache has no
 * eviction policy: it relies on the convention that log templates are compile-time string
 * constants, keeping the entry count bounded in practice. Plugin code that constructs
 * templates dynamically (e.g. by string concatenation) would cause unbounded growth —
 * such usage is a misuse of the logging API.
 * <p>
 * Example: {@code "Resolved {} files in {}"} → {@code "Resolved {0} files in {1}"}
 *
 * @since 4.2.0
 */
final class Slf4jTemplateConverter {

    private static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<>();

    private Slf4jTemplateConverter() {}

    /**
     * Converts an SLF4J {@code {}}-style template to MessageFormat {@code {N}}-style.
     * Returns the input unchanged if it contains no {@code {}} placeholders.
     *
     * @param slf4jTemplate the SLF4J template, may be {@code null}
     * @return the converted template, or the original if no conversion was needed
     */
    static String convert(String slf4jTemplate) {
        if (slf4jTemplate == null || !slf4jTemplate.contains("{}")) {
            return slf4jTemplate;
        }
        return CACHE.computeIfAbsent(slf4jTemplate, Slf4jTemplateConverter::doConvert);
    }

    private static String doConvert(String template) {
        StringBuilder sb = new StringBuilder(template.length() + 16);
        int argIdx = 0;
        int start = 0;
        int idx;
        while ((idx = template.indexOf("{}", start)) >= 0) {
            sb.append(template, start, idx);
            sb.append('{').append(argIdx++).append('}');
            start = idx + 2;
        }
        sb.append(template, start, template.length());
        return sb.toString();
    }
}
