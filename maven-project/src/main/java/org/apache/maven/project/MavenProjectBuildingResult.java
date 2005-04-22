package org.apache.maven.project;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.project.validation.ModelValidationResult;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class MavenProjectBuildingResult
{
    /** */
    private MavenProject project;

    /** */
    private ModelValidationResult modelValidationResult;

    /** */
    private boolean successful;

    /**
     * 
     * @param project
     */
    public MavenProjectBuildingResult( MavenProject project )
    {
        this.project = project;

        successful = true;
    }

    public MavenProjectBuildingResult( ModelValidationResult modelValidationResult )
    {
        this.modelValidationResult = modelValidationResult;

        successful = modelValidationResult.getMessageCount() == 0;
    }

    /**
     * @return Returns the modelValidationResult.
     */
    public ModelValidationResult getModelValidationResult()
    {
        return modelValidationResult;
    }

    /**
     * @return Returns the project.
     */
    public MavenProject getProject()
    {
        return project;
    }

    /**
     * Returns true if the project is valid.
     * 
     * @return Returns true if the project is valid.
     */
    public boolean isSuccessful()
    {
        return successful;
    }
}
