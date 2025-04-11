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

import org.apache.maven.api.cli.cisupport.CISupport;

/**
 * CI detector helper: it uses service discovery (one is always present: {@link GenericCIDetector}) to detect CI, if
 * present and returns a list of detections. If result has 2 or more results, it will return the generic result before.
 */
public final class CIDetectorHelper {
    private CIDetectorHelper() {}

    public static List<CISupport> detectCI() {
        List<CIDetector> detectors = ServiceLoader.load(CIDetector.class).stream()
                .map(ServiceLoader.Provider::get)
                .toList();

        ArrayList<CISupport> result = new ArrayList<>();
        for (CIDetector detector : detectors) {
            Optional<CISupport> detected = detector.detectCI();
            detected.ifPresent(result::add);
        }
        if (result.size() > 1) {
            // remove generic
            result.removeIf(c -> GenericCIDetector.NAME.equals(c.name()));
        }
        return result;
    }
}
