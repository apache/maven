package org.apache.maven.tools.repoclean.validate;

import org.apache.maven.model.v4_0_0.Model;
import org.apache.maven.tools.repoclean.report.Reporter;
import org.codehaus.plexus.util.StringUtils;

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

/**
 * @author jdcasey
 */
public class V4ModelIndependenceValidator
{

    public static final String ROLE = V4ModelIndependenceValidator.class.getName();

    public boolean validate( Model model, Reporter reporter, boolean warnOnly )
    {
        boolean isValid = true;

        if ( StringUtils.isEmpty( model.getModelVersion() ) )
        {
            problem( "Model-version declaration is missing in resulting v4 model.", reporter, warnOnly );
            isValid = false;
        }

        if ( StringUtils.isEmpty( model.getGroupId() ) )
        {
            problem( "Group ID is missing in resulting v4 model.", reporter, warnOnly );
            isValid = false;
        }

        if ( StringUtils.isEmpty( model.getArtifactId() ) )
        {
            problem( "Artifact ID is missing in resulting v4 model.", reporter, warnOnly );
            isValid = false;
        }

        if ( StringUtils.isEmpty( model.getVersion() ) )
        {
            problem( "Version is missing in resulting v4 model.", reporter, warnOnly );
            isValid = false;
        }

        return isValid;
    }

    private void problem( String message, Reporter reporter, boolean warnOnly )
    {
        if ( warnOnly )
        {
            reporter.warn( message );
        }
        else
        {
            reporter.error( message );
        }
    }

}