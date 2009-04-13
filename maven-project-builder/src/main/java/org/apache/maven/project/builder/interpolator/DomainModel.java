package org.apache.maven.project.builder.interpolator;

import java.io.IOException;
import java.util.List;

public interface DomainModel {
    
    boolean isMostSpecialized();
    
    void setMostSpecialized(boolean isMostSpecialized);
}
