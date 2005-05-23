package org.apache.maven.script.beanshell;

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

import bsh.EvalError;
import bsh.Interpreter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.component.factory.bsh.BshComponent;

/**
 * Mojo adapter for a Beanshell Mojo.
 *
 * @todo should log be passed in, or rely on getLog() ?
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class BeanshellMojoAdapter
    extends AbstractMojo
    implements BshComponent
{
    private Mojo mojo;

    private Interpreter interpreter;

    public BeanshellMojoAdapter( Mojo mojo, Interpreter interpreter )
    {
        this.mojo = mojo;
        this.interpreter = interpreter;
    }

    public void execute()
        throws MojoExecutionException
    {
        try
        {
            interpreter.set( "logger", getLog() );

            // TODO: set out, err to a print stream that will log at info, error respectively
        }
        catch ( EvalError evalError )
        {
            throw new MojoExecutionException( "Unable to establish mojo", evalError );
        }

        mojo.execute();
    }

    public Interpreter getInterpreter()
    {
        return interpreter;
    }
}
