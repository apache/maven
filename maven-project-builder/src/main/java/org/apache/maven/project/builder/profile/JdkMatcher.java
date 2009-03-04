package org.apache.maven.project.builder.profile;

import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.project.builder.ProjectUri;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JdkMatcher
    implements ActiveProfileMatcher
{
    public boolean isMatch( ModelContainer modelContainer, List<InterpolatorProperty> properties )
    {
        if ( modelContainer == null )
        {
            throw new IllegalArgumentException( "modelContainer: null" );
        }

        if ( properties == null )
        {
            return false;
        }

        for ( InterpolatorProperty property : properties )
        {
            if ( property.getKey().equals( "${java.specification.version}" ) )
            {
                String version = property.getValue();
                for ( ModelProperty modelProperty : modelContainer.getProperties() )
                {

                    if ( modelProperty.getUri().equals( ProjectUri.Profiles.Profile.Activation.jdk ) )
                    {
                        if ( modelProperty.getResolvedValue().startsWith( "!" ) )
                        {
                            return !version.equals( modelProperty.getResolvedValue().replaceFirst( "!", "" ) );
                        }
                        else if ( isRange( modelProperty.getResolvedValue() ) )
                        {
                            return isInRange( version, getRange( modelProperty.getResolvedValue() ) );
                        }
                        else
                        {
                            return version.equals( modelProperty.getResolvedValue() );
                        }

                    }
                }
                return false;
            }
        }
        return false;
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
        List<String> valueTokens = Arrays.asList( value.split( "." ) );
        List<String> rangeValueTokens = Arrays.asList( rangeValue.value.split( "." ) );

        int max = Math.max( valueTokens.size(), rangeValueTokens.size() );
        addZeroTokens( valueTokens, max );
        addZeroTokens( rangeValueTokens, max );

        if ( value.equals( rangeValue.value ) )
        {
            return ( rangeValue.isClosed() ) ? 0 : -1;
        }

        for ( int i = 0; i < valueTokens.size(); i++ )
        {
            int x = Integer.getInteger( valueTokens.get( i ) );
            int y = Integer.getInteger( rangeValueTokens.get( i ) );
            if ( x < y )
            {
                return -1;
            }
            else if ( x > y )
            {
                return 1;
            }
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
    }
}
