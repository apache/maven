Maven (DefaultMaven) : doExecute
 - add a build timestamp to the request properties
 - validateLocalRepository: validate local repository (check to make sure it exists and is writable)
 - newRepositorySession: create the RepositorySystemSession
 - AbstractLifecycleParticipant.afterSessionStart
 - event: ExecutionEvent.Type.ProjectDiscoveryStarted
 - set the RepositorySystemSession in the ProjectBuildingRequest
 - getProjectsForMavenReactor 
   - this triggers the reading of all the POMs: (NOTE: this could be optimized looking at the POMs if they don't change)
 - create the ReactorReader
 - set RepositorySystemSession.setWorkspaceReader()
 - make the RepositorySystemSession readonly
 ? Can we remove all the @Deprecated methods from MavenSession (4 years, 7 months)
 ? Can we make the MavenSession read-only, do we need a way to share state better 
 - AbstractLifecycleParticipant.afterProjectsRead()
 - lifecycleStarter.execute(session)
 - validateProfiles (NOTE: why does this happen at the end?)

# Things to document
- How the ClassRealms are setup at each stage
- Having meaningful visuals for the lifecycle and stages
- explain forked executions
- remove aggregators, and what are they exactly

# Questions

? forked executions
? aggregators
? All the different resolvers: project, plugin, lifecycle
? Turn if all into JSR330

# Things to refactor

# Isolate project dependency downloads from build execution
- project dependencies are resolved from the mojo executor
- separate dependency resolution from build execution, right now they are intertwined
  - if separated then a calculation can be made for the whole set of dependencies
  - make sure dependency resolution works before the build executes
  - not sure there would be much of a speed impact if one assumes the best speeds will happen when everything is downloaded and the
    conflation of these modes and the complexity it creates is not worth it

- turn all to JSR330 
- use LifecycleModuleBuilder consistently instead of BuilderCommon
- the MavenExecution is calculated in each of the builders instead of once, the implication of this is that project dependency resolution will happen in parallel which means the local repository needs to be safe, and resolution in general.
- the weave builder uses BuilderCommon directly, should be used the same way the other builders work
