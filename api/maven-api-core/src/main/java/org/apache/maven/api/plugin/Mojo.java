package org.apache.maven.api.plugin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;

/**
 * This interface forms the contract required for <code>Mojos</code> to interact with the <code>Maven</code>
 * infrastructure.<br>
 * It features an <code>execute()</code> method, which triggers the Mojo's build-process behavior, and can throw
 * a MojoException if error conditions occur.<br>
 *
 * @since 4.0
 */
@Experimental
@FunctionalInterface @Consumer
public interface Mojo
{
    /**
     * Perform whatever build-process behavior this <code>Mojo</code> implements.<br>
     * This is the main trigger for the <code>Mojo</code> inside the <code>Maven</code> system, and allows
     * the <code>Mojo</code> to communicate errors.
     *
     * @throws MojoException if a problem occurs.
     */
    void execute();

}
