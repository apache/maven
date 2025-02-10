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
package org.apache.maven.impl.model;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.OsService;
import org.apache.maven.impl.util.Os;

@Named
@Singleton
public class DefaultOsService implements OsService {
    @Override
    public String name() {
        return Os.OS_NAME;
    }

    @Override
    public String arch() {
        return Os.OS_ARCH;
    }

    @Override
    public String version() {
        return Os.OS_VERSION;
    }

    @Override
    public String family() {
        return Os.OS_FAMILY;
    }

    @Override
    public boolean isWindows() {
        return Os.IS_WINDOWS;
    }
}
