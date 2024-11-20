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
package org.apache.maven.api.spi;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.api.ProtoSession;
import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.di.Named;

/**
 * Component able to contribute to Maven session user properties. This SPI component is invoked
 * very early, while there is no session created yet.
 *
 * @since 4.0.0
 */
@Experimental
@Consumer
@Named
public interface PropertyContributor extends SpiService {
    /**
     * Invoked just before session is created with a mutable map that carries collected user properties so far.
     *
     * @param userProperties The mutable user properties, never {@code null}.
     * @see #contribute(ProtoSession)
     */
    default void contribute(Map<String, String> userProperties) {}

    /**
     * Invoked just before session is created with proto session instance. The proto session contains user and
     * system properties collected so far, along with other information. This method should return altered
     * (contributions applied) user properties, not only the "new" or "added" properties!
     *
     * @param protoSession The proto session, never {@code null}.
     * @return The user properties with contributions.
     */
    default Map<String, String> contribute(ProtoSession protoSession) {
        HashMap<String, String> userProperties = new HashMap<>(protoSession.getUserProperties());
        contribute(userProperties);
        return userProperties;
    }
}
