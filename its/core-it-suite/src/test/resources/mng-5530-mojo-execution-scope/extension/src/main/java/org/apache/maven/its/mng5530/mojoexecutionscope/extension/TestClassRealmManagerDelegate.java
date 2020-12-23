package org.apache.maven.its.mng5530.mojoexecutionscope.extension;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import javax.inject.Named;

import org.apache.maven.classrealm.ClassRealmManagerDelegate;
import org.apache.maven.classrealm.ClassRealmRequest;
import org.apache.maven.classrealm.ClassRealmRequest.RealmType;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

@Named
public class TestClassRealmManagerDelegate
    implements ClassRealmManagerDelegate
{

    public void setupRealm( ClassRealm classRealm, ClassRealmRequest request )
    {
        if ( request.getType() == RealmType.Plugin )
        {
            request.getForeignImports().put( getClass().getPackage().getName() + ".*", getClass().getClassLoader() );
        }
    }
}
