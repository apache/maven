package org.apache.maven.exception;

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
 * This will be the place where we track anything that can possibly go wrong with a
 * Maven build and try to provide as much help to the user as possible when
 * something does go wrong. This will force us to get specific with exception
 * handling because we should be able to point a user to a spot in the documentation
 * which explains why a particular exception happened.
 *
 * o poorly formed XML POMs (make an error handler for xpp3)
 * o missing artifacts
 * o non existent goals
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * 
 * @version $Id$
 */
public class MavenExceptionHandler
{
}
