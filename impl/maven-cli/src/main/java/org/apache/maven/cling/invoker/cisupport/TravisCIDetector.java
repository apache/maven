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

import java.util.Optional;

import org.apache.maven.api.cli.cisupport.CIInfo;

/**
 * Travis CI support.
 */
public class TravisCIDetector implements CIDetector {
    public static final String NAME = "Travis";

    private static final String TRAVIS = "TRAVIS";
    private static final String TRAVIS_DEBUG_MODE = "TRAVIS_DEBUG_MODE";

    @Override
    public Optional<CIInfo> detectCI() {
        String ciEnv = System.getenv(TRAVIS);
        if ("true".equals(ciEnv)) {
            return Optional.of(new CIInfo() {
                @Override
                public String name() {
                    return NAME;
                }

                @Override
                public boolean isVerbose() {
                    return "true".equals(System.getenv(TRAVIS_DEBUG_MODE));
                }
            });
        }
        return Optional.empty();
    }
}
