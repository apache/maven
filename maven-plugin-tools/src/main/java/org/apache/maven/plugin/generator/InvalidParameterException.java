package org.apache.maven.plugin.generator;

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

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class InvalidParameterException
    extends Exception
{
    public InvalidParameterException()
    {
    }

    public InvalidParameterException( String element, int i )
    {
        super( "The " + element + " element in parameter # " + i + " is invalid. It cannot be null." );
    }

    public InvalidParameterException( Throwable cause )
    {
        super( cause );
    }

    public InvalidParameterException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
