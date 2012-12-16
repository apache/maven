package org.apache.maven.cli.logging;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import org.apache.maven.execution.MavenExecutionRequest;
import org.codehaus.plexus.logging.Logger;

/**
 * Interface for configuration operations on loggers, which are not available in slf4j, then require per-slf4f-binding
 * implementation.
 * 
 * @author Hervé Boutemy
 */
public interface Slf4jConfiguration
{
    /**
     * Set root logging level.
     *
     * @param level the level as defined in Plexus <code>Logger.LEVEL_*</code> and equivalent 
     * <code>MavenExecutionRequest.LOGGING_LEVEL_*</code> constants.
     * @see Logger
     * @see MavenExecutionRequest
     */
    void setRootLoggerLevel( int level );

    void setLoggerFile( File output );
}
