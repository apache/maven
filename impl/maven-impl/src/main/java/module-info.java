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

open module org.apache.maven.impl {
    requires transitive org.apache.maven.api;
    requires transitive org.apache.maven.api.spi;
    requires org.apache.maven.api.metadata;
    requires org.apache.maven.api.xml;
    requires org.apache.maven.api.di;
    requires org.apache.maven.api.annotations;
    requires org.apache.maven.di;
    requires org.apache.maven.support;
    requires org.apache.maven.internal.xml;
    requires org.apache.maven.resolver;
    requires org.apache.maven.resolver.spi;
    requires org.apache.maven.resolver.util;
    requires org.apache.maven.resolver.impl;
    requires org.apache.maven.resolver.named.locks;
    requires org.apache.maven.resolver.connector.basic;
    requires com.ctc.wstx;
    requires org.codehaus.stax2;
    requires org.slf4j;
    requires plexus.sec.dispatcher;
    requires java.xml;

    exports org.apache.maven.api.services.model;
    exports org.apache.maven.impl to
            org.apache.maven.core,
            org.apache.maven.cling,
            org.apache.maven.testing,
            org.apache.maven.compat,
            org.apache.maven.embedder;
    exports org.apache.maven.impl.cache to
            org.apache.maven.core,
            org.apache.maven.cling,
            org.apache.maven.testing,
            org.apache.maven.compat,
            org.apache.maven.embedder;
    exports org.apache.maven.impl.di to
            org.apache.maven.core,
            org.apache.maven.cling,
            org.apache.maven.testing,
            org.apache.maven.compat,
            org.apache.maven.embedder;
    exports org.apache.maven.impl.model to
            org.apache.maven.core,
            org.apache.maven.cling,
            org.apache.maven.testing,
            org.apache.maven.compat,
            org.apache.maven.embedder;
    exports org.apache.maven.impl.model.reflection to
            org.apache.maven.core,
            org.apache.maven.cling,
            org.apache.maven.testing,
            org.apache.maven.compat,
            org.apache.maven.embedder;
    exports org.apache.maven.impl.model.rootlocator to
            org.apache.maven.core,
            org.apache.maven.cling,
            org.apache.maven.testing,
            org.apache.maven.compat,
            org.apache.maven.embedder;
    exports org.apache.maven.impl.resolver to
            org.apache.maven.core,
            org.apache.maven.cling,
            org.apache.maven.testing,
            org.apache.maven.compat,
            org.apache.maven.embedder;
    exports org.apache.maven.impl.resolver.artifact to
            org.apache.maven.core,
            org.apache.maven.cling,
            org.apache.maven.testing,
            org.apache.maven.compat,
            org.apache.maven.embedder;
    exports org.apache.maven.impl.resolver.relocation to
            org.apache.maven.core,
            org.apache.maven.cling,
            org.apache.maven.testing,
            org.apache.maven.compat,
            org.apache.maven.embedder;
    exports org.apache.maven.impl.resolver.scopes to
            org.apache.maven.core,
            org.apache.maven.cling,
            org.apache.maven.testing,
            org.apache.maven.compat,
            org.apache.maven.embedder;
    exports org.apache.maven.impl.resolver.type to
            org.apache.maven.core,
            org.apache.maven.cling,
            org.apache.maven.testing,
            org.apache.maven.compat,
            org.apache.maven.embedder;
    exports org.apache.maven.impl.resolver.validator to
            org.apache.maven.core,
            org.apache.maven.cling,
            org.apache.maven.testing,
            org.apache.maven.compat,
            org.apache.maven.embedder;
    exports org.apache.maven.impl.standalone to
            org.apache.maven.core,
            org.apache.maven.cling,
            org.apache.maven.testing,
            org.apache.maven.compat,
            org.apache.maven.embedder;
    exports org.apache.maven.impl.util to
            org.apache.maven.core,
            org.apache.maven.cling,
            org.apache.maven.testing,
            org.apache.maven.compat,
            org.apache.maven.embedder;

    uses org.apache.maven.api.services.model.RootDetector;

    provides org.apache.maven.api.model.ModelObjectProcessor with
            org.apache.maven.impl.model.DefaultModelObjectPool;
    provides org.apache.maven.api.services.model.RootDetector with
            org.apache.maven.impl.model.rootlocator.DotMvnRootDetector,
            org.apache.maven.impl.model.rootlocator.PomXmlRootDetector;
    provides org.apache.maven.api.services.model.RootLocator with
            org.apache.maven.impl.model.rootlocator.DefaultRootLocator;
}
