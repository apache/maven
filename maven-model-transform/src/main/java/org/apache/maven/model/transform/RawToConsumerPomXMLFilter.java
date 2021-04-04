package org.apache.maven.model.transform;

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
import org.xml.sax.ext.LexicalHandler;

import org.apache.maven.model.transform.sax.AbstractSAXFilter;

/**
 * XML Filter to transform pom.xml to consumer pom.
 * This often means stripping of build-specific information.
 * When extra information is required during filtering it is probably a member of the BuildPomXMLFilter
 *
 * This filter is used at one location:
 * - org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory when publishing POM files.
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
public class RawToConsumerPomXMLFilter extends AbstractSAXFilter
{
    RawToConsumerPomXMLFilter( AbstractSAXFilter filter )
    {
        super( filter );
    }

    /**
     * Don't allow overwriting parent
     */
    @Override
    public final void setParent( XMLReader parent )
    {
        if ( getParent() == null )
        {
            super.setParent( parent );
        }
    }

    @Override
    public LexicalHandler getLexicalHandler()
    {
        return ( (AbstractSAXFilter) getParent() ).getLexicalHandler();
    }
}
