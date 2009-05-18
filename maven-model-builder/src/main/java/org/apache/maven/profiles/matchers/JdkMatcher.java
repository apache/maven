package org.apache.maven.profiles.matchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.maven.model.Profile;

public class JdkMatcher
	implements ProfileMatcher 
	{
	
    private static final String JDK_VERSION = "java.version";

	public boolean isMatch(Profile profile,
			Properties properties) {
        String version = null;
        for ( Entry<Object, Object> ip : properties.entrySet() )
        {
        	if(ip.getKey().equals(JDK_VERSION))
        	{
        		version = (String) ip.getValue();
        		break;
        	}         
        }

        if ( version == null )
        {
            return false;
        }
        
        org.apache.maven.model.Activation activation = profile.getActivation();
        if(activation == null || activation.getJdk() == null)
        {
        	return false;
        }
     
        String jdk = activation.getJdk();
        if ( jdk.startsWith( "!" ) )
        {
            return !version.startsWith( jdk.replaceFirst( "!", "" ) );
        }
        else if ( isRange( jdk ) )
        {
            return isInRange( version, getRange( jdk ) );
        }
        else
        {
            return version.startsWith( jdk );
        }

	}
	
    private static boolean isInRange( String value, List<RangeValue> range )
    {
        int leftRelation = getRelationOrder( value, range.get( 0 ), true );

        if ( leftRelation == 0 )
        {
            return true;
        }

        if ( leftRelation < 0 )
        {
            return false;
        }

        return getRelationOrder( value, range.get( 1 ), false ) <= 0;
    }

    private static int getRelationOrder( String value, RangeValue rangeValue, boolean isLeft )
    {
        if ( rangeValue.value.length() <= 0 )
        {
            return isLeft ? 1 : -1;
        }

        List<String> valueTokens = new ArrayList<String>( Arrays.asList( value.split( "\\." ) ) );
        List<String> rangeValueTokens = new ArrayList<String>( Arrays.asList( rangeValue.value.split( "\\." ) ) );

        int max = Math.max( valueTokens.size(), rangeValueTokens.size() );
        addZeroTokens( valueTokens, max );
        addZeroTokens( rangeValueTokens, max );

        if ( value.equals( rangeValue.value ) )
        {
            if ( !rangeValue.isClosed() )
            {
                return isLeft ? -1 : 1;
            }
            return 0;
        }

        for ( int i = 0; i < valueTokens.size(); i++ )
        {
            int x = Integer.parseInt( valueTokens.get( i ) );
            int y = Integer.parseInt( rangeValueTokens.get( i ) );
            if ( x < y )
            {
                return -1;
            }
            else if ( x > y )
            {
                return 1;
            }
        }
        if ( !rangeValue.isClosed() )
        {
            return isLeft ? -1 : 1;
        }
        return 0;
    }

    private static void addZeroTokens( List<String> tokens, int max )
    {
        if ( tokens.size() < max )
        {
            for ( int i = 0; i < ( max - tokens.size() ); i++ )
            {
                tokens.add( "0" );
            }
        }
    }

    private static boolean isRange( String value )
    {
        return value.contains( "," );
    }

    private static List<RangeValue> getRange( String range )
    {
        List<RangeValue> ranges = new ArrayList<RangeValue>();

        for ( String token : range.split( "," ) )
        {
            if ( token.startsWith( "[" ) )
            {
                ranges.add( new RangeValue( token.replace( "[", "" ), true ) );
            }
            else if ( token.startsWith( "(" ) )
            {
                ranges.add( new RangeValue( token.replace( "(", "" ), false ) );
            }
            else if ( token.endsWith( "]" ) )
            {
                ranges.add( new RangeValue( token.replace( "]", "" ), true ) );
            }
            else if ( token.endsWith( ")" ) )
            {
                ranges.add( new RangeValue( token.replace( ")", "" ), false ) );
            }
            else if ( token.length() <= 0 )
            {
                ranges.add( new RangeValue( "", false ) );
            }
        }
        if ( ranges.size() < 2 )
        {
            ranges.add( new RangeValue( "99999999", false ) );
        }
        return ranges;
    }

    private static class RangeValue
    {
        private String value;

        private boolean isClosed;

        RangeValue( String value, boolean isClosed )
        {
            this.value = value.trim();
            this.isClosed = isClosed;
        }

        public String getValue()
        {
            return value;
        }

        public boolean isClosed()
        {
            return isClosed;
        }
        
        public String toString()
        {
            return value;
        }
    }	
}
