package org.apache.maven.repository.legacy.resolver.conflict;

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

import org.codehaus.plexus.component.annotations.Component;

/**
 * The default conflict resolver that delegates to the nearest strategy.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @see NearestConflictResolver
 * @deprecated As of 3.0, use a specific implementation instead, e.g. {@link NearestConflictResolver}
 */
@Deprecated
@Component( role = ConflictResolver.class )
public class DefaultConflictResolver
    extends NearestConflictResolver
{
}
