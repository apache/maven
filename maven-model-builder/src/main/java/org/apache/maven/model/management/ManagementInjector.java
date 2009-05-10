package org.apache.maven.model.management;

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

/**
 * Handles injection of plugin/dependency management into the model.
 * 
 * @author Benjamin Bentmann
 */
public interface ManagementInjector
{

    /**
     * Merges default values from the plugin and/or dependency management sections of the given model into itself.
     * 
     * @param child The model into which to merge the values specified by its management sections, must not be
     *            <code>null</code>.
     */
    void injectManagement( Model child );

}
