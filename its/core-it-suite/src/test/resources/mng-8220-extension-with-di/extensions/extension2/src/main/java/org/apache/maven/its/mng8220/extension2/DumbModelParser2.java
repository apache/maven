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
package org.apache.maven.its.mng8220.extension2;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.spi.ModelParser;
import org.apache.maven.api.spi.ModelParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("dumb")
final class DumbModelParser2 implements ModelParser {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    public DumbModelParser2() {}

    @Override
    public Optional<Source> locate(Path dir) {
        logger.warn("[MNG-8220] DumbModelParser2 Called from extension");
        return Optional.empty();
    }

    @Override
    public Model parse(Source source, Map<String, ?> options) throws ModelParserException {
        return null;
    }
}
