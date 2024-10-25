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
package org.apache.maven.model.profile.activation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.path.ProfileActivationFilePathInterpolator;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.codehaus.plexus.interpolation.InterpolationException;

/**
 * Determines profile activation based on the existence/absence of some file.
 * File name interpolation support is limited to <code>${project.basedir}</code>
 * system properties and user properties.
 *
 * @see ActivationFile
 * @see org.apache.maven.model.validation.DefaultModelValidator#validateRawModel
 * @deprecated use {@link org.apache.maven.api.services.ModelBuilder} instead
 */
@Named("file")
@Singleton
@Deprecated(since = "4.0.0")
public class FileProfileActivator implements ProfileActivator {

    private final ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator;

    @Inject
    public FileProfileActivator(ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator) {
        this.profileActivationFilePathInterpolator = profileActivationFilePathInterpolator;
    }

    @Override
    public boolean isActive(Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
        Activation activation = profile.getActivation();

        if (activation == null) {
            return false;
        }

        ActivationFile file = activation.getFile();

        if (file == null) {
            return false;
        }

        String path;
        boolean missing;

        if (file.getExists() != null && !file.getExists().isEmpty()) {
            path = file.getExists();
            missing = false;
        } else if (file.getMissing() != null && !file.getMissing().isEmpty()) {
            path = file.getMissing();
            missing = true;
        } else {
            return false;
        }

        try {
            path = profileActivationFilePathInterpolator.interpolate(path, context);
        } catch (InterpolationException e) {
            problems.add(new ModelProblemCollectorRequest(Severity.ERROR, Version.BASE)
                    .setMessage("Failed to interpolate file location " + path + " for profile " + profile.getId() + ": "
                            + e.getMessage())
                    .setLocation(file.getLocation(missing ? "missing" : "exists"))
                    .setException(e));
            return false;
        }

        if (path == null) {
            return false;
        }

        File f = new File(path);

        if (!f.isAbsolute()) {
            return false;
        }

        boolean fileExists = f.exists();

        return missing != fileExists;
    }

    @Override
    public boolean presentInConfig(Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
        Activation activation = profile.getActivation();

        if (activation == null) {
            return false;
        }

        ActivationFile file = activation.getFile();

        return file != null;
    }
}
