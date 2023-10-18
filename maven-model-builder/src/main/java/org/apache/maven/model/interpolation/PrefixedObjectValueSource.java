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
package org.apache.maven.model.interpolation;

import java.util.List;

import org.codehaus.plexus.interpolation.AbstractDelegatingValueSource;
import org.codehaus.plexus.interpolation.PrefixedValueSourceWrapper;
import org.codehaus.plexus.interpolation.QueryEnabledValueSource;

/**
 * Wraps an arbitrary object with an {@link ObjectBasedValueSource} instance, then
 * wraps that source with a {@link PrefixedValueSourceWrapper} instance, to which
 * this class delegates all of its calls.
 */
public class PrefixedObjectValueSource extends AbstractDelegatingValueSource implements QueryEnabledValueSource {

    /**
     * Wrap the specified root object, allowing the specified expression prefix.
     * @param prefix the prefix.
     * @param root the root of the graph.
     */
    public PrefixedObjectValueSource(String prefix, Object root) {
        super(new PrefixedValueSourceWrapper(new ObjectBasedValueSource(root), prefix));
    }

    /**
     * Wrap the specified root object, allowing the specified list of expression
     * prefixes and setting whether the {@link PrefixedValueSourceWrapper} allows
     * unprefixed expressions.
     * @param possiblePrefixes The possible prefixes.
     * @param root The root of the graph.
     * @param allowUnprefixedExpressions if we allow undefined expressions or not.
     */
    public PrefixedObjectValueSource(List<String> possiblePrefixes, Object root, boolean allowUnprefixedExpressions) {
        super(new PrefixedValueSourceWrapper(
                new ObjectBasedValueSource(root), possiblePrefixes, allowUnprefixedExpressions));
    }

    /**
     * {@inheritDoc}
     */
    public String getLastExpression() {
        return ((QueryEnabledValueSource) getDelegate()).getLastExpression();
    }
}
