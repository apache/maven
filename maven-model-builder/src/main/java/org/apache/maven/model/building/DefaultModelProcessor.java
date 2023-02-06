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
package org.apache.maven.model.building;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.locator.ModelLocator;
import org.eclipse.sisu.Typed;

/**
 *
 * Note: uses @Typed to limit the types it is available for injection to just ModelProcessor.
 *
 * This is because the ModelProcessor interface extends ModelLocator and ModelReader. If we
 * made this component available under all its interfaces then it could end up being injected
 * into itself leading to a stack overflow.
 *
 * A side-effect of using @Typed is that it translates to explicit bindings in the container.
 * So instead of binding the component under a 'wildcard' key it is now bound with an explicit
 * key. Since this is a default component this will be a plain binding of ModelProcessor to
 * this implementation type, ie. no hint/name.
 *
 * This leads to a second side-effect in that any @Inject request for just ModelProcessor in
 * the same injector is immediately matched to this explicit binding, which means extensions
 * cannot override this binding. This is because the lookup is always short-circuited in this
 * specific situation (plain @Inject request, and plain explicit binding for the same type.)
 *
 * The simplest solution is to use a custom @Named here so it isn't bound under the plain key.
 * This is only necessary for default components using @Typed that want to support overriding.
 *
 * As a non-default component this now gets a negative priority relative to other implementations
 * of the same interface. Since we want to allow overriding this doesn't matter in this case.
 * (if it did we could add @Priority of 0 to match the priority given to default components.)
 */
@Named("core-default")
@Singleton
@Typed(ModelProcessor.class)
public class DefaultModelProcessor implements ModelProcessor {

    @Inject
    private ModelLocator locator;

    @Inject
    private ModelReader reader;

    public DefaultModelProcessor setModelLocator(ModelLocator locator) {
        this.locator = locator;
        return this;
    }

    public DefaultModelProcessor setModelReader(ModelReader reader) {
        this.reader = reader;
        return this;
    }

    @Override
    public File locatePom(File projectDirectory) {
        return locator.locatePom(projectDirectory);
    }

    @Override
    public Model read(File input, Map<String, ?> options) throws IOException {
        return reader.read(input, options);
    }

    @Override
    public Model read(Reader input, Map<String, ?> options) throws IOException {
        return reader.read(input, options);
    }

    @Override
    public Model read(InputStream input, Map<String, ?> options) throws IOException {
        return reader.read(input, options);
    }
}
