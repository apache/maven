module org.apache.maven.its.modular {
    requires org.apache.maven.api;
    requires org.apache.maven.api.di;

    exports org.apache.maven.its.modular;
    opens org.apache.maven.its.modular;
}
