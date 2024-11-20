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
package org.apache.maven.its.mng7160;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.codehaus.plexus.util.Base64;
import org.codehaus.plexus.util.xml.Xpp3Dom;

@Singleton
@Named
public class Extension extends AbstractMavenLifecycleParticipant {

    @Inject
    public Extension() {
        ClassLoader ext = getClass().getClassLoader();

        testClass("xpp3", Xpp3Dom.class.getName());
        testClass("base64", Base64.class.getName());
    }

    private void testClass(String shortName, String className) {
        try {
            ClassLoader mvn = AbstractMavenLifecycleParticipant.class.getClassLoader();
            ClassLoader ext = getClass().getClassLoader();
            Class<?> clsMvn = mvn.loadClass(className);
            Class<?> clsExt = ext.loadClass(className);
            if (clsMvn != clsExt) {
                System.out.println(shortName + " -> ext");
            } else {
                System.out.println(shortName + " -> mvn");
            }
        } catch (Throwable t) {
            System.out.println(shortName + " -> " + t);
        }
    }
}
