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
import org.codehaus.marmalade.model.MarmaladeTag;
import org.codehaus.marmalade.runtime.MarmaladeExecutionContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;

import java.util.Iterator;

/**
 * Aggregator tag for the actual meat of the mojo. Simply a pass-through
 * surrogate root tag for the eventual component-script (@see MojoTag).
 * 
 * @author jdcasey Created on Feb 8, 2005
 */
public class ExecuteTag
    extends AbstractMarmaladeTag
{

    protected void doExecute( MarmaladeExecutionContext context ) throws MarmaladeExecutionException
    {
        for ( Iterator it = children().iterator(); it.hasNext(); )
        {
            MarmaladeTag child = (MarmaladeTag) it.next();
        }
    }

}