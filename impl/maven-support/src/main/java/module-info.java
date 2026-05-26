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

module org.apache.maven.support {
    requires transitive org.apache.maven.api.model;
    requires transitive org.apache.maven.api.settings;
    requires transitive org.apache.maven.api.toolchain;
    requires transitive org.apache.maven.api.metadata;
    requires transitive org.apache.maven.api.plugin.descriptor;
    requires transitive org.apache.maven.internal.xml;
    requires java.xml;
    requires com.ctc.wstx;
    requires org.codehaus.stax2;

    exports org.apache.maven.model.v4;
    exports org.apache.maven.settings.v4;
    exports org.apache.maven.toolchain.v4;
    exports org.apache.maven.metadata.v4;
    exports org.apache.maven.plugin.descriptor.io;
    exports org.apache.maven.plugin.lifecycle.io;
}
