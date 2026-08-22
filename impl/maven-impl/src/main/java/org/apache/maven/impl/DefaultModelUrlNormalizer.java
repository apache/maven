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
package org.apache.maven.impl;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.DistributionManagement;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Scm;
import org.apache.maven.api.model.Site;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.model.ModelUrlNormalizer;
import org.apache.maven.api.services.model.UrlNormalizer;

/**
 * Normalizes URLs to remove the ugly parent references "../" that got potentially inserted by URL adjustment during
 * model inheritance.
 *
 */
@Named
@Singleton
public class DefaultModelUrlNormalizer implements ModelUrlNormalizer {

    private final UrlNormalizer urlNormalizer;

    @Inject
    public DefaultModelUrlNormalizer(UrlNormalizer urlNormalizer) {
        this.urlNormalizer = urlNormalizer;
    }

    @Override
    public Model normalize(Model model, ModelBuilderRequest request) {
        if (model == null) {
            return null;
        }

        Model.Builder builder = Model.newBuilder(model);
        normalizeBuilder(builder);
        return builder.build();
    }

    @Override
    public void normalize(Model.Builder builder, ModelBuilderRequest request) {
        normalizeBuilder(builder);
    }

    private void normalizeBuilder(Model.Builder builder) {
        builder.url(normalize(builder.getUrl()));

        Scm scm = builder.getScm();
        if (scm != null) {
            builder.getModifiableScm()
                    .url(normalize(scm.getUrl()))
                    .connection(normalize(scm.getConnection()))
                    .developerConnection(normalize(scm.getDeveloperConnection()));
        }

        DistributionManagement dist = builder.getDistributionManagement();
        if (dist != null) {
            Site site = dist.getSite();
            if (site != null) {
                builder.getModifiableDistributionManagement()
                        .getModifiableSite()
                        .url(normalize(site.getUrl()));
            }
        }
    }

    private String normalize(String url) {
        return urlNormalizer.normalize(url);
    }
}
