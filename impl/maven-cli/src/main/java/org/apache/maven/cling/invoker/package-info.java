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

/**
 * This package contain support (mostly abstract) classes, that implement "base" of CLIng.
 * In packages below you find actual implementations.
 *
 * Hierarchy:
 * <ul>{@link org.apache.maven.cling.invoker.LookupInvoker} is the "basis", the common ground of all Maven Tools</ul>
 * <ul>extended by {@link org.apache.maven.cling.invoker.mvn.MavenInvoker} is the "mvn Tool"</ul>
 * <ul>extended by {@link org.apache.maven.cling.invoker.mvnenc.EncryptInvoker} is the "mvnenc Tool"</ul>
 * <ul>extended by {@link org.apache.maven.cling.invoker.mvnsh.ShellInvoker} is the "mvnsh Tool"</ul>
 *
 * There is one specialization of {@link org.apache.maven.cling.invoker.mvn.MavenInvoker}, the "resident"
 * {@link org.apache.maven.cling.invoker.mvn.resident.ResidentMavenInvoker}. The difference is that this invoker
 * will on close "clean up" (tear down) the instance. All invokers are re-entrant.
 */
package org.apache.maven.cling.invoker;
