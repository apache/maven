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
package org.apache.maven.lifecycle;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathNotFoundException;
import org.apache.commons.jxpath.ri.JXPathContextReferenceImpl;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.harness.Xpp3DomPointerFactory;

public class MojoExecutionXPathContainer {
    private JXPathContext context;

    static {
        JXPathContextReferenceImpl.addNodePointerFactory(new Xpp3DomPointerFactory());
    }

    public MojoExecutionXPathContainer(MojoExecution mojoExecution) throws IOException {
        context = JXPathContext.newContext(mojoExecution);
    }

    public Iterator<?> getIteratorForXPathExpression(String expression) {
        return context.iterate(expression);
    }

    public boolean containsXPathExpression(String expression) {
        return context.getValue(expression) != null;
    }

    public Object getValue(String expression) {
        try {
            return context.getValue(expression);
        } catch (JXPathNotFoundException e) {
            return null;
        }
    }

    public boolean xPathExpressionEqualsValue(String expression, String value) {
        return context.getValue(expression) != null
                && context.getValue(expression).equals(value);
    }
}
