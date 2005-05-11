package org.apache.maven.lifecycle;

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

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class Phase
{
    private String id;

    private Set goals;

    public Phase()
    {
    }

    Phase( Phase phase )
    {
        this.id = phase.id;
        if ( phase.goals != null )
        {
            this.goals = new HashSet( phase.goals );
        }
    }

    public String getId()
    {
        return id;
    }

    public Set getGoals()
    {
        if ( goals == null )
        {
            goals = new HashSet();
        }
        return goals;
    }

    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }

        if ( !( obj instanceof Phase ) )
        {
            return false;
        }

        Phase p = (Phase) obj;

        return id.equals( p.id );
    }

    public int hashCode()
    {
        return id.hashCode();
    }
}
