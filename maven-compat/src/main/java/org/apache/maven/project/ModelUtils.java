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
package org.apache.maven.project;

import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;

/** @deprecated */
@Deprecated
public final class ModelUtils {

    /**
     * This should be the resulting ordering of plugins after merging:
     * <p>
     * Given:
     * <pre>
     * parent: X -&gt; A -&gt; B -&gt; D -&gt; E
     * child: Y -&gt; A -&gt; C -&gt; D -&gt; F
     * </pre>
     * Result:
     * <pre>
     * X -&gt; Y -&gt; A -&gt; B -&gt; C -&gt; D -&gt; E -&gt; F
     * </pre>
     */
    public static void mergePluginLists(
            PluginContainer childContainer, PluginContainer parentContainer, boolean handleAsInheritance) {
        throw new UnsupportedOperationException();
    }

    public static List<Plugin> orderAfterMerge(
            List<Plugin> merged, List<Plugin> highPrioritySource, List<Plugin> lowPrioritySource) {
        throw new UnsupportedOperationException();
    }

    public static void mergePluginDefinitions(Plugin child, Plugin parent, boolean handleAsInheritance) {
        throw new UnsupportedOperationException();
    }

    public static void mergeFilterLists(List<String> childFilters, List<String> parentFilters) {
        throw new UnsupportedOperationException();
    }
}
