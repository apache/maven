package org.apache.maven.script.marmalade.tags;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.descriptor.Requirement;
import org.codehaus.marmalade.model.AbstractMarmaladeTag;
import org.codehaus.marmalade.runtime.MarmaladeExecutionContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class RequirementTag
    extends AbstractMarmaladeTag
{
    private String role;

    private String roleHint;

    protected void doExecute( MarmaladeExecutionContext context )
        throws MarmaladeExecutionException
    {
        processChildren( context );

        Requirement requirement = new Requirement( role, roleHint );

        ParameterTag paramTag = (ParameterTag) requireParent( ParameterTag.class );
        paramTag.setRequirement( requirement );
    }

    public void setRoleHint( String roleHint )
    {
        this.roleHint = roleHint;
    }

    public void setRole( String role )
    {
        this.role = role;
    }
}