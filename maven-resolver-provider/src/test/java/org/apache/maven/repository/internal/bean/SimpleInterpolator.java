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
package org.apache.maven.repository.internal.bean;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.interpolation.StringVisitorModelInterpolator;
import org.apache.maven.model.path.PathTranslator;
import org.apache.maven.model.path.UrlNormalizer;
import org.apache.maven.model.root.RootLocator;

import static java.util.Collections.emptyList;

/**
 * just to provide an impl for tests.
 */
@Named
@Singleton
public class SimpleInterpolator extends StringVisitorModelInterpolator {
    @Inject
    public SimpleInterpolator(
            final PathTranslator pathTranslator, final UrlNormalizer urlNormalizer, final RootLocator rootLocator) {
        super(pathTranslator, urlNormalizer, rootLocator, emptyList());
    }
}
