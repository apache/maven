package org.apache.maven.plugin.descriptor;

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
 */
public class Parameter
    implements Cloneable
{
    private String alias;

    private String name;

    private String type;

    private boolean required;

    private boolean editable = true;

    private String description;

    private String expression;

    private String deprecated;

    private String defaultValue;

    private String implementation;

    private Requirement requirement;

    private String since;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public boolean isRequired()
    {
        return required;
    }

    public void setRequired( boolean required )
    {
        this.required = required;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public String getExpression()
    {
        return expression;
    }

    public void setExpression( String expression )
    {
        this.expression = expression;
    }

    public String getDeprecated()
    {
        return deprecated;
    }

    public void setDeprecated( String deprecated )
    {
        this.deprecated = deprecated;
    }

    public int hashCode()
    {
        return name.hashCode();
    }

    public boolean equals( Object other )
    {
        return ( other instanceof Parameter ) && getName().equals( ( (Parameter) other ).getName() );
    }

    public String getAlias()
    {
        return alias;
    }

    public void setAlias( String alias )
    {
        this.alias = alias;
    }

    public boolean isEditable()
    {
        return editable;
    }

    public void setEditable( boolean editable )
    {
        this.editable = editable;
    }

    public void setDefaultValue( String defaultValue )
    {
        this.defaultValue = defaultValue;
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }

    public String toString()
    {
        return "Mojo parameter [name: \'" + getName() + "\'; alias: \'" + getAlias() + "\']";
    }

    public Requirement getRequirement()
    {
        return requirement;
    }

    public void setRequirement( Requirement requirement )
    {
        this.requirement = requirement;
    }

    public String getImplementation()
    {
        return implementation;
    }

    public void setImplementation( String implementation )
    {
        this.implementation = implementation;
    }

    public String getSince()
    {
        return since;
    }

    public void setSince( String since )
    {
        this.since = since;
    }

    /**
     * Creates a shallow copy of this parameter.
     */
    @Override
    public Parameter clone()
    {
        try
        {
            return (Parameter) super.clone();
        }
        catch ( CloneNotSupportedException e )
        {
            throw new UnsupportedOperationException( e );
        }
    }

}
