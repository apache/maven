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

import org.apache.maven.script.marmalade.MarmaladeMojo;
import org.codehaus.marmalade.model.AbstractMarmaladeTag;
import org.codehaus.marmalade.model.MarmaladeScript;
import org.codehaus.marmalade.model.MarmaladeTag;
import org.codehaus.marmalade.runtime.MarmaladeExecutionContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;
import org.codehaus.plexus.component.factory.marmalade.PlexusComponentTag;

import java.util.Iterator;

/**
 * Root tag for marmalade-based mojos
 * 
 * @author jdcasey Created on Feb 8, 2005
 */
public class MojoTag
    extends AbstractMarmaladeTag
    implements PlexusComponentTag
{

    protected boolean alwaysProcessChildren()
    {
        return false;
    }
    
    protected void doExecute( MarmaladeExecutionContext context ) throws MarmaladeExecutionException
    {
        for ( Iterator it = children().iterator(); it.hasNext(); )
        {
            MarmaladeTag child = (MarmaladeTag) it.next();
            if(!(child instanceof ExecuteTag))
            {
                child.execute(context);
            }
        }
    }
    
    public Object getComponent()
    {
        MarmaladeTag realRoot = null;
        for ( Iterator it = children().iterator(); it.hasNext(); )
        {
            MarmaladeTag child = (MarmaladeTag) it.next();
            if(child instanceof ExecuteTag)
            {
                realRoot = child;
                break;
            }
        }
        
        if(realRoot == null)
        {
            throw new IllegalStateException("Mojo scripts MUST have a <execute> tag.");
        }
        
        MarmaladeScript script = new MarmaladeScript( getTagInfo().getSourceFile(), realRoot );
        return new MarmaladeMojo( script );
    }

}