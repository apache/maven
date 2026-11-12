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
package org.apache.maven.plugin.internal;

import java.io.File;

import org.codehaus.plexus.component.configurator.expression.TypeAwareExpressionEvaluator;

/**
 * Adapts a Plexus {@link TypeAwareExpressionEvaluator} to the internal {@link MojoExpressionEvaluator} interface,
 * isolating {@link V4MojoConfigurator} from the Plexus API.
 */
class PlexusMojoExpressionEvaluatorAdapter implements MojoExpressionEvaluator {
    private final TypeAwareExpressionEvaluator delegate;

    PlexusMojoExpressionEvaluatorAdapter(TypeAwareExpressionEvaluator evaluator) {
        this.delegate = evaluator;
    }

    @Override
    public Object evaluate(String expr, Class<?> type) throws Exception {
        return delegate.evaluate(expr, type);
    }

    @Override
    public File alignToBaseDirectory(File file) {
        return delegate.alignToBaseDirectory(file);
    }
}
