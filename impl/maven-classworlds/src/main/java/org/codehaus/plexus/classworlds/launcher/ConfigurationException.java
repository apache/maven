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

/**
 * Indicates an error during <code>Launcher</code> configuration.
 *
 * @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 */
public class ConfigurationException extends Exception {
    /**
     * Construct.
     *
     * @param msg The message.
     */
    public ConfigurationException(String msg) {
        super(msg);
    }

    /**
     * Construct.
     *
     * @param msg    The message.
     * @param lineNo The number of configuraton line where the problem occured.
     * @param line   The configuration line where the problem occured.
     */
    public ConfigurationException(String msg, int lineNo, String line) {
        super(msg + " (" + lineNo + "): " + line);
    }

    protected ConfigurationException(Exception cause) {
        super(cause);
    }
}
