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
package org.apache.maven.it;

/**
 * Basic Ansi support: can't use Ansi because IT is executed in separate classloader.
 */
class AnsiSupport {
    private static final String ESC = String.valueOf((char) 27) + '[';

    private static final String NORMAL = ESC + "0;39m";

    static String success(String msg) {
        return ESC + "1;32m" + msg + NORMAL;
    }

    static String warning(String msg) {
        return ESC + "1;33m" + msg + NORMAL;
    }

    static String error(String msg) {
        return ESC + "1;31m" + msg + NORMAL;
    }

    static String bold(String msg) {
        return ESC + "1m" + msg + NORMAL;
    }
}
