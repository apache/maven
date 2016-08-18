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
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.versioning.ModelVersions;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Provides the super POM that all models implicitly inherit from.
 *
 * @author Benjamin Bentmann
 */
@Component( role = SuperPomProvider.class )
public class DefaultSuperPomProvider
    implements SuperPomProvider
{

    /**
     * Cached super POMs, lazily created.
     */
    private volatile Reference<Map<String, Model>> modelCache =
        new SoftReference<Map<String, Model>>( new HashMap<String, Model>() );

    @Requirement
    private ModelProcessor modelProcessor;

    public DefaultSuperPomProvider setModelProcessor( ModelProcessor modelProcessor )
    {
        this.modelProcessor = modelProcessor;
        return this;
    }

    @Override
    public Model getSuperModel( final String version )
    {
        // [MNG-666] need to be able to operate on a Maven 1 repository
        //    Instead of throwing an exception if version == null, we return a version "4.0.0" super pom.
        final String effectiveVersion = version == null ? ModelVersions.V4_0_0 : version;
        final String resource = "/org/apache/maven/model/pom-" + effectiveVersion + ".xml";

        try
        {
            Map<String, Model> superPoms = this.modelCache.get();
            if ( superPoms == null )
            {
                superPoms = new HashMap<>();
                this.modelCache = new SoftReference<>( superPoms );
            }

            Model superModel = superPoms.get( effectiveVersion );

            if ( superModel == null )
            {
                InputStream is = getClass().getResourceAsStream( resource );

                if ( is == null )
                {
                    throw new IllegalStateException( "The super POM " + resource + " was not found"
                                                         + ", please verify the integrity of your Maven installation" );

                }

                superModel = modelProcessor.read( is, null );
                superPoms.put( effectiveVersion, superModel );
            }

            return superModel;
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( "The super POM " + resource + " is damaged"
                                                 + ", please verify the integrity of your Maven installation", e );
        }
    }

}
