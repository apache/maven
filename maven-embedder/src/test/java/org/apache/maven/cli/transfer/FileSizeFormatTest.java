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

import org.apache.maven.cli.transfer.AbstractMavenTransferListener.FileSizeFormat;
import org.apache.maven.cli.transfer.AbstractMavenTransferListener.FileSizeFormat.ScaleUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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

    @Test
    void testSize()
    {
        FileSizeFormat format = new FileSizeFormat( Locale.ENGLISH );

        long _0_bytes = 0L;
        assertThat( format.format( _0_bytes ) ).isEqualTo( "0 B" );

        long _5_bytes = 5L;
        assertThat( format.format( _5_bytes ) ).isEqualTo( "5 B" );

        long _10_bytes = 10L;
        assertThat( format.format( _10_bytes ) ).isEqualTo( "10 B" );

        long _15_bytes = 15L;
        assertThat( format.format( _15_bytes ) ).isEqualTo( "15 B" );

        long _999_bytes = 999L;
        assertThat( format.format( _999_bytes ) ).isEqualTo( "999 B" );

        long _1000_bytes = 1000L;
        assertThat( format.format( _1000_bytes ) ).isEqualTo( "1.0 kB" );

        long _5500_bytes = 5500L;
        assertThat( format.format( _5500_bytes ) ).isEqualTo( "5.5 kB" );

        long _10_kilobytes = 10L * 1000L;
        assertThat( format.format( _10_kilobytes ) ).isEqualTo( "10 kB" );

        long _15_kilobytes = 15L * 1000L;
        assertThat( format.format( _15_kilobytes ) ).isEqualTo( "15 kB" );

        long _999_kilobytes = 999L * 1000L;
        assertThat( format.format( _999_kilobytes ) ).isEqualTo( "999 kB" );

        long _1000_kilobytes = 1000L * 1000L;
        assertThat( format.format( _1000_kilobytes ) ).isEqualTo( "1.0 MB" );

        long _5500_kilobytes = 5500L * 1000L;
        assertThat( format.format( _5500_kilobytes ) ).isEqualTo( "5.5 MB" );

        long _10_megabytes = 10L * 1000L * 1000L;
        assertThat( format.format( _10_megabytes ) ).isEqualTo( "10 MB" );

        long _15_megabytes = 15L * 1000L * 1000L;
        assertThat( format.format( _15_megabytes ) ).isEqualTo( "15 MB" );

        long _999_megabytes = 999L * 1000L * 1000L;
        assertThat( format.format( _999_megabytes ) ).isEqualTo( "999 MB" );

        long _1000_megabytes = 1000L * 1000L * 1000L;
        assertThat( format.format( _1000_megabytes ) ).isEqualTo( "1.0 GB" );

        long _5500_megabytes = 5500L * 1000L * 1000L;
        assertThat( format.format( _5500_megabytes ) ).isEqualTo( "5.5 GB" );

        long _10_gigabytes = 10L * 1000L * 1000L * 1000L;
        assertThat( format.format( _10_gigabytes ) ).isEqualTo( "10 GB" );

        long _15_gigabytes = 15L * 1000L * 1000L * 1000L;
        assertThat( format.format( _15_gigabytes ) ).isEqualTo( "15 GB" );

        long _1000_gigabytes = 1000L * 1000L * 1000L * 1000L;
        assertThat( format.format( _1000_gigabytes ) ).isEqualTo( "1000 GB" );
    }

    @Test
    void testSizeWithSelectedScaleUnit()
    {
        FileSizeFormat format = new FileSizeFormat( Locale.ENGLISH );

        long _0_bytes = 0L;
        assertThat( format.format( _0_bytes ) ).isEqualTo( "0 B" );
        assertThat( format.format( _0_bytes, ScaleUnit.BYTE ) ).isEqualTo( "0 B" );
        assertThat( format.format( _0_bytes, ScaleUnit.KILOBYTE ) ).isEqualTo( "0 kB" );
        assertThat( format.format( _0_bytes, ScaleUnit.MEGABYTE ) ).isEqualTo( "0 MB" );
        assertThat( format.format( _0_bytes, ScaleUnit.GIGABYTE ) ).isEqualTo( "0 GB" );

        long _5_bytes = 5L;
        assertThat( format.format( _5_bytes ) ).isEqualTo( "5 B" );
        assertThat( format.format( _5_bytes, ScaleUnit.BYTE ) ).isEqualTo( "5 B" );
        assertThat( format.format( _5_bytes, ScaleUnit.KILOBYTE ) ).isEqualTo( "0 kB" );
        assertThat( format.format( _5_bytes, ScaleUnit.MEGABYTE ) ).isEqualTo( "0 MB" );
        assertThat( format.format( _5_bytes, ScaleUnit.GIGABYTE ) ).isEqualTo( "0 GB" );


        long _49_bytes = 49L;
        assertThat( format.format( _49_bytes ) ).isEqualTo( "49 B" );
        assertThat( format.format( _49_bytes, ScaleUnit.BYTE ) ).isEqualTo( "49 B" );
        assertThat( format.format( _49_bytes, ScaleUnit.KILOBYTE ) ).isEqualTo( "0 kB" );
        assertThat( format.format( _49_bytes, ScaleUnit.MEGABYTE ) ).isEqualTo( "0 MB" );
        assertThat( format.format( _49_bytes, ScaleUnit.GIGABYTE ) ).isEqualTo( "0 GB" );

        long _50_bytes = 50L;
        assertThat( format.format( _50_bytes ) ).isEqualTo( "50 B" );
        assertThat( format.format( _50_bytes, ScaleUnit.BYTE ) ).isEqualTo( "50 B" );
        assertThat( format.format( _50_bytes, ScaleUnit.KILOBYTE ) ).isEqualTo( "0.1 kB" );
        assertThat( format.format( _50_bytes, ScaleUnit.MEGABYTE ) ).isEqualTo( "0 MB" );
        assertThat( format.format( _50_bytes, ScaleUnit.GIGABYTE ) ).isEqualTo( "0 GB" );

        long _999_bytes = 999L;
        assertThat( format.format( _999_bytes ) ).isEqualTo( "999 B" );
        assertThat( format.format( _999_bytes, ScaleUnit.BYTE ) ).isEqualTo( "999 B" );
        assertThat( format.format( _999_bytes, ScaleUnit.KILOBYTE ) ).isEqualTo( "1.0 kB" );
        assertThat( format.format( _999_bytes, ScaleUnit.MEGABYTE ) ).isEqualTo( "0 MB" );
        assertThat( format.format( _999_bytes, ScaleUnit.GIGABYTE ) ).isEqualTo( "0 GB" );

        long _1000_bytes = 1000L;
        assertThat( format.format( _1000_bytes ) ).isEqualTo( "1.0 kB" );
        assertThat( format.format( _1000_bytes, ScaleUnit.BYTE ) ).isEqualTo( "1000 B" );
        assertThat( format.format( _1000_bytes, ScaleUnit.KILOBYTE ) ).isEqualTo( "1.0 kB" );
        assertThat( format.format( _1000_bytes, ScaleUnit.MEGABYTE ) ).isEqualTo( "0 MB" );
        assertThat( format.format( _1000_bytes, ScaleUnit.GIGABYTE ) ).isEqualTo( "0 GB" );

        long _49_kilobytes = 49L * 1000L;
        assertThat( format.format( _49_kilobytes ) ).isEqualTo( "49 kB" );
        assertThat( format.format( _49_kilobytes, ScaleUnit.BYTE ) ).isEqualTo( "49000 B" );
        assertThat( format.format( _49_kilobytes, ScaleUnit.KILOBYTE ) ).isEqualTo( "49 kB" );
        assertThat( format.format( _49_kilobytes, ScaleUnit.MEGABYTE ) ).isEqualTo( "0 MB" );
        assertThat( format.format( _49_kilobytes, ScaleUnit.GIGABYTE ) ).isEqualTo( "0 GB" );

        long _50_kilobytes = 50L * 1000L;
        assertThat( format.format( _50_kilobytes ) ).isEqualTo( "50 kB" );
        assertThat( format.format( _50_kilobytes, ScaleUnit.BYTE ) ).isEqualTo( "50000 B" );
        assertThat( format.format( _50_kilobytes, ScaleUnit.KILOBYTE ) ).isEqualTo( "50 kB" );
        assertThat( format.format( _50_kilobytes, ScaleUnit.MEGABYTE ) ).isEqualTo( "0.1 MB" );
        assertThat( format.format( _50_kilobytes, ScaleUnit.GIGABYTE ) ).isEqualTo( "0 GB" );

        long _999_kilobytes = 999L * 1000L;
        assertThat( format.format( _999_kilobytes ) ).isEqualTo( "999 kB" );
        assertThat( format.format( _999_kilobytes, ScaleUnit.BYTE ) ).isEqualTo( "999000 B" );
        assertThat( format.format( _999_kilobytes, ScaleUnit.KILOBYTE ) ).isEqualTo( "999 kB" );
        assertThat( format.format( _999_kilobytes, ScaleUnit.MEGABYTE ) ).isEqualTo( "1.0 MB" );
        assertThat( format.format( _999_kilobytes, ScaleUnit.GIGABYTE ) ).isEqualTo( "0 GB" );

        long _1000_kilobytes = 1000L * 1000L;
        assertThat( format.format( _1000_kilobytes ) ).isEqualTo( "1.0 MB" );
        assertThat( format.format( _1000_kilobytes, ScaleUnit.BYTE ) ).isEqualTo( "1000000 B" );
        assertThat( format.format( _1000_kilobytes, ScaleUnit.KILOBYTE ) ).isEqualTo( "1000 kB" );
        assertThat( format.format( _1000_kilobytes, ScaleUnit.MEGABYTE ) ).isEqualTo( "1.0 MB" );
        assertThat( format.format( _1000_kilobytes, ScaleUnit.GIGABYTE ) ).isEqualTo( "0 GB" );

        long _49_megabytes = 49L * 1000L * 1000L;
        assertThat( format.format( _49_megabytes ) ).isEqualTo( "49 MB" );
        assertThat( format.format( _49_megabytes, ScaleUnit.BYTE ) ).isEqualTo( "49000000 B" );
        assertThat( format.format( _49_megabytes, ScaleUnit.KILOBYTE ) ).isEqualTo( "49000 kB" );
        assertThat( format.format( _49_megabytes, ScaleUnit.MEGABYTE ) ).isEqualTo( "49 MB" );
        assertThat( format.format( _49_megabytes, ScaleUnit.GIGABYTE ) ).isEqualTo( "0 GB" );

        long _50_megabytes = 50L * 1000L * 1000L;
        assertThat( format.format( _50_megabytes ) ).isEqualTo( "50 MB" );
        assertThat( format.format( _50_megabytes, ScaleUnit.BYTE ) ).isEqualTo( "50000000 B" );
        assertThat( format.format( _50_megabytes, ScaleUnit.KILOBYTE ) ).isEqualTo( "50000 kB" );
        assertThat( format.format( _50_megabytes, ScaleUnit.MEGABYTE ) ).isEqualTo( "50 MB" );
        assertThat( format.format( _50_megabytes, ScaleUnit.GIGABYTE ) ).isEqualTo( "0.1 GB" );

        long _999_megabytes = 999L * 1000L * 1000L;
        assertThat( format.format( _999_megabytes ) ).isEqualTo( "999 MB" );
        assertThat( format.format( _999_megabytes, ScaleUnit.BYTE ) ).isEqualTo( "999000000 B" );
        assertThat( format.format( _999_megabytes, ScaleUnit.KILOBYTE ) ).isEqualTo( "999000 kB" );
        assertThat( format.format( _999_megabytes, ScaleUnit.MEGABYTE ) ).isEqualTo( "999 MB" );
        assertThat( format.format( _999_megabytes, ScaleUnit.GIGABYTE ) ).isEqualTo( "1.0 GB" );

        long _1000_megabytes = 1000L * 1000L * 1000L;
        assertThat( format.format( _1000_megabytes ) ).isEqualTo( "1.0 GB" );
        assertThat( format.format( _1000_megabytes, ScaleUnit.BYTE ) ).isEqualTo( "1000000000 B" );
        assertThat( format.format( _1000_megabytes, ScaleUnit.KILOBYTE ) ).isEqualTo( "1000000 kB" );
        assertThat( format.format( _1000_megabytes, ScaleUnit.MEGABYTE ) ).isEqualTo( "1000 MB" );
        assertThat( format.format( _1000_megabytes, ScaleUnit.GIGABYTE ) ).isEqualTo( "1.0 GB" );
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