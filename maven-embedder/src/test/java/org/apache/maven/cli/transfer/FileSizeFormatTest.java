package org.apache.maven.cli.transfer;

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

import java.util.Locale;
import java.util.stream.Stream;

import org.apache.maven.cli.transfer.AbstractMavenTransferListener.FileSizeFormat;
import org.apache.maven.cli.transfer.AbstractMavenTransferListener.FileSizeFormat.ScaleUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class FileSizeFormatTest
{

    @Test
    void testNegativeSize()
    {
        FileSizeFormat format = new FileSizeFormat( Locale.ENGLISH );

        long negativeSize = -100L;
        assertThatIllegalArgumentException().isThrownBy( () -> format.format( negativeSize ) ).withMessage(
                "file size cannot be negative: -100" );
    }

    @ParameterizedTest(name = "{index}: value''{0}'' expectedResult={1}")
    @MethodSource
    void verify_output_without_given_scale_unit(long value, String expectedAnswer)
    {
        FileSizeFormat format = new FileSizeFormat( Locale.ENGLISH );
        assertThat( format.format( value) ).isEqualTo( expectedAnswer );
    }

    static Stream<Arguments> verify_output_without_given_scale_unit() {
        long _0_bytes = 0L;
        long _5_bytes = 5L;
        long _10_bytes = 10L;
        long _15_bytes = 15L;
        long _49_bytes = 49L;
        long _50_bytes = 50L;
        long _999_bytes = 999L;
        long _1000_bytes = 1000L;
        long _5500_bytes = 5500L;
        long _10_kilobytes = 10L * 1000L;
        long _15_kilobytes = 15L * 1000L;
        long _49_kilobytes = 49L * 1000L;
        long _50_kilobytes = 50L * 1000L;
        long _999_kilobytes = 999L * 1000L;
        long _1000_kilobytes = 1000L * 1000L;
        long _5500_kilobytes = 5500L * 1000L;
        long _10_megabytes = 10L * 1000L * 1000L;
        long _15_megabytes = 15L * 1000L * 1000L;
        long _49_megabytes = 49L * 1000L * 1000L;
        long _50_megabytes = 50L * 1000L * 1000L;
        long _999_megabytes = 999L * 1000L * 1000L;
        long _1000_megabytes = 1000L * 1000L * 1000L;
        long _5500_megabytes = 5500L * 1000L * 1000L;
        long _10_gigabytes = 10L * 1000L * 1000L * 1000L;
        long _15_gigabytes = 15L * 1000L * 1000L * 1000L;
        long _1000_gigabytes = 1000L * 1000L * 1000L * 1000L;

        return Stream.of(
                arguments( _0_bytes, "0 B" ),
                arguments( _5_bytes, "5 B" ),
                arguments( _10_bytes, "10 B" ),
                arguments( _15_bytes, "15 B" ),
                arguments( _49_bytes, "49 B" ),
                arguments( _50_bytes, "50 B" ),
                arguments( _999_bytes, "999 B" ),
                arguments( _1000_bytes, "1.0 kB" ),
                arguments( _5500_bytes, "5.5 kB" ),
                arguments( _10_kilobytes, "10 kB" ),
                arguments( _15_kilobytes, "15 kB" ),
                arguments( _49_kilobytes, "49 kB" ),
                arguments( _50_kilobytes, "50 kB" ),
                arguments( _999_kilobytes, "999 kB" ),
                arguments( _1000_kilobytes, "1.0 MB" ),
                arguments( _5500_kilobytes, "5.5 MB" ),
                arguments( _10_megabytes, "10 MB" ),
                arguments( _15_megabytes, "15 MB" ),
                arguments( _49_megabytes, "49 MB" ),
                arguments( _50_megabytes, "50 MB" ),
                arguments( _999_megabytes, "999 MB" ),
                arguments( _1000_megabytes, "1.0 GB" ),
                arguments( _5500_megabytes, "5.5 GB" ),
                arguments( _10_gigabytes, "10 GB" ),
                arguments( _15_gigabytes, "15 GB" ),
                arguments( _1000_gigabytes, "1000 GB" )

        );
    }

    @ParameterizedTest(name = "{index}: value''{0}'' Scale={1} expectedResult={2}")
    @MethodSource
    void verify_output_with_given_scale_unit(long value, ScaleUnit scaleUnit, String expectedResult)
    {
        FileSizeFormat format = new FileSizeFormat( Locale.ENGLISH );
        assertThat( format.format( value, scaleUnit ) ).isEqualTo( expectedResult );
    }

    static Stream<Arguments> verify_output_with_given_scale_unit() {
        long _0_bytes = 0L;
        long _5_bytes = 5L;
        long _49_bytes = 49L;
        long _50_bytes = 50L;
        long _999_bytes = 999L;
        long _1000_bytes = 1000L;
        long _49_kilobytes = 49L * 1000L;
        long _50_kilobytes = 50L * 1000L;
        long _999_kilobytes = 999L * 1000L;
        long _1000_kilobytes = 1000L * 1000L;
        long _49_megabytes = 49L * 1000L * 1000L;
        long _50_megabytes = 50L * 1000L * 1000L;
        long _999_megabytes = 999L * 1000L * 1000L;
        long _1000_megabytes = 1000L * 1000L * 1000L;

        return Stream.of(
                arguments( _0_bytes, ScaleUnit.BYTE, "0 B" ),
                arguments( _0_bytes, ScaleUnit.KILOBYTE, "0 kB" ),
                arguments( _0_bytes, ScaleUnit.MEGABYTE, "0 MB" ),
                arguments( _0_bytes, ScaleUnit.GIGABYTE, "0 GB" ),

                arguments( _5_bytes, ScaleUnit.BYTE, "5 B" ),
                arguments( _5_bytes, ScaleUnit.KILOBYTE, "0 kB" ),
                arguments( _5_bytes, ScaleUnit.MEGABYTE, "0 MB" ),
                arguments( _5_bytes, ScaleUnit.GIGABYTE, "0 GB" ),

                arguments( _49_bytes, ScaleUnit.BYTE, "49 B" ),
                arguments( _49_bytes, ScaleUnit.KILOBYTE, "0 kB" ),
                arguments( _49_bytes, ScaleUnit.MEGABYTE, "0 MB" ),
                arguments( _49_bytes, ScaleUnit.GIGABYTE, "0 GB" ),

                arguments( _50_bytes, ScaleUnit.BYTE, "50 B" ),
                arguments( _50_bytes, ScaleUnit.KILOBYTE, "0.1 kB" ),
                arguments( _50_bytes, ScaleUnit.MEGABYTE, "0 MB" ),
                arguments( _50_bytes, ScaleUnit.GIGABYTE, "0 GB" ),

                arguments( _999_bytes, ScaleUnit.BYTE, "999 B" ),
                arguments( _999_bytes, ScaleUnit.KILOBYTE, "1.0 kB" ),
                arguments( _999_bytes, ScaleUnit.MEGABYTE, "0 MB" ),
                arguments( _999_bytes, ScaleUnit.GIGABYTE, "0 GB" ),

                arguments( _1000_bytes, ScaleUnit.BYTE, "1000 B" ),
                arguments( _1000_bytes, ScaleUnit.KILOBYTE, "1.0 kB" ),
                arguments( _1000_bytes, ScaleUnit.MEGABYTE, "0 MB" ),
                arguments( _1000_bytes, ScaleUnit.GIGABYTE, "0 GB" ),

                arguments( _49_kilobytes, ScaleUnit.BYTE, "49000 B" ),
                arguments( _49_kilobytes, ScaleUnit.KILOBYTE, "49 kB" ),
                arguments( _49_kilobytes, ScaleUnit.MEGABYTE, "0 MB" ),
                arguments( _49_kilobytes, ScaleUnit.GIGABYTE, "0 GB" ),

                arguments( _50_kilobytes, ScaleUnit.BYTE, "50000 B" ),
                arguments( _50_kilobytes, ScaleUnit.KILOBYTE, "50 kB" ),
                arguments( _50_kilobytes, ScaleUnit.MEGABYTE, "0.1 MB" ),
                arguments( _50_kilobytes, ScaleUnit.GIGABYTE, "0 GB" ),

                arguments( _999_kilobytes, ScaleUnit.BYTE, "999000 B" ),
                arguments( _999_kilobytes, ScaleUnit.KILOBYTE, "999 kB" ),
                arguments( _999_kilobytes, ScaleUnit.MEGABYTE, "1.0 MB" ),
                arguments( _999_kilobytes, ScaleUnit.GIGABYTE, "0 GB" ),

                arguments( _1000_kilobytes, ScaleUnit.BYTE, "1000000 B" ),
                arguments( _1000_kilobytes, ScaleUnit.KILOBYTE, "1000 kB" ),
                arguments( _1000_kilobytes, ScaleUnit.MEGABYTE, "1.0 MB" ),
                arguments( _1000_kilobytes, ScaleUnit.GIGABYTE, "0 GB" ),

                arguments( _49_megabytes, ScaleUnit.BYTE, "49000000 B" ),
                arguments( _49_megabytes, ScaleUnit.KILOBYTE, "49000 kB" ),
                arguments( _49_megabytes, ScaleUnit.MEGABYTE, "49 MB" ),
                arguments( _49_megabytes, ScaleUnit.GIGABYTE, "0 GB" ),

                arguments( _50_megabytes, ScaleUnit.BYTE, "50000000 B" ),
                arguments( _50_megabytes, ScaleUnit.KILOBYTE, "50000 kB" ),
                arguments( _50_megabytes, ScaleUnit.MEGABYTE, "50 MB" ),
                arguments( _50_megabytes, ScaleUnit.GIGABYTE, "0.1 GB" ),

                arguments( _999_megabytes, ScaleUnit.BYTE, "999000000 B" ),
                arguments( _999_megabytes, ScaleUnit.KILOBYTE, "999000 kB" ),
                arguments( _999_megabytes, ScaleUnit.MEGABYTE, "999 MB" ),
                arguments( _999_megabytes, ScaleUnit.GIGABYTE, "1.0 GB" ),

                arguments( _1000_megabytes, ScaleUnit.BYTE, "1000000000 B" ),
                arguments( _1000_megabytes, ScaleUnit.KILOBYTE, "1000000 kB" ),
                arguments( _1000_megabytes, ScaleUnit.MEGABYTE, "1000 MB" ),
                arguments( _1000_megabytes, ScaleUnit.GIGABYTE, "1.0 GB" )

        );
    }

    @Test
    void testNegativeProgressedSize()
    {
        FileSizeFormat format = new FileSizeFormat( Locale.ENGLISH );

        long negativeProgressedSize = -100L;
        assertThatIllegalArgumentException().isThrownBy( () -> format.formatProgress( negativeProgressedSize, 10L ) )
                                            .withMessage( "progressed file size cannot be negative: -100" );
    }

    @Test
    void testNegativeProgressedSizeBiggerThanSize()
    {
        FileSizeFormat format = new FileSizeFormat( Locale.ENGLISH );

        assertThatIllegalArgumentException().isThrownBy( () -> format.formatProgress( 100L, 10L ) ).withMessage(
                "progressed file size cannot be greater than size: 100 > 10" );
    }

    @Test
    void testProgressedSizeWithoutSize()
    {
        FileSizeFormat format = new FileSizeFormat( Locale.ENGLISH );

        long _0_bytes = 0L;
        assertThat( format.formatProgress( _0_bytes, -1L ) ).isEqualTo( "0 B" );

        long _1000_bytes = 1000L;
        assertThat( format.formatProgress( _1000_bytes, -1L ) ).isEqualTo( "1.0 kB" );

        long _1000_kilobytes = 1000L * 1000L;
        assertThat( format.formatProgress( _1000_kilobytes, -1L ) ).isEqualTo( "1.0 MB" );

        long _1000_megabytes = 1000L * 1000L * 1000L;
        assertThat( format.formatProgress( _1000_megabytes, -1L ) ).isEqualTo( "1.0 GB" );

    }

    @Test
    void testProgressedBothZero()
    {
        FileSizeFormat format = new FileSizeFormat( Locale.ENGLISH );

        long _0_bytes = 0L;
        assertThat( format.formatProgress( _0_bytes, _0_bytes ) ).isEqualTo( "0 B" );
    }

    @Test
    void testProgressedSizeWithSize()
    {
        FileSizeFormat format = new FileSizeFormat( Locale.ENGLISH );

        long _0_bytes = 0L;
        long _400_bytes = 400L;
        long _800_bytes = 2L * _400_bytes;
        assertThat( format.formatProgress( _0_bytes, _800_bytes ) ).isEqualTo( "0/800 B" );
        assertThat( format.formatProgress( _400_bytes, _800_bytes ) ).isEqualTo( "400/800 B" );
        assertThat( format.formatProgress( _800_bytes, _800_bytes ) ).isEqualTo( "800 B" );

        long _4000_bytes = 4000L;
        long _8000_bytes = 2L * _4000_bytes;
        long _50_kilobytes = 50000L;
        assertThat( format.formatProgress( _0_bytes, _8000_bytes ) ).isEqualTo( "0/8.0 kB" );
        assertThat( format.formatProgress( _400_bytes, _8000_bytes ) ).isEqualTo( "0.4/8.0 kB" );
        assertThat( format.formatProgress( _4000_bytes, _8000_bytes ) ).isEqualTo( "4.0/8.0 kB" );
        assertThat( format.formatProgress( _8000_bytes, _8000_bytes ) ).isEqualTo( "8.0 kB" );
        assertThat( format.formatProgress( _8000_bytes, _50_kilobytes ) ).isEqualTo( "8.0/50 kB" );
        assertThat( format.formatProgress( 2L * _8000_bytes, _50_kilobytes ) ).isEqualTo( "16/50 kB" );
        assertThat( format.formatProgress( _50_kilobytes, _50_kilobytes ) ).isEqualTo( "50 kB" );

        long _500_kilobytes = 500000L;
        long _1000_kilobytes = 2L * _500_kilobytes;
        ;
        long _5000_kilobytes = 5L * _1000_kilobytes;
        long _15_megabytes = 3L * _5000_kilobytes;
        assertThat( format.formatProgress( _0_bytes, _5000_kilobytes ) ).isEqualTo( "0/5.0 MB" );
        assertThat( format.formatProgress( _500_kilobytes, _5000_kilobytes ) ).isEqualTo( "0.5/5.0 MB" );
        assertThat( format.formatProgress( _1000_kilobytes, _5000_kilobytes ) ).isEqualTo( "1.0/5.0 MB" );
        assertThat( format.formatProgress( _5000_kilobytes, _5000_kilobytes ) ).isEqualTo( "5.0 MB" );
        assertThat( format.formatProgress( _5000_kilobytes, _15_megabytes ) ).isEqualTo( "5.0/15 MB" );
        assertThat( format.formatProgress( _15_megabytes, _15_megabytes ) ).isEqualTo( "15 MB" );

        long _500_megabytes = 500000000L;
        long _1000_megabytes = 2L * _500_megabytes;
        long _5000_megabytes = 5L * _1000_megabytes;
        long _15_gigabytes = 3L * _5000_megabytes;
        assertThat( format.formatProgress( _0_bytes, _500_megabytes ) ).isEqualTo( "0/500 MB" );
        assertThat( format.formatProgress( _1000_megabytes, _5000_megabytes ) ).isEqualTo( "1.0/5.0 GB" );
        assertThat( format.formatProgress( _5000_megabytes, _5000_megabytes ) ).isEqualTo( "5.0 GB" );
        assertThat( format.formatProgress( _5000_megabytes, _15_gigabytes ) ).isEqualTo( "5.0/15 GB" );
        assertThat( format.formatProgress( _15_gigabytes, _15_gigabytes ) ).isEqualTo( "15 GB" );
    }

}