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
 * Invoker logging, that happens in three ways:
 * <ul>
 * <li>"early logging" when {@link org.apache.maven.cling.invoker.logging.AccumulatingLogger} just accumulates logged entries.</li>
 * <li>"early failure" when we use {@link java.lang.System#err}</li>
 * <li>"post Logging setup" when we have properly set up SLF4J logging that is used for logging</li>>
 * </ul>
 */
package org.apache.maven.cling.invoker.logging;
