package org.apache.maven.lifecycle.phase;


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
import org.apache.maven.decoration.GoalDecorationParser;
import org.apache.maven.decoration.GoalDecoratorBindings;
import org.apache.maven.lifecycle.AbstractMavenLifecyclePhase;
import org.apache.maven.lifecycle.MavenLifecycleContext;
import org.apache.maven.project.MavenProject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @author <a href="mailto:jdcasey@commonjava.org">John Casey</a>
 * @version $Id$
 */
public class GoalDecorationPhase extends AbstractMavenLifecyclePhase
{
    public static final String MAVEN_XML_DEFAULT_NAMESPACE = "mavenxml";
    public static final String MAVEN_SCRIPT = "decorators.xml";
    private GoalDecoratorBindings decorators;
    private boolean decoratorsInitialized = false;

    public void execute( MavenLifecycleContext context )
        throws Exception
    {
        synchronized ( this )
        {
            if ( !decoratorsInitialized )
            {
                initializeDecorators( context );
            }
        }

        context.setGoalDecoratorBindings(decorators);
    }

    private void initializeDecorators( MavenLifecycleContext context )
        throws XmlPullParserException, IOException
    {
        MavenProject project = context.getProject(  );

        File pom = project.getFile(  );

        File dir = pom.getParentFile(  );

        File scriptFile = new File( dir, MAVEN_SCRIPT );

        if ( scriptFile.exists(  ) )
        {
            BufferedReader reader = null;

            try
            {
                reader = new BufferedReader( new FileReader( scriptFile ) );

                GoalDecorationParser parser = new GoalDecorationParser(  );
                this.decorators = parser.parse( reader );
            }
            finally
            {
                if ( reader != null )
                {
                    try
                    {
                        reader.close(  );
                    }
                    catch ( IOException e )
                    {
                    }
                }
            }
        }
        
        decoratorsInitialized = true;
    }
}
