/* Created on Jul 2, 2004 */
package org.apache.maven.decoration;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.Reader;

/**
 * @author jdcasey
 */
public class GoalDecorationParser {
    
    public static final String ROOT_TAG = "decorators";
    public static final String DEFAULT_GOAL_ATTRIBUTE = "defaultGoal";
    
    public static final String PREGOAL_TAG = "preGoal";
    public static final String POSTGOAL_TAG = "postGoal";
    
    public static final String NAME_ATTRIBUTE = "name";
    public static final String ATTAIN_ATTRIBUTE = "attain";

    public GoalDecorationParser() {
    }
    
    public GoalDecoratorBindings parse(Reader reader) throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        
        parser.setInput(reader);
        
        GoalDecoratorBindings bindings = null;
        
        int eventType = parser.getEventType();
        while(eventType != XmlPullParser.END_DOCUMENT) {
            
            if(eventType == XmlPullParser.START_TAG) {
                String tagName = parser.getName();
                if(ROOT_TAG.equals(tagName)) {
                    bindings = new GoalDecoratorBindings();
                    
                    String defaultGoal = parser.getAttributeValue("", DEFAULT_GOAL_ATTRIBUTE);
                    if(defaultGoal != null && defaultGoal.length() > 0) {
                        bindings.setDefaultGoal(defaultGoal);
                    }
                }
                else if(PREGOAL_TAG.equals(tagName)) {
                    String name = parser.getAttributeValue("", NAME_ATTRIBUTE);
                    String attain = parser.getAttributeValue("", ATTAIN_ATTRIBUTE);
                    DefaultGoalDecorator decorator = new DefaultGoalDecorator(name, attain);
                    
                    bindings.addPreGoal(decorator);
                }
                else if(POSTGOAL_TAG.equals(tagName)) {
                    String name = parser.getAttributeValue("", NAME_ATTRIBUTE);
                    String attain = parser.getAttributeValue("", ATTAIN_ATTRIBUTE);
                    DefaultGoalDecorator decorator = new DefaultGoalDecorator(name, attain);
                    
                    bindings.addPostGoal(decorator);
                }
                else {
                    throw new IllegalArgumentException(tagName + " is not allowed.");
                }
            }
            
            eventType = parser.next();
        }
        
        return bindings;
    }

}
