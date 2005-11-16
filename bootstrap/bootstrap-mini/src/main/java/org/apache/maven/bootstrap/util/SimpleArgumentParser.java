package org.apache.maven.bootstrap.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

/**
 * @author Brett Porter
 */
public class SimpleArgumentParser
{
    private Map arguments = new TreeMap();

    private List parameters = new ArrayList();

    public void parseCommandLineArguments( String[] args )
    {
        boolean stillHasArgs = true;
        for ( int i = 0; i < args.length; i++ )
        {
            if ( args[i].equals( "--" ) )
            {
                stillHasArgs = false;
            }
            else if ( args[i].startsWith( "-" ) && stillHasArgs )
            {
                if ( args[i].startsWith( "--" ) )
                {
                    String name;
                    int index = args[i].indexOf( '=' );
                    if ( index >= 0 )
                    {
                        name = args[i].substring( 0, index ).trim();
                    }
                    else
                    {
                        name = args[i];
                    }

                    Argument arg = (Argument) arguments.get( name );
                    if ( arg != null )
                    {
                        if ( arg.isHasValue() )
                        {
                            String value = null;
                            if ( index >= 0 )
                            {
                                value = args[i].substring( index + 1 ).trim();
                            }
                            else if ( i != args.length - 1 && !args[i + 1].startsWith( "-" ) )
                            {
                                value = args[i + 1];
                                i++;
                            }

                            arg.setValue( value );
                        }
                        arg.setSet( true );
                    }
                }
                else
                {
                    String name = args[i].substring( 0, 2 );

                    Argument arg = (Argument) arguments.get( name );
                    if ( arg != null )
                    {
                        if ( arg.isHasValue() )
                        {
                            String value = null;
                            if ( args[i].length() > 2 )
                            {
                                value = args[i].substring( 2 );
                            }
                            else if ( i != args.length - 1 && !args[i + 1].startsWith( "-" ) )
                            {
                                value = args[i + 1];
                                i++;
                            }

                            arg.setValue( value );
                        }
                        arg.setSet( true );
                    }
                }
            }
            else
            {
                parameters.add( args[i] );
            }
        }
    }

    public String getArgumentValue( String argument )
    {
        Argument arg = (Argument) arguments.get( argument );
        String value = null;
        if ( arg != null )
        {
            value = arg.getValue();
        }
        return value;
    }

    public List getParameters()
    {
        return parameters;
    }

    public void addArgument( String argument, String description, String alias )
    {
        addArgument( argument, description, alias, false, null );
    }

    public void addArgument( String argument, String description, String alias, boolean hasValue, String defaultValue )
    {
        Argument arg = new Argument( argument, description, alias, hasValue, defaultValue );
        arguments.put( argument, arg );
        if ( alias != null )
        {
            arguments.put( alias, arg );
        }
    }

    public void addArgument( String argument, String description, boolean hasValue, String defaultValue )
    {
        addArgument( argument, description, null, hasValue, defaultValue );
    }

    public void addArgument( String argument, String description )
    {
        addArgument( argument, description, null );
    }

    public void addArgument( String argument, String description, boolean hasValue )
    {
        addArgument( argument, description, hasValue, null );
    }

    public boolean isArgumentSet( String argument )
    {
        Argument arg = (Argument) arguments.get( argument );
        return arg.isSet();
    }

    private static class Argument
    {
        private final String name;

        private final String description;

        private final String alias;

        private String value;

        private final boolean hasValue;

        private final String defaultValue;

        private boolean set;

        public Argument( String name, String description, String alias, boolean hasValue, String defaultValue )
        {
            this.name = name;
            this.description = description;
            this.alias = alias;
            this.hasValue = hasValue;
            this.defaultValue = defaultValue;
        }

        public String getName()
        {
            return name;
        }

        public String getDescription()
        {
            return description;
        }

        public String getAlias()
        {
            return alias;
        }

        public String getValue()
        {
            if ( value == null )
            {
                return defaultValue;
            }
            return value;
        }

        public void setValue( String value )
        {
            this.value = value;
        }

        public boolean isHasValue()
        {
            return hasValue;
        }

        public boolean isSet()
        {
            return set;
        }

        public void setSet( boolean set )
        {
            this.set = set;
        }
    }
}
