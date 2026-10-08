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

module org.apache.maven.logging {
    requires org.apache.maven.api;
    requires org.apache.maven.jline;
    requires org.slf4j;

    exports org.apache.maven.logging.api;
    exports org.apache.maven.slf4j to
            org.apache.maven.cling,
            org.apache.maven.embedder,
            org.apache.maven.core;

    provides org.slf4j.spi.SLF4JServiceProvider with
            org.apache.maven.slf4j.MavenServiceProvider;
}
