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
package org.apache.maven.its.mng8385;

import org.apache.maven.api.ProtoSession;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.spi.PropertyContributor;

import java.util.HashMap;

@Named
public class CustomPropertyContributor implements PropertyContributor {
    @Override
    public ProtoSession contribute(ProtoSession protoSession) {
        HashMap<String, String> userProperties = new HashMap<>(protoSession.getUserProperties());
        userProperties.put("mng8385", "washere!");
        return protoSession.toBuilder().withUserProperties(userProperties).build();
    }
}
