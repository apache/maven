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

import java.util.Collection;

import org.apache.maven.api.ExtensibleEnum;
import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * An SPI interface to extend Maven with a new enum value.
 *
 * @param <T> The type of extensible enum to extend
 */
@Experimental
@Consumer
public interface ExtensibleEnumProvider<T extends ExtensibleEnum> {

    /**
     * Registers new values for the T extensible enum.
     *
     * @return a collection of T instances to register
     */
    @Nonnull
    Collection<T> provides();
}
