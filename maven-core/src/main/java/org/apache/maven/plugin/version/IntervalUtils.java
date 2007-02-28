package org.apache.maven.plugin.version;

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

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IntervalUtils
{
    
    private static final String PERIOD_PART_PATTERN = "[0-9]+[WwDdHhMm]?";
    
    private static final Map PART_TYPE_CONTRIBUTIONS;
    
    static
    {
        Map contributions = new HashMap();
        
        contributions.put( "w", new Long( 7 * 24 * 60 * 60 * 1000 ) );
        contributions.put( "d", new Long( 24 * 60 * 60 * 1000 ) );
        contributions.put( "h", new Long( 60 * 60 * 1000 ) );
        contributions.put( "m", new Long( 60 * 1000 ) );
        
        PART_TYPE_CONTRIBUTIONS = contributions;
    }
    
    private IntervalUtils()
    {
        // don't allow construction
    }
    
    public static boolean isExpired( String intervalSpec, Date lastChecked )
    {
        if( "never".equalsIgnoreCase( intervalSpec ) )
        {
            return false;
        }
        else if( "always".equalsIgnoreCase( intervalSpec ) )
        {
            return true;
        }
        else if( intervalSpec != null && intervalSpec.toLowerCase().startsWith("interval:") && intervalSpec.length() > "interval:".length())
        {
            String intervalPart = intervalSpec.substring( "interval:".length() );
            
            // subtract the specified period from now() and see if it's still after the lastChecked date.
            long period = IntervalUtils.parseInterval(intervalPart);
            
            Calendar cal = Calendar.getInstance();
            
            cal.setTimeInMillis( System.currentTimeMillis() - period );
            
            Date test = cal.getTime();
            
            return lastChecked == null || test.after( lastChecked );
        }
        else
        {
            throw new IllegalArgumentException( "Invalid interval specification: \'" + intervalSpec + "\'" );
        }
    }
    
    public static long parseInterval( String interval )
    {
        Matcher partMatcher = Pattern.compile(PERIOD_PART_PATTERN).matcher(interval);
        
        long period = 0;
        
        while( partMatcher.find() )
        {
            String part = partMatcher.group();
            
            period += getPartPeriod( part );
        }
        
        return period;
    }

    private static long getPartPeriod( String part )
    {
        char type = part.charAt( part.length() - 1 );
        
        String coefficientPart;
        
        if( Character.isLetter(type))
        {
            coefficientPart = part.substring( 0, part.length() - 1);
        }
        else
        {
            // if the interval doesn't specify a resolution, assume minutes.
            coefficientPart = part;
            
            type = 'm';
        }
        
        int coefficient = Integer.parseInt( coefficientPart );
        
        Long period = (Long) PART_TYPE_CONTRIBUTIONS.get( "" + Character.toLowerCase( type ) );
        
        long result = 0;
        
        if( period != null )
        {
            result = coefficient * period.longValue();
        }
        
        return result;
    }

}
