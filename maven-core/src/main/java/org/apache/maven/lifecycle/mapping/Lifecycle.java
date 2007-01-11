package org.apache.maven.lifecycle.mapping;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class Lifecycle.
 */
public class Lifecycle
{
    /**
     * Field id
     */
    private String id;

    /**
     * Field phases
     */
    private Map phases;
    
    private List optionalMojos = new ArrayList();

    /**
     * Method getId
     */
    public String getId()
    {
        return this.id;
    } //-- String getId() 

    /**
     * Method getPhases
     */
    public Map getPhases()
    {
        return this.phases;
    }

    /**
     * Method setId
     *
     * @param id
     */
    public void setId( String id )
    {
        this.id = id;
    } //-- void setId(String) 
    
    public void addOptionalMojo( String optionalMojo )
    {
        this.optionalMojos.add( optionalMojo );
    }
    
    public void setOptionalMojos( List optionalMojos )
    {
        this.optionalMojos = optionalMojos;
    }
    
    public List getOptionalMojos()
    {
        return this.optionalMojos;
    }
}
