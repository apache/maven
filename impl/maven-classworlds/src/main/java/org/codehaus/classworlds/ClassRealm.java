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
package org.codehaus.classworlds;

/*

Copyright 2002 (C) The Werken Company. All Rights Reserved.

Redistribution and use of this software and associated documentation
("Software"), with or without modification, are permitted provided
that the following conditions are met:

1. Redistributions of source code must retain copyright
   statements and notices.  Redistributions must also contain a
   copy of this document.

2. Redistributions in binary form must reproduce the
   above copyright notice, this list of conditions and the
   following disclaimer in the documentation and/or other
   materials provided with the distribution.

3. The name "classworlds" must not be used to endorse or promote
   products derived from this Software without prior written
   permission of The Werken Company.  For written permission,
   please contact bob@werken.com.

4. Products derived from this Software may not be called "classworlds"
   nor may "classworlds" appear in their names without prior written
   permission of The Werken Company. "classworlds" is a registered
   trademark of The Werken Company.

5. Due credit should be given to The Werken Company.
   (http://classworlds.werken.com/).

THIS SOFTWARE IS PROVIDED BY THE WERKEN COMPANY AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
THE WERKEN COMPANY OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
OF THE POSSIBILITY OF SUCH DAMAGE.

*/

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * <p>Autonomous sub-portion of a <code>ClassWorld</code>.</p>
 *
 * <p>This class most closed maps to the <code>ClassLoader</code>
 * role from Java and in facts can provide a <code>ClassLoader</code>
 * view of itself using {@link #getClassLoader}.</p>
 *
 * <p>This is a legacy interface from Classworlds 1.x, preserved for binary compatibility.
 * The compiled bytecode of {@code org.eclipse.sisu:org.eclipse.sisu.plexus} references this
 * interface directly. Removing it breaks all Maven 3+ builds.
 * New code should use {@link org.codehaus.plexus.classworlds.realm.ClassRealm}.</p>
 *
 * @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 * @author <a href="mailto:jason@zenplex.com">Jason van Zyl</a>
 * @deprecated Use {@link org.codehaus.plexus.classworlds.realm.ClassRealm} for new code.
 *             This interface must remain on the classpath for Sisu compatibility.
 */
@SuppressWarnings("rawtypes")
@Deprecated
public interface ClassRealm {
    String getId();

    ClassWorld getWorld();

    void importFrom(String realmId, String pkgName) throws NoSuchRealmException;

    void addConstituent(URL constituent);

    ClassRealm locateSourceRealm(String className);

    void setParent(ClassRealm classRealm);

    ClassRealm createChildRealm(String id) throws DuplicateRealmException;

    ClassLoader getClassLoader();

    ClassRealm getParent();

    URL[] getConstituents();

    // ----------------------------------------------------------------------
    // Classloading
    // ----------------------------------------------------------------------

    Class loadClass(String name) throws ClassNotFoundException;

    // ----------------------------------------------------------------------
    // Resource handling
    // ----------------------------------------------------------------------

    URL getResource(String name);

    Enumeration findResources(String name) throws IOException;

    InputStream getResourceAsStream(String name);

    void display();
}
