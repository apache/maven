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

import org.apache.maven.model.profile.ProfileActivationContext;
import org.codehaus.plexus.interpolation.InterpolationException;

/**
 * Interpolates path for {@link org.apache.maven.model.ActivationFile}.
 *
 * @author Ravil Galeyev
 */
public interface ProfileActivationFilePathInterpolator
{
    /**
     * Interpolates given {@code path}.
     * 
     * @return absolute path or {@code null} if the input was {@code null}.
     */
    String interpolate( String path, ProfileActivationContext context ) throws InterpolationException;
}
