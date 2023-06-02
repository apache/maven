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
package org.codehaus.plexus.util.xml;

import java.io.IOException;

import org.codehaus.plexus.util.xml.pull.XmlSerializer;

/**
 *
 */
public class Xpp3Dom {

    private String root;

    public Xpp3Dom(String root) {
        this.root = root;
    }

    public void writeToSerializer(String namespace, XmlSerializer s) throws IOException {
        s.startDocument("UTF-8", Boolean.FALSE);
        s.startTag(namespace, root);
        s.endTag(namespace, root);
        s.endDocument();
    }
}
