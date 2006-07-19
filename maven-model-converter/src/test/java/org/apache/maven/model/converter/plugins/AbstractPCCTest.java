package org.apache.maven.model.converter.plugins;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.Properties;

/**
 * @author Dennis Lundberg
 * @version $Id$
 */
public class AbstractPCCTest extends PlexusTestCase
{
    protected Xpp3Dom configuration;
    protected AbstractPluginConfigurationConverter pluginConfigurationConverter;
    protected Properties projectProperties;
    protected org.apache.maven.model.v3_0_0.Model v3Model;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        configuration = new Xpp3Dom( "configuration" );

        projectProperties = new Properties();

        v3Model = new org.apache.maven.model.v3_0_0.Model();
        v3Model.setBuild( new org.apache.maven.model.v3_0_0.Build() );
    }
}
