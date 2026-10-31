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
package org.apache.maven.di.impl;

/**
 * Styles already escaped diagnostic text without changing its content or line boundaries.
 *
 * @since 4.1
 */
public interface BindingStyler {
    BindingStyler NONE = new BindingStyler() {};

    default String key(String text) {
        return text;
    }

    default String qualifier(String text) {
        return text;
    }

    default String scope(String text, boolean present) {
        return text;
    }

    default String priority(String text, boolean nonZero) {
        return text;
    }

    default String optionalDependency(String text) {
        return text;
    }

    default String header(String text) {
        return text;
    }
}
