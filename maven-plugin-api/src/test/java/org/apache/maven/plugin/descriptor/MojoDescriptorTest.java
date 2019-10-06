package org.apache.maven.plugin.descriptor;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MojoDescriptorTest
{
    @Test
    public void getParameterMap() throws DuplicateParameterException
    {
        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        Parameter param1 = new Parameter();
        param1.setName( "param1" );
        param1.setDefaultValue( "value1" );
        mojoDescriptor.addParameter( param1 );

        List<Parameter> parameters = mojoDescriptor.getParameters();
        assertEquals( 1, parameters.size() );

        Map<String, Parameter> parameterMap = mojoDescriptor.getParameterMap();
        assertEquals( parameters.size(), parameterMap.size() );

        Parameter param2 = new Parameter();
        param2.setName( "param2" );
        param2.setDefaultValue( "value2" );
        mojoDescriptor.addParameter( param2 );

        assertEquals( 2, parameters.size() );
        assertEquals( parameters.size(), parameterMap.size() );
    }

}