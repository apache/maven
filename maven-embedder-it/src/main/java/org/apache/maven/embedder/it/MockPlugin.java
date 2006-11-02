package org.apache.maven.embedder.it;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.model.Model;

import java.io.File;/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * A mock tool plugin that uses the Maven Embedder API
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MockPlugin
{
    private MavenEmbedder maven;

    public MockPlugin()
        throws Exception
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        maven = new MavenEmbedder();

        maven.setClassLoader( classLoader );

        maven.start();
    }

    public Model readModel( File model )
        throws Exception
    {
        return maven.readModel( model );
    }
}
