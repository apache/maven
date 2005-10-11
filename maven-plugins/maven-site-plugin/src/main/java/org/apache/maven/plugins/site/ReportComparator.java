package org.apache.maven.plugins.site;

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

import org.apache.maven.reporting.MavenReport;

import java.util.Comparator;
import java.util.Locale;

/**
 * Sorts reports.
 *
 * @todo move to reporting API?
 * @todo allow reports to define their order in some other way?
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class ReportComparator
    implements Comparator
{
    public int compare( Object o1, Object o2 )
    {
        MavenReport r1 = (MavenReport) o1;
        MavenReport r2 = (MavenReport) o2;

        return r1.getName( Locale.getDefault() ).compareTo( r2.getName( Locale.getDefault() ) );
    }
}
