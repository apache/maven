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
package org.apache.maven.impl.model.profile;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.ActivationFile;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.MavenException;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.apache.maven.api.services.model.ProfileActivator;
import org.apache.maven.impl.model.DefaultModelValidator;

/**
 * Determines profile activation based on the existence/absence of some file.
 * File name interpolation support is limited to <code>${project.basedir}</code>
 * system properties and user properties.
 *
 * @see ActivationFile
 * @see DefaultModelValidator#validateRawModel
 */
@Named("file")
@Singleton
public class FileProfileActivator implements ProfileActivator {

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

        boolean hasExists = file.getExists() != null && !file.getExists().isEmpty();
        boolean hasMissing = file.getMissing() != null && !file.getMissing().isEmpty();
        if (hasExists) {
            if (hasMissing) {
                problems.add(
                        BuilderProblem.Severity.WARNING,
                        ModelProblem.Version.BASE,
                        String.format(
                                "Profile '%s' file activation conflict: Both 'missing' (%s) and 'exists' assertions are defined. "
                                        + "The 'missing' assertion will be ignored. Please remove one assertion to resolve this conflict.",
                                profile.getId(), file.getMissing()),
                        file.getLocation("missing"));
            }
            path = file.getExists();
            missing = false;
        } else if (hasMissing) {
            path = file.getMissing();
            missing = true;
        } else {
            return false;
        }

        boolean fileExists;
        try {
            fileExists = context.exists(path, false);
        } catch (MavenException e) {
            problems.add(
                    BuilderProblem.Severity.ERROR,
                    ModelProblem.Version.BASE,
                    "Failed to check file existence " + path + " for profile " + profile.getId() + ": "
                            + e.getMessage(),
                    file.getLocation(missing ? "missing" : "exists"),
                    e);
            return false;
        }

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
