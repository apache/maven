package org.apache.maven.model;

public interface DomainModel {
    
    boolean isMostSpecialized();
    
    void setMostSpecialized(boolean isMostSpecialized);
}
