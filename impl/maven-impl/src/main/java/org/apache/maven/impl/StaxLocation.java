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
package org.apache.maven.impl;

import javax.xml.stream.XMLStreamException;

import org.apache.maven.api.services.xml.Location;

public class StaxLocation implements Location {

    private final javax.xml.stream.Location location;

    public static Location getLocation(Exception e) {
        return toLocation(e instanceof XMLStreamException xe ? xe.getLocation() : null);
    }

    public static Location toLocation(javax.xml.stream.Location location) {
        return location != null ? new StaxLocation(location) : null;
    }

    public static String getMessage(Exception e) {
        String message = e.getMessage();
        if (e instanceof XMLStreamException xe && xe.getLocation() != null) {
            int idx = message.indexOf("\nMessage: ");
            if (idx >= 0) {
                return message.substring(idx + "\nMessage: ".length());
            }
        }
        return message;
    }

    public StaxLocation(javax.xml.stream.Location location) {
        this.location = location;
    }

    @Override
    public int getLineNumber() {
        return location.getLineNumber();
    }

    @Override
    public int getColumnNumber() {
        return location.getColumnNumber();
    }

    @Override
    public int getCharacterOffset() {
        return location.getCharacterOffset();
    }

    @Override
    public String getPublicId() {
        return location.getPublicId();
    }

    @Override
    public String getSystemId() {
        return location.getSystemId();
    }
}
