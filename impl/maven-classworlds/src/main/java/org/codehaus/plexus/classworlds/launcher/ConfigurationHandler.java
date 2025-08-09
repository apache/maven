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
package org.codehaus.plexus.classworlds.launcher;

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

import java.io.File;
import java.net.URL;

import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;

/**
 * Receive notification of the logical content of launcher configuration, independently from parsing.
 *
 * @author Igor Fedorenko
 */
public interface ConfigurationHandler {

    /**
     * Define the main class name
     * @param mainClassName the main class name
     * @param mainRealmName the main realm from which the main class is loaded
     */
    void setAppMain(String mainClassName, String mainRealmName);

    /**
     * Define a new realm
     * @param realmName the new realm name
     * @throws DuplicateRealmException when realm with name already exists
     */
    void addRealm(String realmName) throws DuplicateRealmException;

    /**
     * Add an import specification from a realm
     * @param realmName the realm name
     * @param importSpec the import specification
     * @throws NoSuchRealmException if realm doesn't exist
     */
    void addImportFrom(String realmName, String importSpec) throws NoSuchRealmException;

    /**
     * Add a file to the realm
     * @param file the file to load content from
     */
    void addLoadFile(File file);

    /**
     * Add an URL to the realm
     * @param url the url to load content from
     */
    void addLoadURL(URL url);
}
