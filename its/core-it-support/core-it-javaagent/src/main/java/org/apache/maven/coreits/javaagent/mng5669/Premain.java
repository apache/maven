package org.apache.maven.coreits.javaagent.mng5669;

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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * 
 * @author Robert Scholte
 *
 */
public class Premain
{
    public static void premain( String agentArgs, Instrumentation inst )
    {
        inst.addTransformer( new ClassFileTransformer()
        {

            public byte[] transform( ClassLoader loader, String className, Class<?> classBeingRedefined,
                                     ProtectionDomain protectionDomain, byte[] classfileBuffer )
                throws IllegalClassFormatException
            {
                if ( "org/apache/maven/model/io/DefaultModelReader".equals( className ) )
                {
                    ClassReader r = new ClassReader( classfileBuffer );
                    final ClassWriter w = new ClassWriter( Opcodes.ASM6 );

                    ClassVisitor v = new DefaultModelReaderVisitior( Opcodes.ASM6, w );

                    r.accept( v, ClassReader.EXPAND_FRAMES );
                    return w.toByteArray();
                }
                else
                {
                    return classfileBuffer;
                }
            }
        } );
    }

    private static class DefaultModelReaderVisitior
        extends ClassVisitor
    {
        DefaultModelReaderVisitior( int api, org.objectweb.asm.ClassVisitor cv )
        {
            super( api, cv );
        }

        @Override
        public MethodVisitor visitMethod( int access, String name, String desc, String signature, String[] exceptions )
        {
            MethodVisitor mv = cv.visitMethod( access, name, desc, signature, exceptions );
            if ( "getSource".equals( name ) )
            {
                return new GetSourceMethodAdvice( Opcodes.ASM6, mv, access, name, desc );
            }
            else
            {
                return mv;
            }
        }
    }

    // org.apache.maven.model.io.DefaultModelReader.getSource(Map<String, ?>)
    private static class GetSourceMethodAdvice
        extends AdviceAdapter
    {
        GetSourceMethodAdvice( int api, MethodVisitor mv, int access, String name, String desc )
        {
            super( api, mv, access, name, desc );
        }

        @Override
        protected void onMethodEnter()
        {
            // System.out.println( options ),
            mv.visitFieldInsn( GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;" );
            mv.visitVarInsn( ALOAD, 1 );
            mv.visitMethodInsn( INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false );
        }
    }
}
