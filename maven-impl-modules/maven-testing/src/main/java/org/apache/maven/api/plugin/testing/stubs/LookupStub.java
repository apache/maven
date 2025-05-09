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
package org.apache.maven.api.plugin.testing.stubs;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.LookupException;

/**
 * A stub implementation of {@link Lookup} for testing purposes.
 * Provides a simple service lookup mechanism for tests.
 *
 * @since 4.0.0
 */
public class LookupStub implements Lookup {

    public static final Lookup EMPTY = new LookupStub();

    /**
     * For sub-class constructors.
     */
    protected LookupStub() {}

    @Override
    public <T> T lookup(Class<T> type) {
        throw new LookupException("This is only a stub.");
    }

    @Override
    public <T> T lookup(Class<T> type, String string) {
        throw new LookupException("This is only a stub.");
    }

    @Override
    public <T> Optional<T> lookupOptional(Class<T> type) {
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> lookupOptional(Class<T> type, String string) {
        return Optional.empty();
    }

    @Override
    public <T> List<T> lookupList(Class<T> type) {
        return List.of();
    }

    @Override
    public <T> Map<String, T> lookupMap(Class<T> type) {
        return Map.of();
    }
}
