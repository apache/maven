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
package org.apache.maven.internal.impl;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.api.Version;
import org.apache.maven.api.VersionConstraint;
import org.apache.maven.api.VersionRange;
import org.apache.maven.api.services.VersionParser;
import org.apache.maven.model.version.ModelVersionParser;

import static org.apache.maven.internal.impl.Utils.nonNull;

/**
 * A wrapper class around a resolver version that works as model version parser as well.
 */
@Named
@Singleton
public class DefaultVersionParser implements VersionParser {
    private final ModelVersionParser modelVersionParser;

    @Inject
    public DefaultVersionParser(ModelVersionParser modelVersionParser) {
        this.modelVersionParser = nonNull(modelVersionParser, "modelVersionParser");
    }

    @Override
    public Version parseVersion(String version) {
        return modelVersionParser.parseVersion(version);
    }

    @Override
    public VersionRange parseVersionRange(String range) {
        return modelVersionParser.parseVersionRange(range);
    }

    @Override
    public VersionConstraint parseVersionConstraint(String constraint) {
        return modelVersionParser.parseVersionConstraint(constraint);
    }

    @Override
    public boolean isSnapshot(String version) {
        return modelVersionParser.isSnapshot(version);
    }
}
