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
package org.apache.maven.project.harness;

import java.io.File;
import java.util.Iterator;
import java.util.Objects;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathNotFoundException;
import org.apache.commons.jxpath.ri.JXPathContextReferenceImpl;
import org.apache.maven.project.MavenProject;

public class PomTestWrapper {

    private File pomFile;

    private JXPathContext context;

    private MavenProject mavenProject;

    static {
        JXPathContextReferenceImpl.addNodePointerFactory(new Xpp3DomPointerFactory());
    }

    public PomTestWrapper(File pomFile, MavenProject mavenProject) {
        this.mavenProject = Objects.requireNonNull(mavenProject, "mavenProject cannot be null");
        this.pomFile = pomFile;
        context = JXPathContext.newContext(mavenProject.getModel());
    }

    public PomTestWrapper(MavenProject mavenProject) {
        this.mavenProject = Objects.requireNonNull(mavenProject, "mavenProject cannot be null");
        context = JXPathContext.newContext(mavenProject.getModel());
    }

    public MavenProject getMavenProject() {
        return mavenProject;
    }

    public File getBasedir() {
        return (pomFile != null) ? pomFile.getParentFile() : null;
    }

    public void setValueOnModel(String expression, Object value) {
        context.setValue(expression, value);
    }

    /*
       public int containerCountForUri( String uri )
           throws IOException
       {
           Validate.notEmpty( uri, "uri can neither be null nor empty " );
           ModelDataSource source = new DefaultModelDataSource();
           source.init( domainModel.getModelProperties(), null );
           return source.queryFor( uri ).size();
       }
    */

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
