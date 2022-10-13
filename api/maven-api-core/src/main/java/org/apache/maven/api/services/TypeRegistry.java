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
package org.apache.maven.api.services;

import org.apache.maven.api.Service;
import org.apache.maven.api.Type;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Access to {@link Type} registry.
 *
 * @since 4.0
 */
@Experimental
public interface TypeRegistry
    extends Service
{

    /**
     * Obtain the {@link Type} from the specified {@code id}. If no type is known for {@code id}, the registry will
     * create a custom {@code Type} for it.
     *
     * @param id the id of the type to retrieve
     * @return the type
     */
    @Nonnull
    Type getType( @Nonnull
    String id );

}
