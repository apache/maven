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
package org.apache.maven.logging.system;

/**
 * {@link System.LoggerFinder} that routes {@code java.lang.System.Logger}
 * calls through Maven's unified logging API.
 * <p>
 * This finder is discovered by the JDK via {@link java.util.ServiceLoader}
 * and must be on the system classpath (in {@code ${maven.home}/boot/}).
 *
 * @since 4.2.0
 */
public final class MavenLoggerFinder extends System.LoggerFinder {

    @Override
    public System.Logger getLogger(String name, Module module) {
        return new MavenSystemLogger(name);
    }
}
