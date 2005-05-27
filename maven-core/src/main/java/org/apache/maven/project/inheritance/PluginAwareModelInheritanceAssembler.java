package org.apache.maven.project.inheritance;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.ModelUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

public class PluginAwareModelInheritanceAssembler
    extends DefaultModelInheritanceAssembler
{
    
//    private PluginManager pluginManager;
//
//    public void assembleModelInheritance( Model child, Model parent )
//    {
//        super.assembleModelInheritance( child, parent );
//        
//        Build parentBuild = parent.getBuild();
//        Build childBuild = child.getBuild();
//        
//        if(parentBuild != null)
//        {
//            List parentPlugins = parentBuild.getPlugins();
//            
//            if( childBuild == null )
//            {
//                childBuild = new Build();
//                child.setBuild(childBuild);
//            }
//            
//            Map childPluginMap = childBuild.getPluginsAsMap();
//            
//            for ( Iterator it = parentPlugins.iterator(); it.hasNext(); )
//            {
//                Plugin parentPlugin = (Plugin) it.next();
//                
//                String inherited = parentPlugin.getInherited();
//                
//                if( inherited != null )
//                {
//                    
//                }
//                else
//                {
//                    // determine from the plugin descriptor what the default behavior is...
//                    PluginDescriptor pluginDescriptor = pluginManager.verifyPlugin(parentPlugin.getGroupId(), parentPlugin.getArtifactId(), parentPlugin.getVersion(), session);
//                }
//            }
//            ModelUtils.mergeSupplementalPluginDefinition( plugin, def );
//        }
//    }

}
