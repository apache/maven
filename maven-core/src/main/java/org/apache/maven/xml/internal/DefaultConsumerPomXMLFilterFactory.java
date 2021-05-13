package org.apache.maven.xml.internal;

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

import org.apache.maven.model.building.DefaultBuildPomXMLFilterFactory;
import org.apache.maven.model.transform.RawToConsumerPomXMLFilterFactory;

/**
 * The default implementation of the {@link RawToConsumerPomXMLFilterFactory}
 * It will provide several values for the consumer pom based on its context.
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
public class DefaultConsumerPomXMLFilterFactory extends RawToConsumerPomXMLFilterFactory
{
    public DefaultConsumerPomXMLFilterFactory( DefaultBuildPomXMLFilterFactory buildPomXMLFilterFactory )
    {
        super( buildPomXMLFilterFactory );
    }
}
