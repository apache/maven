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
package org.apache.maven.plugin.testing;

import junit.framework.TestCase;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;

public class TestSilentLog extends TestCase {

    public void testLog() {
        Log log = new SilentLog();
        String text = new String("Text");
        Throwable e = new RuntimeException();
        log.debug(text);
        log.debug(text, e);
        log.debug(e);
        log.info(text);
        log.info(text, e);
        log.info(e);
        log.warn(text);
        log.warn(text, e);
        log.warn(e);
        log.error(text);
        log.error(text, e);
        log.error(e);
        log.isDebugEnabled();
        log.isErrorEnabled();
        log.isWarnEnabled();
        log.isInfoEnabled();
    }

    public void testLogger() {
        Logger log = new SilentLog();
        String text = new String("Text");
        Throwable e = new RuntimeException();

        log.debug(text);
        log.debug(text, e);
        log.error(text);
        log.error(text, e);
        log.warn(text);
        log.warn(text, e);
        log.info(text);
        log.info(text, e);

        log.fatalError(text);
        log.fatalError(text, e);
        log.getChildLogger(text);
        log.getName();
        log.getThreshold();
        log.isDebugEnabled();
        log.isErrorEnabled();
        log.isFatalErrorEnabled();
        log.isInfoEnabled();
        log.isWarnEnabled();
    }
}
