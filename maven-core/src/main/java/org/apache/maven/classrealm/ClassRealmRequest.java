package org.apache.maven.classrealm;

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

import java.util.List;

/**
 * Describes the requirements for a new class realm.
 * 
 * @author Benjamin Bentmann
 */
public interface ClassRealmRequest
{

    enum RealmType
    {
        Project, Extension, Plugin,
    }

    /**
     * Gets the type of the class realm.
     * 
     * @return The type of the class realm, never {@code null}.
     */
    RealmType getType();

    /**
     * Gets the parent class realm (if any).
     * 
     * @return The parent class realm or {@code null} if using the Maven core realm as parent.
     */
    ClassLoader getParent();

    /**
     * Gets the packages/types to import from the parent realm
     * 
     * @return The modifiable list of packages/types to import from the parent realm, never {@code null}.
     */
    List<String> getImports();

    /**
     * Gets the constituents for the class realm.
     * 
     * @return The modifiable list of constituents for the class realm, never {@code null}.
     */
    List<ClassRealmConstituent> getConstituents();

}
