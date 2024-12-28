# Simple reproducer for SettingsBuilderRequest not emitted

- Build this small extension
`./mvnw clean install`
- Use this extension in a project by `.mvn/extensions.xml`:
```xml
<extensions>
    <extension>
        <groupId>org.example</groupId>
        <artifactId>maven4-reproducer</artifactId>
        <version>1.0-SNAPSHOT</version>
    </extension>
</extensions>
```
- Run a build with Maven 4 rc-2:
  `[INFO] [stdout] Closing Simple Event Spy, checking SettingsBuilderRequest event` => all good
- Run a build with Maven 4 latest rc-3 snapshot:
  `[WARNING] Failed to close spy org.example.SimpleEventSpy: No value present` => the SettingsBuilderRequest event is not there