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
package org.codehaus.plexus.classworlds;

/*
 * Copyright 2001-2006 Codehaus Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 */
public class UrlUtils {
    public static String normalizeUrlPath(String name) {
        if (name.startsWith("/")) {
            name = name.substring(1);
        }

        // Looking for org/codehaus/werkflow/personality/basic/../common/core-idioms.xml
        //                                               |    i  |
        //                                               +-------+ remove
        //
        int i = name.indexOf("/..");

        // Can't be at the beginning because we have no root to refer to so
        // we start at 1.
        if (i > 0) {
            int j = name.lastIndexOf("/", i - 1);

            if (j >= 0) {
                name = name.substring(0, j) + name.substring(i + 3);
            }
        }

        return name;
    }

    public static Set<URL> getURLs(URLClassLoader loader) {
        return new HashSet<>(Arrays.asList(loader.getURLs()));
    }
}
