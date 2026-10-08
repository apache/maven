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
package org.apache.maven.logging.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.maven.api.services.OutputCapabilities;

/** Compatibility capture for plugins compiled against older Maven APIs. */
public final class OutputCapabilitiesData {
    public static final String REQUEST_DATA_KEY = "maven.logging.outputCapabilities";

    private OutputCapabilitiesData() {}

    public static Map<String, String> capture(OutputCapabilities capabilities) {
        Map<String, String> properties = new LinkedHashMap<>();
        capabilities.getDestination().ifPresent(destination -> properties.put("destination", destination.name()));
        capabilities.getEncoding().ifPresent(encoding -> properties.put("encoding", encoding.name()));
        capabilities.getFormat().ifPresent(format -> properties.put("format", format.name()));
        return Collections.unmodifiableMap(properties);
    }
}
