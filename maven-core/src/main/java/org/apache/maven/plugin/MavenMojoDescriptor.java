package org.apache.maven.plugin;

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

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.codehaus.plexus.component.repository.ComponentDescriptor;

import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenMojoDescriptor
    extends ComponentDescriptor
{
    private MojoDescriptor mojoDescriptor;

    public MavenMojoDescriptor( MojoDescriptor pluginDescriptor )
    {
        this.mojoDescriptor = pluginDescriptor;
    }

    public MojoDescriptor getMojoDescriptor()
    {
        return mojoDescriptor;
    }

    public String getId()
    {
        return mojoDescriptor.getId();
    }

    public String getImplementation()
    {
        return mojoDescriptor.getImplementation();
    }

    public List getPrereqs()
    {
        return mojoDescriptor.getPrereqs();
    }

    public String getRole()
    {
        return "org.apache.maven.plugin.Plugin";
    }

    public String getRoleHint()
    {
        return getId();
    }

    public String getComponentType()
    {
        return "maven-plugin";
    }
}
