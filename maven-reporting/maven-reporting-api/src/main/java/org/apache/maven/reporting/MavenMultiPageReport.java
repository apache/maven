package org.apache.maven.reporting;

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

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkFactory;

import java.util.Locale;

/**
 * Temporary class for backwards compatibility. This method
 * should be moved to the MavenReport class, and the other 'generate'
 * method should be dropped. But that will render all reporting mojo's 
 * uncompilable. 
 *
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 */
public interface MavenMultiPageReport
    extends MavenReport
{
    void generate( Sink sink, SinkFactory sinkFactory, Locale locale )
        throws MavenReportException;
}
