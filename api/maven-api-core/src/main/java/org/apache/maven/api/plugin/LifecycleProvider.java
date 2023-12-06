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
package org.apache.maven.api.plugin;

import java.util.List;

import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.plugin.descriptor.lifecycle.Lifecycle;

/**
 * Interface that can be provided by the plugin to wire in custom lifecycles
 * leveraged using the {@link org.apache.maven.api.plugin.annotations.Execute}
 * annotation.  If a {@code META-INF/maven/lifecycle.xml} file is packaged
 * in the plugin, Maven will provide a default implementation that will parse
 * the file and return the contained lifecycle definitions.
 */
@Experimental
@Consumer
public interface LifecycleProvider {

    List<Lifecycle> getLifecycles();
}
