package org.apache.maven.xml.filter;

import org.xml.sax.XMLFilter;

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

import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Filter to adjust pom on filesystem before being processed for effective pom.
 * There should only be 1 BuildPomXMLFilter, so the same is being used by both
 * org.apache.maven.model.building.DefaultModelBuilder.transformData(InputStream) and
 * org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory.newFileTransformerManager()
 * 
 * 
 * @author Robert Scholte
 * @since 3.7.0
 */
public class BuildPomXMLFilter extends XMLFilterImpl 
{
    private XMLFilter rootFilter;
    
    BuildPomXMLFilter()
    {
        super();
    }

    BuildPomXMLFilter( XMLReader parent )
    {
        super( parent );
    }
}
