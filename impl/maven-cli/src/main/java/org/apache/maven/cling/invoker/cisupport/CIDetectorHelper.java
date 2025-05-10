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
package org.apache.maven.cling.invoker.cisupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.apache.maven.api.cli.cisupport.CIInfo;

/**
 * CI detector helper: it uses service discovery to discover {@link CIDetector}s. If resulting list has more than
 * one element, it will remove the {@link GenericCIDetector} result, assuming a more specific one is also present.
 */
public final class CIDetectorHelper {
    private CIDetectorHelper() {}

    public static List<CIInfo> detectCI() {
        ArrayList<CIInfo> result = ServiceLoader.load(CIDetector.class).stream()
                .map(ServiceLoader.Provider::get)
                .map(CIDetector::detectCI)
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(ArrayList::new));

        if (result.size() > 1) {
            // remove generic
            result.removeIf(c -> GenericCIDetector.NAME.equals(c.name()));
        }
        return result;
    }
}
