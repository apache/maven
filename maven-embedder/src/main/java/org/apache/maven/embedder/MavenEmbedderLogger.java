package org.apache.maven.embedder;

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
 * @author Jason van Zyl
 * @version $Id$
 */
public interface MavenEmbedderLogger
{
    int LEVEL_DEBUG = 0;

    int LEVEL_INFO = 1;

    int LEVEL_WARN = 2;

    int LEVEL_ERROR = 3;

    int LEVEL_FATAL = 4;

    int LEVEL_DISABLED = 5;

    void debug( String message );

    void debug( String message,
                Throwable throwable );

    boolean isDebugEnabled();

    void info( String message );

    void info( String message,
               Throwable throwable );

    boolean isInfoEnabled();

    void warn( String message );

    void warn( String message,
               Throwable throwable );

    boolean isWarnEnabled();

    void error( String message );

    void error( String message,
                Throwable throwable );

    boolean isErrorEnabled();

    void fatalError( String message );

    void fatalError( String message,
                     Throwable throwable );

    boolean isFatalErrorEnabled();

    void setThreshold( int threshold );

    int getThreshold();

    void close();
}
