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
package org.apache.maven.internal;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

/**
 * Access to core {@link ClassRealm}.
 *
 * @since 4.0.0
 */
@Experimental
public interface CoreRealm {

    /**
     * Obtain the {@link ClassRealm} used for Maven Core.
     *
     * @return the class realm of core.
     */
    @Nonnull
    ClassRealm getRealm();

    /**
     * Shorthand method to obtain the {@link ClassWorld} used for Maven Core.
     *
     * @return the class world in use.
     */
    @Nonnull
    default ClassWorld getClassWorld() {
        return getRealm().getWorld();
    }
}
