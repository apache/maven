package org.apache.maven.plugins;

/*
 * Copyright 2004-2006 The Apache Software Foundation.
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.scm.manager.AbstractScmManager;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.manager.plexus.DefaultScmManager;

/**
 * Test for classpath issues
 *
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 * @goal test
 */
public class ScmTestMojo
    extends AbstractMojo
{

    /**
     * Maven SCM Manager.
     *
     * @parameter expression="${component.org.apache.maven.scm.manager.ScmManager}"
     * @required
     * @readonly
     */
    protected ScmManager scmManager;

    public void execute()
    {
        System.out.println( "scmManager class: " + scmManager.getClass().getName() );
        ClassLoader defaultScmManagerClassLoader = DefaultScmManager.class.getClassLoader();
        ClassLoader scmManagerClassLoader = scmManager.getClass().getClassLoader();
        System.out.println( "DefaultScmManager.class.getClassLoader(): " + defaultScmManagerClassLoader );
        System.out.println( "scmManager.getClass().getClassLoader(): " + scmManagerClassLoader );
        System.out.println( "DefaultScmManager.class.isAssignableFrom( scmManager.getClass() ): " +
            DefaultScmManager.class.isAssignableFrom( scmManager.getClass() ) );
        System.out.println( "AbstractScmManager.class.isAssignableFrom( scmManager.getClass() )" +
            AbstractScmManager.class.isAssignableFrom( scmManager.getClass() ) );
        System.out.println( "ScmManager.class.isAssignableFrom( scmManager.getClass() ): " +
            ScmManager.class.isAssignableFrom( scmManager.getClass() ) );

        /* This breaks */
        DefaultScmManager defaultScmManager = (DefaultScmManager) scmManager;
    }

}
