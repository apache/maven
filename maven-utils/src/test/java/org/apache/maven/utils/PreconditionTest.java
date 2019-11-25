package org.apache.maven.utils;

import org.junit.jupiter.api.Test;

import static org.apache.maven.utils.Precondition.notBlank;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PreconditionTest
{
    @Test
    void first()
    {
        assertThatIllegalArgumentException()
                .isThrownBy( () -> notBlank( "x", "Message" ) );
    }
}