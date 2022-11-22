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
package org.apache.maven.internal.impl;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.LookupException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

@Named
@Singleton
public class DefaultLookup implements Lookup {

    private final PlexusContainer container;

    @Inject
    public DefaultLookup(PlexusContainer container) {
        this.container = container;
    }

    @Override
    public <T> T lookup(Class<T> type) {
        try {
            return container.lookup(type);
        } catch (ComponentLookupException e) {
            throw new LookupException(e);
        }
    }

    @Override
    public <T> T lookup(Class<T> type, String name) {
        try {
            return container.lookup(type, name);
        } catch (ComponentLookupException e) {
            throw new LookupException(e);
        }
    }

    @Override
    public <T> List<T> lookupList(Class<T> type) {
        try {
            return container.lookupList(type);
        } catch (ComponentLookupException e) {
            throw new LookupException(e);
        }
    }

    @Override
    public <T> Map<String, T> lookupMap(Class<T> type) {
        try {
            return container.lookupMap(type);
        } catch (ComponentLookupException e) {
            throw new LookupException(e);
        }
    }
}
