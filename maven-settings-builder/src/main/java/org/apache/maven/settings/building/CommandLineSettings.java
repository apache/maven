package org.apache.maven.settings.building;

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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CommandLineSettings
{
    private List<String> activeProfiles = new ArrayList<>( 3 );
    private List<String> inActiveProfiles = new ArrayList<>( 3 );

    public List<String> getActiveProfiles()
    {
        return Collections.unmodifiableList( activeProfiles );
    }

    public void addActiveProfiles( Collection<String> profiles )
    {
        activeProfiles.addAll( profiles );
    }

    public List<String> getInActiveProfiles()
    {
        return Collections.unmodifiableList( inActiveProfiles );
    }

    public void addInActiveProfiles( Collection<String> profiles )
    {
        inActiveProfiles.addAll( profiles );
    }
}
