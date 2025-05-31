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
package org.apache.maven.cling.invoker.mvnup.jdom;

import org.codehaus.plexus.util.StringUtils;
import org.jdom2.Comment;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for {@link Content}.
 */
class JDomContentHelper {

    private static final Logger LOG = LoggerFactory.getLogger(JDomContentHelper.class);

    static String contentAsString(Content content) {
        if (content instanceof Element) {
            return elementToString((Element) content);
        }
        if (content instanceof Text) {
            return textToString((Text) content);
        }
        if (null != content) {
            return content.getCType() + " => " + content.getValue().trim();
        }
        return "content is null";
    }

    private static String elementToString(Element element) {
        return element.getCType() + " => <" + element.getName() + "> : "
                + element.getValue().trim().replaceAll("\n", "\\\\n");
    }

    private static String textToString(Text text) {
        String value = text.getValue().replaceAll("\n", "\\\\n");
        if (isNewline(text)) {
            return "New Line: '" + value + "'";
        }
        if (isMultiNewLine(text)) {
            return "Multi New Line: '" + value + "'";
        }
        return text.getCType() + " => " + value;
    }

    /**
     * Check if content represent newlines.
     *
     * @param content the content to check
     * @return boolean Returns true if content has at least one newline
     */
    static boolean hasNewlines(Content content) {
        if (content instanceof Text) {
            Text text = (Text) content;
            String value = text.getValue();
            return StringUtils.countMatches(value, "\n") >= 1 && value.trim().isEmpty();
        }
        return false;
    }

    /**
     * Check if content is one newline, i.e. \n.<br>
     * Maybe followed by an empty string.
     *
     * @param content the content to check
     * @return boolean
     */
    static boolean isNewline(Content content) {
        if (content instanceof Text) {
            Text text = (Text) content;
            String value = text.getValue();
            return StringUtils.countMatches(value, "\n") == 1 && value.trim().isEmpty();
        }
        return false;
    }

    /**
     * Check if content is a multiline text, i.e. \n\n (or more).<br>
     * * Maybe followed by an empty string.
     *
     * @param content the content to check
     * @return boolean
     */
    static boolean isMultiNewLine(Content content) {
        if (content instanceof Text) {
            Text text = (Text) content;
            String value = text.getValue();
            return StringUtils.countMatches(value, "\n") > 1 && value.trim().isEmpty();
        }
        return false;
    }

    static boolean isComment(Content content) {
        return content instanceof Comment;
    }

    static Content getSuccessorOfContentWithIndex(int index, Element parent) {
        return isIndexValid(index + 1, parent) ? parent.getContent(index + 1) : null;
    }

    static Content getPredecessorOfContentWithIndex(int index, Element parent) {
        return isIndexValid(index - 1, parent) ? parent.getContent(index - 1) : null;
    }

    static Content getContentWithIndex(int index, Element parent) {
        return isIndexValid(index, parent) ? parent.getContent(index) : null;
    }

    /**
     * Check if index is a valid regarding the contents of the given element
     *
     * @param index   the index to check
     * @param element the element to get the contents from
     * @return true if index is valid index: <code>element.getContent(index)</code>
     */
    static boolean isIndexValid(int index, Element element) {
        int numberOfContents = element.getContent().size();
        if (index < 0 || numberOfContents == 0 || index >= numberOfContents) {
            LOG.trace("Parent: {} has no content with index {}", JDomContentHelper.contentAsString(element), index);
            return false;
        }
        return true;
    }

    /**
     * Count new lines of new/multiline-predecessors
     *
     * @param content the content to start from
     * @return count of newlines
     */
    static int countNewlinesPredecessors(Content content) {
        int newLineCount = 0;

        Element parent = content.getParentElement();
        int descendantIndex = parent.indexOf(content);
        Content predecessor = JDomContentHelper.getPredecessorOfContentWithIndex(descendantIndex, parent);

        while (JDomContentHelper.hasNewlines(predecessor)) {
            newLineCount += StringUtils.countMatches(predecessor.getValue(), "\n");
            descendantIndex--;
            predecessor = JDomContentHelper.getPredecessorOfContentWithIndex(descendantIndex, parent);
        }

        return newLineCount;
    }
}
