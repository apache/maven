package org.apache.maven.model.inheritance;

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

import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBuildingRequest;

/**
 * Handles inheritance of model values.
 * 
 * @author Benjamin Bentmann
 */
public interface InheritanceAssembler
{

    /**
     * Merges values from the specified parent model into the given child model.
     * 
     * @param child The child model into which to merge the values inherited from the parent, must not be
     *            <code>null</code>.
     * @param parent The (read-only) parent model from which to inherit the values, may be <code>null</code>.
     * @param request The model building request that holds further settings, must not be {@code null}.
     */
    void assembleModelInheritance( Model child, Model parent, ModelBuildingRequest request );

}
