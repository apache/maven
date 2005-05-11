package org.apache.maven.reactor;

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

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ReactorException
    extends Exception
{
    public ReactorException()
    {
    }

    public ReactorException( String message )
    {
        super( message );
    }

    public ReactorException( Throwable cause )
    {
        super( cause );
    }

    public ReactorException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
