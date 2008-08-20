package org.apache.maven.project;


/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Exception that occurs when the user has identifed a project to make, but that project doesn't exist.
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * @version $Id: DuplicateProjectException.java 640549 2008-03-24 20:05:11Z bentmann $
 */
public class MissingProjectException
    extends Exception
{
    public MissingProjectException( String message )
    {
        super( message );
    }
    
    public MissingProjectException( String message, Exception e )
    {
        super( message, e );
    }
}
