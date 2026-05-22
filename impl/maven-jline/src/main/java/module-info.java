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

module org.apache.maven.jline {
    requires org.apache.maven.api;
    requires org.apache.maven.api.di;
    requires org.apache.maven.api.annotations;
    requires org.jline.reader;
    requires org.jline.style;
    requires org.jline.builtins;
    requires org.jline.console;
    requires org.jline.console.ui;
    requires org.jline.terminal;
    requires org.jline.terminal.jni;
    requires org.jline.jansi.core;

    exports org.apache.maven.jline to
            org.apache.maven.logging,
            org.apache.maven.cling,
            org.apache.maven.embedder;
}
