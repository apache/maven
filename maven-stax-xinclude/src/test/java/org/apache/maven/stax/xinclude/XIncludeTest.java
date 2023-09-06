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
package org.apache.maven.stax.xinclude;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.io.EscapingWriterFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XIncludeTest {

    @Test
    void testBasicInclusion() throws Exception {
        String input = "<?xml version='1.0'?>\n" + "<document xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n"
                + "  <p>120 Mz is adequate for an average home user.</p>\n"
                + "  <xi:include href=\"disclaimer.xml\"/>\n"
                + "</document>";
        Map<String, String> includes = Collections.singletonMap(
                "http://www.example.com/disclaimer.xml",
                "<?xml version='1.0'?>\n" + "<disclaimer>\n"
                        + "  <p>The opinions represented herein represent those of the individual\n"
                        + "  and should not be interpreted as official policy endorsed by this\n"
                        + "  organization.</p>\n"
                        + "</disclaimer>");
        String expected = "<?xml version='1.0'?>\n" + "<document xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n"
                + "  <p>120 Mz is adequate for an average home user.</p>\n"
                + "  <disclaimer xml:base=\"http://www.example.com/disclaimer.xml\">\n"
                + "  <p>The opinions represented herein represent those of the individual\n"
                + "  and should not be interpreted as official policy endorsed by this\n"
                + "  organization.</p>\n"
                + "</disclaimer>\n"
                + "</document>";

        assertXInclude(input, includes, expected);
    }

    @Test
    void testTextualInclusion() throws Exception {
        String input = "<?xml version='1.0'?>\n" + "<document xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n"
                + "  <p>This document has been accessed\n"
                + "  <xi:include href=\"count.txt\" parse=\"text/plain\"/> times.</p>\n"
                + "</document>";
        Map<String, String> includes = Collections.singletonMap("http://www.example.com/count.txt", "324387");
        String expected = "<?xml version='1.0'?>\n" + "<document xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n"
                + "  <p>This document has been accessed\n"
                + "  324387 times.</p>\n"
                + "</document>";

        assertXInclude(input, includes, expected);
    }

    @Test
    void testTextualInclusionOfXml() throws Exception {
        String input = "<?xml version='1.0'?>\n" + "<document xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n"
                + "  <p>The following is the source of the \"data.xml\" resource:</p>\n"
                + "  <example><xi:include href=\"data.xml\" parse=\"text/plain\"/></example>\n"
                + "</document>";
        Map<String, String> includes = Collections.singletonMap(
                "http://www.example.com/data.xml",
                "<?xml version='1.0'?>\n" + "<data>\n" + "  <item><![CDATA[Brooks & Shields]]></item>\n" + "</data>");
        String expected = "<?xml version='1.0'?>\n" + "<document xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n"
                + "  <p>The following is the source of the \"data.xml\" resource:</p>\n"
                + "  <example>&lt;?xml version='1.0'?&gt;\n"
                + "&lt;data&gt;\n"
                + "  &lt;item&gt;&lt;![CDATA[Brooks &amp; Shields]]&gt;&lt;/item&gt;\n"
                + "&lt;/data&gt;</example>\n"
                + "</document>";

        assertXInclude(input, includes, expected);
    }

    @Test
    void testFragmentInclusion() throws Exception {
        String input = "<?xml version='1.0'?>\n" + "<price-quote xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n"
                + "  <prepared-for>Joe Smith</prepared-for>\n"
                + "  <good-through>20040930</good-through>\n"
                + "  <xi:include href=\"price-list.xml\" xpointer=\"w002-description\"/>\n"
                + "  <volume>40</volume>\n"
                + "  <xi:include href=\"price-list.xml\" xpointer=\"element(w002-prices/2)\"/>\n"
                + "</price-quote>";
        Map<String, String> includes = new HashMap<>();
        includes.put(
                "http://www.example.com/price-list.dtd",
                "<!ATTLIST item id ID #REQUIRED> " + "<!ATTLIST description id ID #REQUIRED> "
                        + "<!ATTLIST prices id ID #REQUIRED> ");
        includes.put(
                "http://www.example.com/price-list.xml",
                "<?xml version='1.0'?>\n" + "<!DOCTYPE price-list SYSTEM \"price-list.dtd\">\n"
                        + "<price-list xml:lang=\"en-us\">\n"
                        + "  <item id=\"w001\">\n"
                        + "    <description id=\"w001-description\">\n"
                        + "      <p>Normal Widget</p>\n"
                        + "    </description>\n"
                        + "    <prices id=\"w001-prices\">\n"
                        + "      <price currency=\"USD\" volume=\"1+\">39.95</price>\n"
                        + "      <price currency=\"USD\" volume=\"10+\">34.95</price>\n"
                        + "      <price currency=\"USD\" volume=\"100+\">29.95</price>\n"
                        + "    </prices>\n"
                        + "  </item>\n"
                        + "  <item id=\"w002\">\n"
                        + "    <description id=\"w002-description\">\n"
                        + "      <p>Super-sized widget with bells <i>and</i> whistles.</p>\n"
                        + "    </description>\n"
                        + "    <prices id=\"w002-prices\">\n"
                        + "      <price currency=\"USD\" volume=\"1+\">59.95</price>\n"
                        + "      <price currency=\"USD\" volume=\"10+\">54.95</price>\n"
                        + "      <price currency=\"USD\" volume=\"100+\">49.95</price>\n"
                        + "    </prices>\n"
                        + "  </item>\n"
                        + "</price-list>");
        String expected = "<?xml version='1.0'?>\n" + "<price-quote xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n"
                + "  <prepared-for>Joe Smith</prepared-for>\n"
                + "  <good-through>20040930</good-through>\n"
                + "  <description id=\"w002-description\" xml:base=\"http://www.example.com/price-list.xml\" xml:lang=\"en-us\">\n"
                + "      <p>Super-sized widget with bells <i>and</i> whistles.</p>\n"
                + "    </description>\n"
                + "  <volume>40</volume>\n"
                + "  <price currency=\"USD\" volume=\"10+\" xml:base=\"http://www.example.com/price-list.xml\" xml:lang=\"en-us\">54.95</price>\n"
                + "</price-quote>";

        assertXInclude(input, includes, expected);
    }

    @Test
    void testTextualInclusionWithFragment() throws Exception {
        String input1 = "<?xml version='1.0'?>\n" + "<document xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n"
                + "<p>This example includes just the ‘use' lines from a Perl script.</p>\n"
                + "<pre><xi:include parse=\"text/plain\" fragid=\"line=2,6\" href=\"code.pl\"/></pre>\n"
                + "<p>There are four of them.</p>\n"
                + "</document>";
        Map<String, String> includes = Collections.singletonMap(
                "http://www.example.com/code.pl",
                "#!/usr/bin/perl -- # --*-Perl-*--\n" + "\n"
                        + "use strict;\n"
                        + "use English;\n"
                        + "use Getopt::Std;\n"
                        + "use vars qw($opt_p $opt_q $opt_u $opt_m);\n"
                        + "\n"
                        + "my $usage = \"Usage: $0 [-q] [-u|-p|-m] file [ file ... ]\\n\";\n"
                        + "\n"
                        + "die $usage if ! getopts('qupm');\n"
                        + "\n"
                        + "die $usage if ($opt_p + $opt_u + $opt_m) != 1;\n"
                        + "\n"
                        + "my $file = shift @ARGV || die $usage;\n"
                        + "\n"
                        + "my $opt = '-u' if $opt_u;\n"
                        + "$opt = '-p' if $opt_p;\n"
                        + "$opt = '-m' if $opt_m;\n"
                        + "\n"
                        + "while ($file) {\n"
                        + "    print \"Converting $file to $opt linebreaks.\\n\" if !$opt_q;\n"
                        + "    open (F, \"$file\");\n"
                        + "    binmode F;\n"
                        + "    read (F, $_, -s $file);\n"
                        + "    close (F);\n"
                        + "\n"
                        + "    s/\\r\\n/\\n/sg;\n"
                        + "    s/\\r/\\n/sg;\n"
                        + "\n"
                        + "    if ($opt eq '-p') {\n"
                        + "\ts/\\n/\\r\\n/sg;\n"
                        + "    } elsif ($opt eq '-m') {\n"
                        + "\ts/\\n/\\r/sg;\n"
                        + "    }\n"
                        + "\n"
                        + "    open (F, \">$file\");\n"
                        + "    binmode F;\n"
                        + "    print F $_;\n"
                        + "    close (F);\n"
                        + "\n"
                        + "    $file = shift @ARGV;\n"
                        + "}");
        String expected1 = "<?xml version='1.0'?>\n" + "<document xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n"
                + "<p>This example includes just the ‘use' lines from a Perl script.</p>\n"
                + "<pre>use strict;\n"
                + "use English;\n"
                + "use Getopt::Std;\n"
                + "use vars qw($opt_p $opt_q $opt_u $opt_m);\n"
                + "</pre>\n"
                + "<p>There are four of them.</p>\n"
                + "</document>";

        assertXInclude(input1, includes, expected1);

        String input2 = "<?xml version='1.0'?>\n" + "<document xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n"
                + "<p>This example includes a range of characters.</p>\n"
                + "<pre><xi:include parse=\"text/plain\" fragid=\"char=100,200;length=758,UTF-8\" href=\"code.pl\"/></pre>\n"
                + "</document>";
        String expected2 = "<?xml version='1.0'?>\n" + "<document xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n"
                + "<p>This example includes a range of characters.</p>\n"
                + "<pre>_q $opt_u $opt_m);\n"
                + "\n"
                + "my $usage = \"Usage: $0 [-q] [-u|-p|-m] file [ file ... ]\\n\";\n"
                + "\n"
                + "die $usage if ! ge</pre>\n"
                + "</document>";

        assertXInclude(input2, includes, expected2);
    }

    @Test
    void testAttributeCopying() throws Exception {
        String input = "<?xml version='1.0'?>\n"
                + "<document xmlns:xi=\"http://www.w3.org/2001/XInclude\" xmlns:eg=\"http://www.example.com/namespace/example\">\n"
                + "<p>This example includes a “definition” paragraph from some document\n"
                + "twice using attribute copying.</p>\n"
                + "\n"
                + "<xi:include eg:root=\"one\" href=\"src.xml\" xpointer=\"element(def)\"/>\n"
                + "\n"
                + "<xi:include eg:root=\"two\" href=\"src.xml\" xpointer=\"element(def)\"/>\n"
                + "\n"
                + "</document>";
        Map<String, String> includes = Collections.singletonMap(
                "http://www.example.com/src.xml",
                "<document>\n" + "  <para>Some paragraph.</para>\n"
                        + "  <para xml:id=\"def\">Some definition.</para>\n"
                        + "  <para>Some other paragraph.</para>\n"
                        + "</document>");
        String expected = "<?xml version='1.0'?>\n"
                + "<document xmlns:xi=\"http://www.w3.org/2001/XInclude\" xmlns:eg=\"http://www.example.com/namespace/example\">\n"
                + "<p>This example includes a “definition” paragraph from some document\n"
                + "twice using attribute copying.</p>\n"
                + "\n"
                + "<para eg:root=\"one\" xml:base=\"http://www.example.com/src.xml\" xml:id=\"def\">Some definition.</para>\n"
                + "\n"
                + "<para eg:root=\"two\" xml:base=\"http://www.example.com/src.xml\" xml:id=\"def\">Some definition.</para>\n"
                + "\n"
                + "</document>";

        assertXInclude(input, includes, expected);
    }

    @Test
    void testAttributeCopying2() throws Exception {
        String input = "<?xml version='1.0'?>\n"
                + "<document xmlns:xi=\"http://www.w3.org/2001/XInclude\" xmlns:eg=\"http://www.example.com/namespace/example\">\n"
                + "<p>This example shows attribute replacement.</p>\n"
                + "\n"
                + "<xi:include eg:root=\"one\" set-xml-id=\"inc1\" href=\"src-2.xml\" xpointer=\"element(note)\"/>\n"
                + "\n"
                + "<xi:include eg:root=\"two\" set-xml-id=\"inc2\" href=\"src-2.xml\" xpointer=\"element(note)\"/>\n"
                + "\n"
                + "</document>";
        Map<String, String> includes = Collections.singletonMap(
                "http://www.example.com/src-2.xml",
                "<document>\n"
                        + "  <note xml:id=\"note\"><p>Consider the <phrase xml:id=\"wombat\">Wombat</phrase>.</p></note>\n"
                        + "</document>");
        String expected = "<?xml version='1.0'?>\n"
                + "<document xmlns:xi=\"http://www.w3.org/2001/XInclude\" xmlns:eg=\"http://www.example.com/namespace/example\">\n"
                + "<p>This example shows attribute replacement.</p>\n"
                + "\n"
                + "<note eg:root=\"one\" xml:base=\"http://www.example.com/src-2.xml\" xml:id=\"inc1\"><p>Consider the <phrase xml:id=\"wombat\">Wombat</phrase>.</p></note>\n"
                + "\n"
                + "<note eg:root=\"two\" xml:base=\"http://www.example.com/src-2.xml\" xml:id=\"inc2\"><p>Consider the <phrase xml:id=\"wombat\">Wombat</phrase>.</p></note>\n"
                + "\n"
                + "</document>";

        assertXInclude(input, includes, expected);
    }

    @Test
    void testFallback() throws Exception {
        String input = "<?xml version='1.0'?>\n" + "<div>\n"
                + "  <xi:include href=\"example.txt\" parse=\"text\" xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n"
                + "    <xi:fallback><xi:include href=\"fallback-example.txt\" parse=\"text/plain\">\n"
                + "        <xi:fallback><a href=\"mailto:bob@example.org\">Report error</a></xi:fallback>\n"
                + "      </xi:include></xi:fallback>\n"
                + "  </xi:include>\n"
                + "</div>";
        Map<String, String> includes = Collections.emptyMap();
        String expected = "<?xml version='1.0'?>\n" + "<div>\n"
                + "  <a href=\"mailto:bob@example.org\">Report error</a>\n"
                + "</div>";

        assertXInclude(input, includes, expected);
    }

    private void assertXInclude(String input, Map<String, String> includes, String expected) throws Exception {
        WstxInputFactory factory = new WstxInputFactory();
        WstxOutputFactory outputFactory = new WstxOutputFactory();
        factory.setProperty(XMLInputFactory2.P_REPORT_PROLOG_WHITESPACE, true);
        factory.setXMLResolver((publicID, systemID, baseURI, namespace) -> {
            String r = URI.create(baseURI).resolve(systemID).toString();
            String text = includes.get(r);
            if (text == null) {
                return null;
            }
            return new StreamSource(new ByteArrayInputStream(text.getBytes()), r);
        });

        XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(input));
        XMLStreamReader xiReader =
                new XIncludeStreamReader(factory, outputFactory, "http://www.example.com/pom.xml", reader);

        XMLEventReader er = factory.createXMLEventReader(xiReader);
        StringWriter sw = new StringWriter();
        outputFactory.setProperty(XMLOutputFactory2.P_TEXT_ESCAPER, new EscapingWriterFactory() {
            @Override
            public Writer createEscapingWriterFor(Writer writer, String s) throws UnsupportedEncodingException {
                return new Writer() {
                    @Override
                    public void write(char[] cbuf, int off, int len) throws IOException {
                        for (int i = 0; i < len; i++) {
                            char ch = cbuf[off + i];
                            switch (ch) {
                                case '<':
                                    writer.write("&lt;");
                                    break;
                                case '>':
                                    writer.write("&gt;");
                                    break;
                                case '&':
                                    writer.write("&amp;");
                                    break;
                                default:
                                    writer.write(ch);
                                    break;
                            }
                        }
                    }

                    @Override
                    public void flush() throws IOException {
                        writer.flush();
                    }

                    @Override
                    public void close() throws IOException {
                        writer.close();
                    }
                };
            }

            @Override
            public Writer createEscapingWriterFor(OutputStream output, String s) throws UnsupportedEncodingException {
                return new Writer() {
                    @Override
                    public void write(char[] cbuf, int off, int len) throws IOException {
                        for (int i = 0; i < len; i++) {
                            char ch = cbuf[off + i];
                            switch (ch) {
                                case '<':
                                    output.write("&lt;".getBytes());
                                    break;
                                case '>':
                                    output.write("&gt;".getBytes());
                                    break;
                                case '&':
                                    output.write("&amp;".getBytes());
                                    break;
                                default:
                                    output.write(ch);
                                    break;
                            }
                        }
                    }

                    @Override
                    public void flush() throws IOException {
                        output.flush();
                    }

                    @Override
                    public void close() throws IOException {
                        output.close();
                    }
                };
            }
        });
        XMLEventWriter ew = outputFactory.createXMLEventWriter(sw);
        while (er.hasNext()) {
            ew.add(er.nextEvent());
        }

        assertEquals(expected, sw.toString());
    }
}
