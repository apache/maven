package org.apache.maven.script.marmalade.tags;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.descriptor.Parameter;
import org.codehaus.marmalade.model.AbstractMarmaladeTag;
import org.codehaus.marmalade.runtime.MarmaladeExecutionContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;

/**
 * @author jdcasey Created on Feb 8, 2005
 */
public class ParameterTag
    extends AbstractMarmaladeTag
    implements DescriptionParent
{

    private String name;

    private String type;

    private String description;

    private String expression;

    private String validator;

    private String defaultVal;

    private boolean required = true;

    protected void doExecute( MarmaladeExecutionContext context ) throws MarmaladeExecutionException
    {
        processChildren( context );

        Parameter parameter = buildParameter();

        ParametersTag paramsTag = (ParametersTag) requireParent( ParametersTag.class );
        paramsTag.addParameter( parameter );
    }

    private Parameter buildParameter()
    {
        Parameter param = new Parameter();

        param.setName( name );
        param.setDefaultValue( defaultVal );
        param.setDescription( description );
        param.setExpression( expression );
        param.setRequired( required );
        param.setType( type );
        param.setValidator( validator );

        return param;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public void setExpression( String expression )
    {
        this.expression = expression;
    }

    public void setValidator( String validator )
    {
        this.validator = validator;
    }

    public void setDefault( String defaultVal )
    {
        this.defaultVal = defaultVal;
    }

    public void setRequired( boolean required )
    {
        this.required = required;
    }

}