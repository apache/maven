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
package org.apache.maven.cli.logging.impl;

import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.apache.maven.cli.logging.BaseSlf4jConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pseudo-configuration for unsupported slf4j binding.
 *
 * @author Hervé Boutemy
 * @since 3.2.4
 */
public class UnsupportedSlf4jBindingConfiguration extends BaseSlf4jConfiguration {
    private final Logger logger = LoggerFactory.getLogger(UnsupportedSlf4jBindingConfiguration.class);

    private String slf4jBinding;

    private Map<URL, Set<Object>> supported;

    public UnsupportedSlf4jBindingConfiguration(String slf4jBinding, Map<URL, Set<Object>> supported) {
        this.slf4jBinding = slf4jBinding;
        this.supported = supported;
    }

    @Override
    public void activate() {
        logger.warn("The SLF4J binding actually used is not supported by Maven: {}", slf4jBinding);
        logger.warn("Maven supported bindings are:");

        String ls = System.lineSeparator();

        for (Map.Entry<URL, Set<Object>> entry : supported.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append("(from ").append(entry.getKey().toExternalForm()).append(')');

            for (Object binding : entry.getValue()) {
                sb.append(ls).append("- ").append(binding);
            }

            logger.warn(sb.toString());
        }
    }
}
