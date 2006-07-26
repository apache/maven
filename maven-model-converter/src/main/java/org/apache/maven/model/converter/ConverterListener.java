package org.apache.maven.model.converter;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

public interface ConverterListener
{
    void debug( String message );

    void debug( String message, Throwable throwable );

    void info( String message );

    void info( String message, Throwable throwable );

    void warn( String message );

    void warn( String message, Throwable throwable );

    void error( String message);

    void error( String message, Throwable throwable );
}
