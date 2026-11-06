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
package org.apache.maven.its.mng12537ext;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.spi.ModelParser;
import org.apache.maven.api.spi.ModelParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal Maven 4 core extension. When loaded, it logs a marker message so
 * the IT can verify the StAX reader correctly parsed
 * {@code <extension id="g:a:v"/>} in {@code reactor.xml}.
 */
@Singleton
@Named("mng12537-logging-extension")
final class LoggingExtension implements ModelParser {

    private static final Logger logger = LoggerFactory.getLogger(LoggingExtension.class);

    @Override
    public Optional<Source> locate(Path dir) {
        logger.info("[MNG-12537] Extension loaded via id shorthand!");
        return Optional.empty();
    }

    @Override
    public Model parse(Source source, Map<String, ?> options) throws ModelParserException {
        return null;
    }
}
