package org.apache.maven.model.path;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Objects;

import org.apache.maven.api.model.DistributionManagement;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Scm;
import org.apache.maven.api.model.Site;
import org.apache.maven.model.building.ModelBuildingRequest;

/**
 * Normalizes URLs to remove the ugly parent references "../" that got potentially inserted by URL adjustment during
 * model inheritance.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultModelUrlNormalizer
    implements ModelUrlNormalizer
{

    private final UrlNormalizer urlNormalizer;

    @Inject
    public DefaultModelUrlNormalizer( UrlNormalizer urlNormalizer )
    {
        this.urlNormalizer = urlNormalizer;
    }

    @Override
    public Model normalize( Model model, ModelBuildingRequest request )
    {
        if ( model == null )
        {
            return null;
        }

        Model.Builder builder = Model.newBuilder( model );

        builder.url( normalize( model.getUrl() ) );

        Scm scm = model.getScm();
        if ( scm != null )
        {
            scm = Scm.newBuilder( scm )
                    .url( normalize( scm.getUrl() ) )
                    .connection( normalize( scm.getConnection() ) )
                    .developerConnection( normalize( scm.getDeveloperConnection() ) )
                    .build();
            if ( scm != model.getScm() )
            {
                builder.scm( scm );
            }
        }

        DistributionManagement dist = model.getDistributionManagement();
        if ( dist != null )
        {
            Site site = dist.getSite();
            if ( site != null )
            {
                site = Site.newBuilder( site )
                        .url( normalize( site.getUrl() ) )
                        .build();
                if ( site != dist.getSite() )
                {
                    builder.distributionManagement( dist.withSite( site ) );
                }
            }
        }

        return builder.build();
    }

    private String normalize( String url )
    {
        String newUrl = urlNormalizer.normalize( url );
        return Objects.equals( url, newUrl ) ? null : newUrl;
    }

}
