/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.acm.model;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id:$
 */
public class Model
{
    private Map environments = new HashMap();

    private Set propertyKeySet  = new HashSet();

    public void addEnvironment( Environment environment )
    {
        environments.put( environment.getId(), environment );
    }

    public Map getEnvironments()
    {
        return environments;
    }

    public Environment getEnvironment( String id )
    {
        return (Environment) environments.get( id );
    }

    public void addPropertyKey( String propertyKey )
    {
        propertyKeySet.add( propertyKey );
    }

    public Set getPropertyKeySet()
    {
        return propertyKeySet;
    }

    // ----------------------------------------------------------------------
    // for each proprety key I want to know what the values are in each
    // of the environments are.
    // ----------------------------------------------------------------------
}
