package org.apache.maven.project;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;

import junit.framework.TestCase;

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

public class ModelUtilsTest
    extends TestCase
{

    public void testShouldInheritOnePluginWithExecution()
    {
        Plugin parent = new Plugin();
        parent.setArtifactId( "testArtifact" );
        parent.setGroupId( "testGroup" );
        parent.setVersion( "1.0" );

        PluginExecution parentExecution = new PluginExecution();
        parentExecution.setId( "testExecution" );

        parent.addExecution( parentExecution );

        Plugin child = new Plugin();
        child.setArtifactId( "testArtifact" );
        child.setGroupId( "testGroup" );
        child.setVersion( "1.0" );

        ModelUtils.mergePluginDefinitions( child, parent, false );

        assertEquals( 1, child.getExecutions().size() );
    }

    public void testShouldMergeInheritedPluginHavingExecutionWithLocalPlugin()
    {
        Plugin parent = new Plugin();
        parent.setArtifactId( "testArtifact" );
        parent.setGroupId( "testGroup" );
        parent.setVersion( "1.0" );

        PluginExecution parentExecution = new PluginExecution();
        parentExecution.setId( "testExecution" );

        parent.addExecution( parentExecution );

        Plugin child = new Plugin();
        child.setArtifactId( "testArtifact" );
        child.setGroupId( "testGroup" );
        child.setVersion( "1.0" );

        PluginExecution childExecution = new PluginExecution();
        childExecution.setId( "testExecution2" );

        child.addExecution( childExecution );

        ModelUtils.mergePluginDefinitions( child, parent, false );

        assertEquals( 2, child.getExecutions().size() );
    }

    public void testShouldNOTMergeInheritedPluginHavingInheritEqualFalse()
    {
        Plugin parent = new Plugin();
        parent.setArtifactId( "testArtifact" );
        parent.setGroupId( "testGroup" );
        parent.setVersion( "1.0" );
        parent.setInherited( "false" );

        PluginExecution parentExecution = new PluginExecution();
        parentExecution.setId( "testExecution" );

        parent.addExecution( parentExecution );

        Plugin child = new Plugin();
        child.setArtifactId( "testArtifact" );
        child.setGroupId( "testGroup" );
        child.setVersion( "1.0" );

        ModelUtils.mergePluginDefinitions( child, parent, true );

        assertEquals( 0, child.getExecutions().size() );
    }

}
