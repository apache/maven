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
import org.codehaus.plexus.component.factory.marmalade.PlexusComponentTag;

/**
 * Root tag for marmalade-based mojos
 * 
 * @author jdcasey Created on Feb 8, 2005
 */
public class MojoTag
    extends AbstractMarmaladeTag
    implements PlexusComponentTag
{

    private MarmaladeTag realRoot;

    protected boolean shouldAddChild( MarmaladeTag child )
    {
        if ( child instanceof ExecuteTag )
        {
            this.realRoot = child;

            // we don't ever want THIS script to execute the ExecuteTag.
            // Instead,
            // we pull it out for later wrapping into a new script.
            return false;
        }
        else
        {
            return true;
        }
    }

    public Object getComponent()
    {
        MarmaladeScript script = new MarmaladeScript( getTagInfo().getSourceFile(), realRoot );
        return new MarmaladeMojo( script );
    }

}