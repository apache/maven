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

import org.apache.maven.api.cli.cisupport.CISupport;

/**
 * Generic CI support. This offers same support as Maven 3 always had. Is also special, as code will reject this
 * detector result IF there are also any other returned via discovered services.
 */
public class GenericCIDetector implements CIDetector {
    public static final String NAME = "generic";

    @Override
    public Optional<CISupport> detectCI() {
        String ciEnv = System.getenv("CI");
        if (ciEnv != null && !"false".equals(ciEnv)) {
            return Optional.of(new CISupport() {
                @Override
                public String name() {
                    return NAME;
                }

                @Override
                public String message() {
                    return "Environment variable CI equals \"true\". Disable detection by removing that variable or by setting it to any other value";
                }

                @Override
                public boolean isDebug() {
                    return false;
                }
            });
        }
        return Optional.empty();
    }
}
