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
package org.apache.maven.model.merge;

import java.util.Map;
import java.util.Objects;

/**
 * The domain-specific model merger for the Maven POM, overriding generic code from parent class when necessary with
 * more adapted algorithms.
 *
 */
public class MavenModelMerger extends org.apache.maven.internal.impl.model.MavenModelMerger {

    /**
     * Merges the specified source object into the given target object.
     *
     * @param target The target object whose existing contents should be merged with the source, must not be
     *            <code>null</code>.
     * @param source The (read-only) source object that should be merged into the target object, may be
     *            <code>null</code>.
     * @param sourceDominant A flag indicating whether either the target object or the source object provides the
     *            dominant data.
     * @param hints A set of key-value pairs that customized merger implementations can use to carry domain-specific
     *            information along, may be <code>null</code>.
     */
    public void merge(
            org.apache.maven.model.Model target,
            org.apache.maven.model.Model source,
            boolean sourceDominant,
            Map<?, ?> hints) {
        Objects.requireNonNull(target, "target cannot be null");
        if (source == null) {
            return;
        }
        target.update(merge(target.getDelegate(), source.getDelegate(), sourceDominant, hints));
    }
}
