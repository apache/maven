package org.apache.maven.plugin;

import org.apache.maven.model.Goal;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.codehaus.plexus.util.xml.Xpp3Dom;

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

public class GoalInstance
{

    private final MojoDescriptor mojoDescriptor;

    private String pluginKey;

    private String executionId;

    private Xpp3Dom calculatedConfiguration;

    public GoalInstance( Plugin plugin, PluginExecution pluginExecution, Goal goal, MojoDescriptor mojoDescriptor )
    {
        if ( plugin != null )
        {
            this.pluginKey = plugin.getKey();
        }

        if ( pluginExecution != null )
        {
            this.executionId = pluginExecution.getId();
        }

        this.mojoDescriptor = mojoDescriptor;

        calculateConfiguration( plugin, pluginExecution, goal );
    }

    public GoalInstance( Plugin plugin, Goal goal, MojoDescriptor mojoDescriptor )
    {
        this( plugin, null, goal, mojoDescriptor );
    }

    public GoalInstance( MojoDescriptor mojoDescriptor )
    {
        this( null, null, null, mojoDescriptor );
    }

    public MojoDescriptor getMojoDescriptor()
    {
        return mojoDescriptor;
    }

    public String getMojoExecutePhase()
    {
        return mojoDescriptor.getExecutePhase();
    }

    public String getPluginKey()
    {
        return pluginKey;
    }

    public String getExecutionId()
    {
        return executionId;
    }

    public String getGoalId()
    {
        return mojoDescriptor.getGoal();
    }

    public Xpp3Dom calculateConfiguration( Plugin plugin, PluginExecution pluginExecution, Goal goal )
    {
        calculatedConfiguration = new Xpp3Dom( "configuration" );

        if ( plugin != null )
        {
            Xpp3Dom pluginConfig = (Xpp3Dom) plugin.getConfiguration();

            if ( pluginConfig != null )
            {
                calculatedConfiguration = Xpp3Dom.mergeXpp3Dom( pluginConfig, calculatedConfiguration );
            }
        }

        if ( pluginExecution != null )
        {
            Xpp3Dom executionConfig = (Xpp3Dom) pluginExecution.getConfiguration();

            if ( executionConfig != null )
            {
                calculatedConfiguration = Xpp3Dom.mergeXpp3Dom( executionConfig, calculatedConfiguration );
            }
        }

        if ( goal != null )
        {
            Xpp3Dom goalConfig = (Xpp3Dom) goal.getConfiguration();

            if ( goalConfig != null )
            {
                calculatedConfiguration = Xpp3Dom.mergeXpp3Dom( goalConfig, calculatedConfiguration );
            }
        }

        calculatedConfiguration = new Xpp3Dom( calculatedConfiguration );

        return calculatedConfiguration;
    }

    public boolean equals( Object object )
    {
        if ( object == this )
        {
            return true;
        }

        if ( object instanceof GoalInstance )
        {
            GoalInstance other = (GoalInstance) object;

            if ( !getMojoDescriptor().equals( other.getMojoDescriptor() ) )
            {
                return false;
            }

            String execId = getExecutionId();

            String otherExecId = other.getExecutionId();

            if ( execId == otherExecId )
            {
                return true;
            }

            if ( execId == null && otherExecId != null )
            {
                return false;
            }

            if ( execId != null && otherExecId == null )
            {
                return false;
            }

            return execId.equals( otherExecId );
        }

        return false;
    }

    public int hashCode()
    {
        int result = 2;

        // this should NEVER be null...
        result += getMojoDescriptor().hashCode();

        String execId = getExecutionId();

        if ( execId != null )
        {
            result -= execId.hashCode();
        }

        return result;
    }

    public String toString()
    {
        return "goal instance {goal: " + getGoalId() + ", execution-id: " + getExecutionId() + "}";
    }

    public Xpp3Dom getCalculatedConfiguration()
    {
        return new Xpp3Dom( calculatedConfiguration );
    }

    public void incorporate( GoalInstance other )
    {
        Xpp3Dom otherConfig = (Xpp3Dom) other.getCalculatedConfiguration();

        if ( otherConfig != null )
        {
            calculatedConfiguration = new Xpp3Dom( Xpp3Dom.mergeXpp3Dom( otherConfig, calculatedConfiguration ) );
        }
    }

}
