package org.apache.maven.project.validation;

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

import org.apache.maven.model.Model;

/**
 * Checks the model for missing or invalid values.
 * 
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public interface ModelValidator
{

    String ROLE = ModelValidator.class.getName();

    ModelValidationResult validate( Model model );

    /**
     * Checks the specified model for missing or invalid values.
     * 
     * @param model The model to validate, must not be {@code null}.
     * @param lenient A flag whether validation should be lenient instead of strict. For building of projects, strict
     *            validation should be used to ensure proper building. For the mere retrievel of dependencies during
     *            artifact resolution, lenient validation should be used to account for models of poor quality.
     * @return The result of the validation, never {@code null}.
     */
    ModelValidationResult validate( Model model, boolean lenient );

}
