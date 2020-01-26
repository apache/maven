package org.apache.maven.model.superpom;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelProcessor;

/**
 * Provides the super POM that all models implicitly inherit from.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultSuperPomProvider
    implements SuperPomProvider
{

    /**
     * The cached super POM, lazily created.
     */
    private Model superModel;

    @Inject
    private ModelProcessor modelProcessor;

    public DefaultSuperPomProvider setModelProcessor( ModelProcessor modelProcessor )
    {
        this.modelProcessor = modelProcessor;
        return this;
    }

    @Override
    public Model getSuperModel( String version )
    {
        if ( superModel == null )
        {
            String resource = "/org/apache/maven/model/pom-" + version + ".xml";

            InputStream is = getClass().getResourceAsStream( resource );

            if ( is == null )
            {
                throw new IllegalStateException( "The super POM " + resource + " was not found"
                    + ", please verify the integrity of your Maven installation" );
            }

            try
            {
                Map<String, Object> options = new HashMap<>( 2 );
                options.put( "xml:4.0.0", "xml:4.0.0" );

                String modelId = "org.apache.maven:maven-model-builder:"
                    + this.getClass().getPackage().getImplementationVersion() + ":super-pom";
                InputSource inputSource = new InputSource();
                inputSource.setModelId( modelId );
                inputSource.setLocation( getClass().getResource( resource ).toExternalForm() );
                options.put( ModelProcessor.INPUT_SOURCE, inputSource );

                superModel = modelProcessor.read( is, options );
            }
            catch ( IOException e )
            {
                throw new IllegalStateException( "The super POM " + resource + " is damaged"
                    + ", please verify the integrity of your Maven installation", e );
            }
        }

        return superModel;
    }

}
