package org.apache.maven.model.processors;

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

import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.Model;

public class IssueManagementProcessor
    extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        Model t = (Model) target;
        Model c = (Model) child;
        Model p = (Model) parent;

        if ( c.getIssueManagement() != null )
        {
            IssueManagement childMng = c.getIssueManagement();
            IssueManagement mng = new IssueManagement();

            mng.setSystem( childMng.getSystem() );
            mng.setUrl( childMng.getUrl() );
            t.setIssueManagement( mng );
        }
        else if ( p != null && p.getIssueManagement() != null )
        {
            IssueManagement parentMng = p.getIssueManagement();
            IssueManagement mng = new IssueManagement();

            mng.setSystem( parentMng.getSystem() );
            mng.setUrl( parentMng.getUrl() );
            t.setIssueManagement( mng );
        }
    }
}
