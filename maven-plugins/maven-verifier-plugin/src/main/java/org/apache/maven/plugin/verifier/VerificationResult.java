package org.apache.maven.plugin.verifier;

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

import org.apache.maven.plugin.verifier.model.File;

import java.util.ArrayList;
import java.util.List;

public class VerificationResult
{
    private List existenceFailures = new ArrayList();

    private List nonExistenceFailures = new ArrayList();

    private List contentFailures = new ArrayList();

    public void addExistenceFailure( File file )
    {
        existenceFailures.add( file );
    }

    public void addNonExistenceFailure( File file )
    {
        nonExistenceFailures.add( file );
    }

    public void addContentFailure( File file )
    {
        contentFailures.add( file );
    }

    public List getExistenceFailures()
    {
        return existenceFailures;
    }

    public List getNonExistenceFailures()
    {
        return nonExistenceFailures;
    }

    public List getContentFailures()
    {
        return contentFailures;
    }

    public boolean hasFailures()
    {
        return !getExistenceFailures().isEmpty() || !getNonExistenceFailures().isEmpty() ||
            !getContentFailures().isEmpty();
    }
}
