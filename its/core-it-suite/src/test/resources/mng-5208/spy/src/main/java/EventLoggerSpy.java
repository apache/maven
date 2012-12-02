
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.codehaus.plexus.component.annotations.Component;

@Component( role=EventSpy.class )
public class EventLoggerSpy extends AbstractEventSpy {


    @Override
    public void init(Context context) throws Exception {
    }

    @Override
    public void onEvent(Object event) throws Exception {
        if (event instanceof ExecutionEvent) {

            ExecutionEvent executionEvent = (ExecutionEvent) event;
            System.out.println( "executionEvent:" + executionEvent.getType()  + "/" + executionEvent.getProject().getId());
        }
    }
}

