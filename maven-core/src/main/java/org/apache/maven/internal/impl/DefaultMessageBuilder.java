package org.apache.maven.internal.impl;

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

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.MessageBuilder;

@Experimental
public class DefaultMessageBuilder implements MessageBuilder
{
    private final @Nonnull org.apache.maven.shared.utils.logging.MessageBuilder delegate;

    public DefaultMessageBuilder( @Nonnull org.apache.maven.shared.utils.logging.MessageBuilder delegate )
    {
        this.delegate = delegate;
    }

    @Override
    @Nonnull
    public MessageBuilder success( Object o )
    {
        delegate.success( o );
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder warning( Object o )
    {
        delegate.warning( o );
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder failure( Object o )
    {
        delegate.failure( o );
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder strong( Object o )
    {
        delegate.strong( o );
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder mojo( Object o )
    {
        delegate.mojo( o );
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder project( Object o )
    {
        delegate.project( o );
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder a( char[] chars, int i, int i1 )
    {
        delegate.a( chars, i, i1 );
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder a( char[] chars )
    {
        delegate.a( chars );
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder a( CharSequence charSequence, int i, int i1 )
    {
        delegate.a( charSequence, i, i1 );
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder a( CharSequence charSequence )
    {
        delegate.a( charSequence );
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder a( Object o )
    {
        delegate.a( o );
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder newline()
    {
        delegate.newline();
        return this;
    }

    @Override
    @Nonnull
    public MessageBuilder format( String s, Object... objects )
    {
        delegate.format( s, objects );
        return this;
    }
}
