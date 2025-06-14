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
package org.apache.maven.model;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

public abstract class BaseObject implements Serializable, Cloneable, InputLocationTracker {
    protected transient ChildrenTracking childrenTracking;

    protected Object delegate;

    public BaseObject() {}

    public BaseObject(Object delegate, BaseObject parent) {
        this.delegate = requireNonNull(delegate, "delegate cannot be null");
        this.childrenTracking = parent != null ? parent::replace : null;
    }

    public BaseObject(Object delegate, ChildrenTracking parent) {
        this.delegate = requireNonNull(delegate, "delegate cannot be null");
        this.childrenTracking = parent;
    }

    public Object getDelegate() {
        return delegate;
    }

    public void update(Object newDelegate) {
        if (delegate != newDelegate) {
            if (childrenTracking != null) {
                childrenTracking.replace(delegate, newDelegate);
            }
            delegate = newDelegate;
            this.cleanDelegateCache();
        }
    }

    protected void cleanDelegateCache() {
        // Default implementation does nothing, subclasses can override to clean up any cached data
    }

    protected boolean replace(Object oldDelegate, Object newDelegate) {
        return false;
    }

    @FunctionalInterface
    protected interface ChildrenTracking {
        boolean replace(Object oldDelegate, Object newDelegate);
    }
}
