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
package org.apache.maven.utils.dag;

import java.util.Iterator;
import java.util.List;

public class CycleDetectedException extends Exception {
    private List<String> cycle;

    public CycleDetectedException(final String message, final List<String> cycle) {
        super(message);

        this.cycle = cycle;
    }

    public List<String> getCycle() {
        return cycle;
    }

    public String cycleToString() {
        final StringBuilder buffer = new StringBuilder();

        for (Iterator<String> iterator = cycle.iterator(); iterator.hasNext(); ) {
            buffer.append(iterator.next());

            if (iterator.hasNext()) {
                buffer.append(" --> ");
            }
        }
        return buffer.toString();
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " " + cycleToString();
    }
}
