package org.apache.maven.script.marmalade.tags;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import org.codehaus.marmalade.model.AbstractMarmaladeTag;
import org.codehaus.marmalade.runtime.MarmaladeExecutionContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;
import org.codehaus.marmalade.runtime.TagExecutionException;

/**
 * @author jdcasey Created on Feb 8, 2005
 */
public abstract class AbstractBooleanValuedBodyTag
    extends AbstractMarmaladeTag
{

    protected boolean alwaysProcessChildren()
    {
        return false;
    }

    protected void doExecute( MarmaladeExecutionContext context ) throws MarmaladeExecutionException
    {
        Boolean bodyValue = (Boolean) getBody( context, Boolean.class );

        if ( bodyValue == null )
        {
            throw new TagExecutionException( getTagInfo(), "Tag body must specify a valid boolean value." );
        }

        setValue( bodyValue );
    }

    protected abstract void setValue( Boolean value ) throws MarmaladeExecutionException;

}