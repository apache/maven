package org.apache.maven.model.converter.plugins;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author Dennis Lundberg
 * @version $Id$
 */
public class PropertyUtilsTest extends TestCase
{
    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    public void testConvertOnOffToBoolean()
    {
        Assert.assertEquals( null, PropertyUtils.convertOnOffToBoolean( null ) );
        Assert.assertEquals( null, PropertyUtils.convertOnOffToBoolean( "someValue" ) );
        Assert.assertEquals( "true", PropertyUtils.convertOnOffToBoolean( "on" ) );
        Assert.assertEquals( "false", PropertyUtils.convertOnOffToBoolean( "OFF" ) );
    }

    public void testConvertYesNoToBoolean()
    {
        Assert.assertEquals( null, PropertyUtils.convertYesNoToBoolean( null ) );
        Assert.assertEquals( null, PropertyUtils.convertYesNoToBoolean( "someValue" ) );
        Assert.assertEquals( "true", PropertyUtils.convertYesNoToBoolean( "yes" ) );
        Assert.assertEquals( "false", PropertyUtils.convertYesNoToBoolean( "NO" ) );
    }

    public void testInvertBoolean()
    {
        Assert.assertEquals( null, PropertyUtils.invertBoolean( null ) );
        Assert.assertEquals( "true", PropertyUtils.invertBoolean( "someValue" ) );
        Assert.assertEquals( "true", PropertyUtils.invertBoolean( "false" ) );
        Assert.assertEquals( "false", PropertyUtils.invertBoolean( "true" ) );
    }
}
