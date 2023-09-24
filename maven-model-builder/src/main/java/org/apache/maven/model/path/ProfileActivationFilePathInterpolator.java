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

import java.io.File;

import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.codehaus.plexus.interpolation.AbstractValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;

/**
 * Finds an absolute path for {@link ActivationFile#getExists()} or {@link ActivationFile#getMissing()}
 *
 * @author Ravil Galeyev
 */
@Named
@Singleton
public class ProfileActivationFilePathInterpolator {

    @Inject
    private PathTranslator pathTranslator;

    public ProfileActivationFilePathInterpolator setPathTranslator(PathTranslator pathTranslator) {
        this.pathTranslator = pathTranslator;
        return this;
    }

    /**
     * Interpolates given {@code path}.
     *
     * @return absolute path or {@code null} if the input was {@code null}
     */
    public String interpolate(String path, ProfileActivationContext context) throws InterpolationException {
        if (path == null) {
            return null;
        }

        RegexBasedInterpolator interpolator = new RegexBasedInterpolator();

        final File basedir = context.getProjectDirectory();

        if (basedir != null) {
            interpolator.addValueSource(new AbstractValueSource(false) {
                @Override
                public Object getValue(String expression) {
                    if ("basedir".equals(expression) || "project.basedir".equals(expression)) {
                        return basedir.getAbsolutePath();
                    }
                    return null;
                }
            });
        } else if (path.contains("${basedir}")) {
            return null;
        }

        interpolator.addValueSource(new MapBasedValueSource(context.getProjectProperties()));

        interpolator.addValueSource(new MapBasedValueSource(context.getUserProperties()));

        interpolator.addValueSource(new MapBasedValueSource(context.getSystemProperties()));

        String absolutePath = interpolator.interpolate(path, "");

        return pathTranslator.alignToBaseDirectory(absolutePath, basedir);
    }
}
