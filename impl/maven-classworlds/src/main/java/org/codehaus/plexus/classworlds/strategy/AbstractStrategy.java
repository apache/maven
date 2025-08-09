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
package org.codehaus.plexus.classworlds.strategy;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;

import org.codehaus.plexus.classworlds.UrlUtils;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

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

/**
 * @author Jason van Zyl
 */
public abstract class AbstractStrategy implements Strategy {

    protected ClassRealm realm;

    public AbstractStrategy(ClassRealm realm) {
        this.realm = realm;
    }

    protected String getNormalizedResource(String name) {
        return UrlUtils.normalizeUrlPath(name);
    }

    protected Enumeration<URL> combineResources(Enumeration<URL> en1, Enumeration<URL> en2, Enumeration<URL> en3) {
        Collection<URL> urls = new LinkedHashSet<>();

        addAll(urls, en1);
        addAll(urls, en2);
        addAll(urls, en3);

        return Collections.enumeration(urls);
    }

    private void addAll(Collection<URL> target, Enumeration<URL> en) {
        if (en != null) {
            while (en.hasMoreElements()) {
                target.add(en.nextElement());
            }
        }
    }

    public ClassRealm getRealm() {
        return realm;
    }
}
