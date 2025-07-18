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
package org.apache.maven.cling.invoker.mvnenc;

import java.util.List;
import java.util.Map;

import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.mvnenc.EncryptOptions;
import org.apache.maven.cling.invoker.LookupContext;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

@SuppressWarnings("VisibilityModifier")
public class EncryptContext extends LookupContext {
    public EncryptContext(InvokerRequest invokerRequest, EncryptOptions encryptOptions) {
        super(invokerRequest, true, encryptOptions);
    }

    public Map<String, Goal> goals;

    public List<AttributedString> header;
    public AttributedStyle style;
    public LineReader reader;

    public void addInHeader(String text) {
        addInHeader(AttributedStyle.DEFAULT, text);
    }

    public void addInHeader(AttributedStyle style, String text) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.style(style).append(text);
        header.add(asb.toAttributedString());
    }

    @Override
    public EncryptOptions options() {
        return (EncryptOptions) super.options();
    }
}
