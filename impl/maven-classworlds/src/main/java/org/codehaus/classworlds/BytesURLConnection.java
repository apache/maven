/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.codehaus.classworlds;

/*
 * Copyright 2001-2010 Codehaus Foundation.
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * URLConnection implementation for byte arrays.
 *
 * <p><b>Note:</b> This is a legacy class provided for backward compatibility with Maven 2.
 * New code should avoid using this internal implementation detail.</p>
 *
 * @deprecated This is a legacy internal class.
 */
@Deprecated
public class BytesURLConnection extends URLConnection {
    protected final byte[] content;

    protected int offset;

    protected int length;

    public BytesURLConnection(URL url, byte[] content) {
        super(url);
        this.content = content;
    }

    public void connect() {}

    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }
}
