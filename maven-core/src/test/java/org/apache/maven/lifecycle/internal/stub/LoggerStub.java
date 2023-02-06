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
package org.apache.maven.lifecycle.internal.stub;

import org.codehaus.plexus.logging.Logger;

/**
 * @author Kristian Rosenvold
 */
public class LoggerStub implements Logger {
    public void debug(String s) {}

    public void debug(String s, Throwable throwable) {}

    public boolean isDebugEnabled() {
        return true;
    }

    public void info(String s) {}

    public void info(String s, Throwable throwable) {}

    public boolean isInfoEnabled() {
        return true;
    }

    public void warn(String s) {}

    public void warn(String s, Throwable throwable) {}

    public boolean isWarnEnabled() {
        return true;
    }

    public void error(String s) {}

    public void error(String s, Throwable throwable) {}

    public boolean isErrorEnabled() {
        return true;
    }

    public void fatalError(String s) {}

    public void fatalError(String s, Throwable throwable) {}

    public boolean isFatalErrorEnabled() {
        return true;
    }

    public Logger getChildLogger(String s) {
        return null;
    }

    public int getThreshold() {
        return 0;
    }

    public void setThreshold(int i) {}

    public String getName() {
        return "StubLogger";
    }
}
