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
package org.apache.maven.model.path;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.model.Site;
import org.apache.maven.model.building.ModelBuildingRequest;

/**
 * Normalizes URLs to remove the ugly parent references "../" that got potentially inserted by URL adjustment during
 * model inheritance.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultModelUrlNormalizer implements ModelUrlNormalizer {

    @Inject
    private UrlNormalizer urlNormalizer;

    public DefaultModelUrlNormalizer setUrlNormalizer(UrlNormalizer urlNormalizer) {
        this.urlNormalizer = urlNormalizer;
        return this;
    }

    @Override
    public void normalize(Model model, ModelBuildingRequest request) {
        if (model == null) {
            return;
        }

        model.setUrl(normalize(model.getUrl()));

        Scm scm = model.getScm();
        if (scm != null) {
            scm.setUrl(normalize(scm.getUrl()));
            scm.setConnection(normalize(scm.getConnection()));
            scm.setDeveloperConnection(normalize(scm.getDeveloperConnection()));
        }

        DistributionManagement dist = model.getDistributionManagement();
        if (dist != null) {
            Site site = dist.getSite();
            if (site != null) {
                site.setUrl(normalize(site.getUrl()));
            }
        }
    }

    private String normalize(String url) {
        return urlNormalizer.normalize(url);
    }
}
