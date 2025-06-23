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
package org.apache.maven.impl.cache;

import java.util.Objects;

import org.apache.maven.api.services.Request;
import org.apache.maven.api.services.RequestTrace;

/**
 * A cache selector that matches requests based on their type and optional parent request type.
 *
 * Supports CSS-like selectors:
 * - "RequestType" matches any request of that type
 * - "ParentType RequestType" matches RequestType with ParentType as parent
 * - "ParentType *" matches any request with ParentType as parent
 * - "* RequestType" matches RequestType with any parent (equivalent to just "RequestType")
 *
 * @param parentRequestType
 * @param requestType
 * @param config
 */
public record CacheSelector(String parentRequestType, String requestType, PartialCacheConfig config) {

    public CacheSelector {
        Objects.requireNonNull(requestType, "requestType cannot be null");
        Objects.requireNonNull(config, "config cannot be null");
    }

    /**
     * Creates a selector that matches any request of the specified type.
     */
    public static CacheSelector forRequestType(String requestType, PartialCacheConfig config) {
        return new CacheSelector(null, requestType, config);
    }

    /**
     * Creates a selector that matches requests with a specific parent type.
     */
    public static CacheSelector forParentAndRequestType(
            String parentRequestType, String requestType, PartialCacheConfig config) {
        return new CacheSelector(parentRequestType, requestType, config);
    }

    /**
     * Checks if this selector matches the given request.
     *
     * @param req the request to match
     * @return true if this selector matches the request
     */
    public boolean matches(Request<?> req) {
        // Check if request type matches any of the implemented interfaces
        if (!"*".equals(requestType) && !matchesAnyInterface(req.getClass(), requestType)) {
            return false;
        }

        // If no parent type specified, it matches
        if (parentRequestType == null) {
            return true;
        }

        // Check parent request type
        if (!matchesParentRequestType(req, parentRequestType)) {
            return false;
        }

        return true;
    }

    /**
     * Checks if a class or any of its implemented interfaces matches the given type name.
     *
     * @param clazz the class to check
     * @param typeName the type name to match against
     * @return true if the class or any of its interfaces matches the type name
     */
    private boolean matchesAnyInterface(Class<?> clazz, String typeName) {
        // Check the class itself first
        if (typeName.equals(getShortClassName(clazz))) {
            return true;
        }

        // Check all implemented interfaces
        for (Class<?> iface : clazz.getInterfaces()) {
            if (typeName.equals(getShortClassName(iface))) {
                return true;
            }
            // Recursively check parent interfaces
            if (matchesAnyInterface(iface, typeName)) {
                return true;
            }
        }

        // Check superclass if it exists
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return matchesAnyInterface(superClass, typeName);
        }

        return false;
    }

    /**
     * Checks if the parent request type matches the given selector pattern.
     *
     * @param req the request to check
     * @param parentRequestType the parent request type pattern to match
     * @return true if the parent matches the pattern
     */
    private boolean matchesParentRequestType(Request<?> req, String parentRequestType) {
        if ("*".equals(parentRequestType)) {
            return true;
        }

        RequestTrace trace = req.getTrace();
        if (trace == null || trace.parent() == null) {
            return false;
        }

        Object parentData = trace.parent().data();
        if (!(parentData instanceof Request<?> parentReq)) {
            return false;
        }

        // Check if parent request matches any interface with the given name
        return matchesAnyInterface(parentReq.getClass(), parentRequestType);
    }

    /**
     * Gets the short class name (without package) of a class.
     */
    private String getShortClassName(Class<?> clazz) {
        String name = clazz.getSimpleName();
        return name.isEmpty() ? clazz.getName() : name;
    }


    @Override
    public String toString() {
        if (parentRequestType == null) {
            return requestType;
        }
        return parentRequestType + " " + requestType;
    }
}
