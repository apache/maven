package org.apache.maven.script.marmalade.tags;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import org.codehaus.marmalade.runtime.MarmaladeExecutionException;

/**
 * @author jdcasey Created on Feb 8, 2005
 */
public class ExecutionStrategyTag
    extends AbstractStringValuedBodyTag
{

    protected void setValue( String value ) throws MarmaladeExecutionException
    {
        MetadataTag metadataTag = (MetadataTag) requireParent( MetadataTag.class );
        metadataTag.setExecutionStrategy( value );
    }

}