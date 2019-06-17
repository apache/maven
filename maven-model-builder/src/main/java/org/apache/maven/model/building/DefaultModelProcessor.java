package org.apache.maven.model.building;

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.locator.ModelLocator;

/**
 * DefaultModelProcessor
 */
@Named
@Singleton
public class DefaultModelProcessor
    implements ModelProcessor
{

    @Inject
    private ModelLocator locator;

    @Inject
    private ModelReader reader;

    public DefaultModelProcessor setModelLocator( ModelLocator locator )
    {
        this.locator = locator;
        return this;
    }

    public DefaultModelProcessor setModelReader( ModelReader reader )
    {
        this.reader = reader;
        return this;
    }

    @Override
    public File locatePom( File projectDirectory )
    {
        return locator.locatePom( projectDirectory );
    }

    @Override
    public Model read( File input, Map<String, ?> options )
        throws IOException
    {
        return reader.read( input, options );
    }

    @Override
    public Model read( Reader input, Map<String, ?> options )
        throws IOException
    {
        return reader.read( input, options );
    }

    @Override
    public Model read( InputStream input, Map<String, ?> options )
        throws IOException
    {
        return reader.read( input, options );
    }

}
