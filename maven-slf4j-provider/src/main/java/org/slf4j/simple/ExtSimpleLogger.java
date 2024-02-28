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
package org.slf4j.simple;

/**
 * Class inheriting SimpleLogger to work around the fact that the {@link #write(StringBuilder, Throwable)}
 * method is package private.
 */
public class ExtSimpleLogger extends SimpleLogger {

    public ExtSimpleLogger(String name) {
        super(name);
    }

    @Override
    void write(StringBuilder buf, Throwable t) {
        doWrite(buf, t);
    }

    protected void doWrite(StringBuilder buf, Throwable t) {
        super.write(buf, t);
    }
}
