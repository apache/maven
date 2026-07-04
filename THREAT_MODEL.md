<!-- SPDX-License-Identifier: Apache-2.0
     https://www.apache.org/legal/release-policy.html -->

# Apache Maven — Umbrella Threat Model (v0 DRAFT)

## §1 Header

- **Project family:** Apache Maven (build tool core, runtime, resolver, and the maintained plugin set). This is an **umbrella** threat model covering ~26 repositories across ~34 branch-targets — see §2. Individual repos/branches inherit this model except where a §2 row narrows it.
- **Modeled against:** the current `master`/maintenance branches of the in-scope repositories as of the date below. Because Maven is mid-transition from the 3.x to the 4.x runtime line, this model carries a **3.x-vs-4.x axis** as a first-class distinction rather than describing a single profile (see §2, §4, §5a, §6).
- **Date:** 2026-07-04
- **Author:** ASF Security team, drafted via the threat-model-producer (Scovetta) rubric at the Maven PMC's request (path 3 — Security team drafts, Maven PMC reviews).
- **Status:** **v0 DRAFT — for Maven PMC review.** Not yet ratified. Most claims are *(inferred)* and each carries a matching open question in §14.
- **Version binding:** the threat model is versioned alongside the projects. A report against a released Maven core / plugin version *N* is triaged against the model as it stood at *N*, not at `master` HEAD. Each branch-target (§2) binds to the releases cut from that branch. See §14 Q20.
- **Reporting cross-reference:** findings that fall under §8 (claimed properties) should be reported privately per the ASF process at <https://maven.apache.org/security.html> / <https://www.apache.org/security/>. Findings that fall under §3 (out of scope) or §9 (properties not provided) — including "a plugin executed code", "the published POM differs from the source POM", or "checksums do not authenticate the publisher" — will be closed citing this document.
- **Provenance legend:** every non-trivial claim carries exactly one tag:
  - *(documented)* — stated in Maven's own docs/site; cited inline.
  - *(maintainer)* — stated by a Maven PMC member in response to this process. (None yet — v0.)
  - *(inferred)* — reasoned from Maven's architecture, domain knowledge, or the absence of a feature; **not yet confirmed.** Each *(inferred)* tag names the §14 question that must ratify it, e.g. *(inferred, Q5)*.
- **Draft confidence:** ~26 documented / 0 maintainer / ~59 inferred. This is a react-to-me draft, not a ratified model — the heavy *(inferred)* weighting is expected for a v0 the PMC has not yet reviewed.

**What Maven is.** Apache Maven is a build-automation and dependency-management tool for JVM projects. Given a project description in `pom.xml` (the Project Object Model), Maven resolves declared dependencies and build **plugins** from configured **repositories** into a local repository (`~/.m2/repository`), then executes a lifecycle of plugin goals — compiling, testing, packaging, signing, and deploying code. Plugins and build **extensions** are ordinary JVM artifacts that Maven downloads and executes **as arbitrary code in the build JVM**. Maven is invoked from the CLI (`mvn`, or the `mvnd` daemon, or a project-local `mvnw` wrapper) by a developer or a CI runner. Its security model is therefore fundamentally a **supply-chain and arbitrary-code-execution** model, and — by explicit design — Maven does not sandbox the code it is asked to build or the plugins it is asked to run.

---

## §2 Scope and intended use

**Primary intended use.** Building, testing, packaging, and publishing JVM software from a trusted `pom.xml` in a developer or CI environment, resolving dependencies and plugins from repositories the operator has chosen to configure and trust. *(documented — security.html: "the Maven security model assumes you trust the `pom.xml` and the code, dependencies and repositories that are used in your build".)*

**Caller roles.** Unlike a network service, Maven has no anonymous client. The roles are:
- **Build author / operator** — writes or vendors the `pom.xml`, `settings.xml`, and `.mvn/` config; chooses repositories; runs `mvn`. **Trusted** — this actor has already chosen what code to execute. *(inferred, Q10)*
- **Dependency / plugin / extension author** — a third party whose artifact is resolved into the build and executed. **Semi-trusted adversary in scope** for supply-chain threats (see §7). *(inferred, Q10)*
- **Repository / mirror operator** — serves artifacts and metadata. **Semi-trusted adversary in scope** (compromise, poisoning, MITM on plaintext transport). *(inferred, Q10)*

**The 3.x-vs-4.x axis (carried on this table).** Maven is mid-transition. Most plugin `master` branches still compile and run against the **Maven 3.9.x** API; seven "split" plugins have moved `master` to the **Maven 4** API and keep a `*-3.x` maintenance branch on the 3.9.x API. The runtime line changes the trust surface (consumer-POM transform, `mvnenc`, resolver 2.x, `mvnup` — all Maven-4-only; see §6/§9). Each branch-target is therefore tagged with the **Maven API line** it targets, and a finding is triaged against **that** line's surface.

| # | Repository / component | Branch-target(s) | Maven API line | Touches outside process | In model? |
| --- | --- | --- | --- | --- | --- |
| **Core & runtime** | | | | | |
| 1 | `maven` (core) | `master` (4.0.x); `maven-3.9.x`/3.10.x maint. | **both** | reads POM/settings/`~/.m2`, spawns plugin code, network resolve | **yes** |
| 2 | `maven-resolver` | `master` (2.x); `1.9.x` maint. | **both** | HTTP(S) transport, local-repo I/O | **yes** |
| 3 | `maven-mvnd` (daemon) | `master` | 4.x-oriented | long-lived JVM, sockets, filesystem | **yes** |
| 4 | `maven-wrapper` | `master` | line-agnostic | downloads + executes a Maven distribution | **yes** |
| 5 | `maven-build-cache-extension` | `master` | 4.x-oriented | reads/writes build-output cache | **yes** |
| **Single-line master plugins (Maven 3.9.x API)** | | | | | |
| 6 | `maven-surefire` (surefire + failsafe) | `master` | 3.x | forks test JVMs, runs test code | **yes** |
| 7 | `maven-javadoc-plugin` | `master` | 3.x | forks `javadoc`, unpacks archives | **yes** |
| 8 | `maven-dependency-plugin` | `master` | 3.x | resolves + unpacks artifacts | **yes** |
| 9 | `maven-checkstyle-plugin` | `master` | 3.x | reads source, resolves rulesets | **yes** |
| 10 | `maven-release-plugin` | `master` | 3.x | SCM writes, invokes nested Maven | **yes** |
| 11 | `maven-shade-plugin` | `master` | 3.x | rewrites/merges JAR bytecode | **yes** |
| 12 | `maven-assembly-plugin` | `master` | 3.x | reads/writes archives | **yes** |
| 13 | `maven-scm` | `master` | 3.x | invokes SCM clients (git/svn/…) | **yes** |
| 14 | `maven-site-plugin` | `master` | 3.x | renders site, resolves skins/reports | **yes** |
| 15 | `maven-enforcer` | `master` | 3.x | evaluates rules over the build | **yes** |
| 16 | `maven-archetype` | `master` | 3.x | scaffolds projects from templates | **yes** |
| 17 | `maven-resolver-ant-tasks` | `master` | 3.x | Ant-side resolution/transport | **yes** |
| 18 | `maven-indexer` | `master` | 3.x | parses repository index metadata | **yes** |
| **Split plugins — master on Maven 4 API + `*-3.x` maintenance** | | | | | |
| 19 | `maven-compiler-plugin` | `master` (M4); `*-3.x` | **both (two targets)** | forks/embeds compiler, reads source | **yes** |
| 20 | `maven-jar-plugin` | `master` (M4); `*-3.x` | **both** | writes JARs | **yes** |
| 21 | `maven-clean-plugin` | `master` (M4); `*-3.x` | **both** | deletes filesystem paths | **yes** |
| 22 | `maven-deploy-plugin` | `master` (M4); `*-3.x` | **both** | uploads artifacts to remote repo | **yes** |
| 23 | `maven-install-plugin` | `master` (M4); `*-3.x` | **both** | writes to local repo | **yes** |
| 24 | `maven-resources-plugin` | `master` (M4); `*-3.x` | **both** | copies/filters resource files | **yes** |
| 25 | `maven-source-plugin` | `master` (M4); `*-3.x` | **both** | packages source JARs | **yes** |

Counting the two targets each for rows 1, 2, and 19–25 yields ~34 branch-targets across ~25 repositories. `mvnup` and `mvnenc` ship inside the Maven 4 core distribution (row 1, `master`) and are in model. See §14 Q3.

---

## §3 Out of scope (explicit non-goals)

- **Parent POMs** (`maven-parent`, `apache` parent, plugin/plugins parents) — configuration aggregation, no runtime surface of their own. *(inferred, Q2)*
- **`maven-studies`** — experimental/incubating code, not a supported product. *(inferred, Q2)*
- **SVN mirrors** of Git repositories — read-only mirrors, no independent surface. *(inferred, Q2)*
- **CI / build infrastructure** (Jenkins jobs, GitHub Actions, ASF Infra) — SDLC/build-hygiene, out of layer per the rubric. *(inferred, Q2)*
- **Project website, skins, and documentation content** (`maven-site`, Fluido/skin projects) — presentation, not the tool. *(inferred, Q2)*
- **Building untrusted code without operator-provided isolation.** Maven does not aim to safely build code it has been told to build. *(documented — security.html: "If you want to use Maven to build untrusted code, it is up to you to provide the required isolation.")*
- **The security of the artifacts a build produces.** Whether *your* code is vulnerable is your project's concern, not Maven's. *(inferred, Q2)*
- **Build/release/SDLC hygiene** of the Maven repos themselves (action pinning, reproducible builds, 2FA) — out of scope per the threat-model rubric.

---

## §4 Trust boundaries and data flow

Maven is not a network service; it is a **local process that pulls remote inputs and executes them.** The meaningful trust transitions are:

1. **Local config → build JVM (trusted → trusted).** `pom.xml`, `settings.xml`, `.mvn/extensions.xml`, `.mvn/maven.config`, and the existing contents of `~/.m2/repository` are treated as **already-authorized by the operator**. Malicious content here is out of model — the operator supplying it has already won. *(inferred, Q5)* The exception the operator must understand: cloning and building an **untrusted** third-party project makes that project's `pom.xml`/`.mvn/` a trusted input to Maven even though the operator did not author it (see §11). *(inferred, Q10)*

2. **Remote repository → local repository (semi-trusted → trusted-on-arrival).** This is the **primary security boundary.** Artifacts and metadata arrive over resolver transport from repositories/mirrors. Maven verifies **transport integrity** (checksums) but does **not**, in core, verify **publisher authenticity** (PGP signatures) — see §8/§9. Once an artifact lands in `~/.m2/repository` it is thereafter treated as trusted local input (transition 1). A poisoned local repo is out of model; poisoning it *over the wire* is in model. *(inferred, Q4, Q5, Q6, Q11)*

3. **Resolved plugin/extension → arbitrary code execution (BY DESIGN).** A resolved plugin, build extension, or `${...}`-driven lifecycle binding runs as arbitrary JVM code in the build. There is **no trust boundary here** — crossing from "declared in POM" to "executing" is the intended behavior, not a violation. *(documented — security.html trust statement.)*

**Maven-4-only transitions (in model only for 4.x branch-targets):**
- **Build POM → consumer POM transform.** At `install`/`deploy`, Maven 4 rewrites the source `pom.xml` into a flattened **consumer POM** that is what downstream actually resolves. The **published POM deliberately differs from the source POM.** *(documented — whatsnewinmaven4: consumer POM is flattened, drops parent references, flattens BOM imports, keeps only compile/runtime transitive deps.)* See §9 / §11a / Q7.
- **`mvnup` → source `pom.xml` rewrite.** The Maven 4 upgrade tool **writes back into the user's `pom.xml`** in place. *(documented — whatsnewinmaven4.)* The write target (the source tree) is a trusted output location; the transform inputs are the trusted POM. *(inferred, Q8)* See Q8.
- **`mvnenc` → encrypted `settings-security`/vault.** Maven 4 reworks settings encryption from Maven 3's obfuscation to real encryption with optional external vault. *(documented — whatsnewinmaven4.)* Key/vault material is outside Maven's boundary (§10). *(inferred, Q9)*

**Reachability precondition (the triager's first test).** A finding is in-model only if it is reachable from a **remote-repository-supplied** artifact/metadata byte (transition 2), from **resolver transport**, or from the **Maven-4 transform surfaces** above — *without* first assuming the operator supplied malicious local config or a pre-poisoned `~/.m2`. A finding that requires attacker control of `pom.xml`/`settings.xml`/`~/.m2` on a build the operator authored is `OUT-OF-MODEL: trusted-input` (§6, §13).

---

## §5 Assumptions about the environment

- **Runtime.** A JVM. Maven 4 core requires **Java 17+**; the 3.x line runs on older JDKs. *(documented — whatsnewinmaven4.)*
- **Operator-controlled machine.** Maven assumes it runs on a host the operator controls; local filesystem, environment variables, and `~/.m2` are trusted. *(inferred, Q5)*
- **Network.** Maven reaches repositories over operator-configured URLs. Plaintext `http://` external repositories are treated as untrusted and are **blocked by default** in the shipped `conf/settings.xml` via the `external:http:*` mirror / `<blocked>` mechanism. *(documented — security.html, CVE-2021-26291 mitigation.)*
- **Clock / entropy.** Not security-load-bearing in core. *(inferred, Q2)*
- **What Maven does to its host (side-effect inventory).** Maven **does** write to `~/.m2/repository` and the project `target/`, open network connections to configured repositories, fork child JVMs/processes (plugins, test runners, compilers, SCM clients), and read environment variables and `settings.xml` (which may contain **plaintext or obfuscated credentials**). `mvnd` additionally runs a **long-lived daemon JVM** reused across builds. These are all intended. *(inferred, Q5, Q12)*

## §5a Build-time and configuration variants (the knobs that move the model)

| Knob | Default | Effect on model | Maintainer stance |
| --- | --- | --- | --- |
| Resolver **checksum policy** (`-C`/`--strict-checksums` vs `-c`/`--lax-checksums`; `checksumPolicy`) | `warn` (lax) | `warn` logs but does **not** fail on checksum mismatch → integrity not enforced by default | to confirm — Q4 |
| PGP **signature verification** | **off** in core (no signature check) | authenticity of publisher unverified unless an extension is added | to confirm — Q6 |
| `maven.consumer.pom.flatten` (M4) | on for install/deploy | published POM ≠ source POM | to confirm — Q7 |
| `settings.xml` credential encryption | Maven 3: obfuscation; Maven 4: `mvnenc` real encryption | master-password/vault protects at-rest creds | to confirm — Q9 |
| `external:http:*` repo blocking | **on** (shipped `settings.xml`) | plaintext external repos rejected | documented (security.html) |
| `mvnd` daemon reuse | on when invoked as `mvnd` | cross-build JVM state reuse | to confirm — Q12 |

**Insecure-default question (Q4, wave 1).** Resolver's default checksum policy is `warn`, not `fail`. Is `warn` the **supported production posture** (so "a corrupted artifact was accepted with only a warning" is `BY-DESIGN`/downstream-responsibility), or is `fail`/`--strict-checksums` the intended posture for anything trust-sensitive (making a silent-accept report `VALID-HARDENING`)? This reshapes §8, §9, §10, §11a, §13 at once.

---

## §6 Assumptions about inputs

Maven's inputs split cleanly into **operator-supplied (trusted)** and **repository-supplied (semi-trusted)**. Where the trust posture differs between the 3.x and 4.x lines, the table has a `Line` column.

| Input | Line | Origin | Attacker-controllable? | Caller/operator must enforce |
| --- | --- | --- | --- | --- |
| `pom.xml` (source/build POM) | both | operator's project | **no** — trusted local config | don't build untrusted projects unsandboxed (§10) *(inferred, Q5)* |
| `settings.xml` (+ `settings-security`) | both | operator `~/.m2` | **no** — trusted; holds credentials | protect the file / key material *(inferred, Q5, Q9)* |
| `.mvn/extensions.xml`, `.mvn/maven.config` | both | operator project | **no** — trusted; loads extensions as code | same as `pom.xml` *(inferred, Q5)* |
| Existing `~/.m2/repository` contents | both | prior builds | **no** — trusted-on-arrival | don't share a poisoned local repo *(inferred, Q11)* |
| **Resolved dependency/plugin/extension bytes** | both | remote repo/mirror | **yes** | checksum policy; add signature verification; pin versions *(inferred, Q4, Q6)* |
| **Repository metadata** (`maven-metadata.xml`, checksums, index) | both | remote repo/mirror | **yes** | resolver parses these before trust is established *(inferred, Q4, Q18)* |
| Resolver **transport** (HTTP responses, redirects, TLS) | both | network/mirror | **yes** | HTTPS; block plaintext external repos *(documented — security.html)* |
| Archive contents unpacked by plugins (zip/tar/jar) | both | remote artifact | **yes** | path-traversal (`zip-slip`) surface in unpack plugins *(documented — security.html Plexus Archiver)* |
| **Consumer-POM transform input** (`mvn deploy`) | **4.x only** | operator project → published | source **no**; the *published* result is a new surface for downstream | verify what you publish matches intent *(inferred, Q7)* |
| **`mvnup` rewrite input** | **4.x only** | operator `pom.xml` | **no** — trusted, but written back in place | review the diff `mvnup` produces *(inferred, Q8)* |
| **`mvnenc` secrets / vault** | **4.x only** | operator | **no** — trusted | manage the master key/vault (§10) *(inferred, Q9)* |

**Size/shape.** Maven imposes no general bound on POM size, dependency-graph depth, or artifact size; resolution of a hostile dependency graph (deep transitive fan-out, decompression of hostile archives) is a resource surface. *(inferred, Q17)*

---

## §7 Adversary model

**In scope:**
- **Malicious dependency, plugin, or extension author** whose artifact is resolved into a build. Capability: run arbitrary code in the build JVM (that part is by design — §9), *and* attempt to exploit resolver/plugin parsing (archive path traversal, metadata parsing) **before/around** that execution, or poison other builds via the shared local repo. *(inferred, Q10)*
- **Compromised or malicious repository / mirror operator.** Capability: serve tampered artifacts, tampered metadata/checksums, or malicious redirects. Bounded by checksum/signature posture (§5a) and HTTPS. *(inferred, Q10)*
- **Network attacker (MITM)** on any plaintext (`http://`) transport the operator has not blocked. *(documented — security.html, CVE-2021-26291/CVE-2013-0253/CVE-2012-6153.)*

**Explicitly out of scope:**
- **The operator running the build**, and anyone with write access to the operator's `pom.xml`, `settings.xml`, `.mvn/`, or `~/.m2`. They have already chosen the code that runs. *(documented — security.html trust statement.)*
- **A local co-tenant with filesystem access** to `~/.m2` or the source tree. *(inferred, Q5)*
- **The author of the project being built**, when the operator has chosen to build that project (see §11 for the untrusted-project misuse). *(inferred, Q10)*

---

## §8 Security properties the project provides

For each: property → violation symptom → severity → provenance.

1. **Transport integrity via checksums.** For each resolved artifact, resolver compares the downloaded bytes against the repository's published checksum. *Symptom of break:* a tampered artifact is accepted as valid. *Severity:* **high** (but note the default policy only **warns** — §5a/Q4). *(inferred, Q4)*
2. **Plaintext-external-repo rejection.** The shipped `conf/settings.xml` blocks `external:http:*` so a plaintext external mirror cannot silently substitute artifacts. *Symptom:* a MITM downgrades resolution to plaintext and substitutes bytes. *Severity:* **high.** *(documented — security.html.)*
3. **Credential-at-rest protection (`mvnenc`, Maven 4).** Server credentials in `settings.xml` can be encrypted (real encryption in 4.x; obfuscation only in 3.x). *Symptom:* plaintext credential recovery from `settings.xml`. *Severity:* **medium** (3.x is obfuscation, not a confidentiality guarantee — §9). *(documented — whatsnewinmaven4.)*
4. **Consumer-POM minimization (Maven 4).** The published consumer POM omits build-internal detail (internal plugin config, non-inherited structure), reducing accidental exposure of internal repository URLs/config to downstream. *Symptom:* internal config leaks into the published POM. *Severity:* **low–medium.** *(documented — whatsnewinmaven4.)*
5. **Resolver API encapsulation (Maven 4).** Resolver 2.x is hidden behind the new Maven API; plugins no longer call it directly, shrinking the plugin-facing transport attack surface. *Symptom:* a plugin drives transport in an unsafe way. *Severity:* **low.** *(documented — whatsnewinmaven4.)*
6. **JVM memory safety.** Maven is managed JVM code; classic memory-corruption (OOB, UAF) is not the threat class. *Symptom:* JVM crash/`OutOfMemoryError` — a robustness bug, not memory unsafety. *Severity:* **correctness-only** unless it enables the above. *(inferred, Q2)*

**No resource-exhaustion guarantee is made.** Maven does not bound POM size, transitive-graph size, archive decompression, or plugin CPU/memory. A hostile dependency graph or archive that exhausts memory/CPU is **not** a §8 violation. *(inferred, Q17)* See §9.

---

## §9 Security properties the project does *not* provide

This is the load-bearing section for triage. State each plainly.

- **No build/plugin sandbox — arbitrary code execution during a build is BY DESIGN.** Maven executes the `pom.xml` it is given, which "commonly includes compiling and running the associated code and using plugins and dependencies". A plugin, extension, test, or lifecycle binding running arbitrary code is **the intended behavior, not a vulnerability.** *(documented — security.html.)* **A scanner reporting "plugin/extension/test executes arbitrary code" is a `BY-DESIGN` non-finding, not a vulnerability.** See §11a.
- **No publisher-authenticity verification in core.** Checksums prove *integrity in transit*, not *who published the artifact*. Maven core/resolver does **not** verify PGP signatures of dependencies or plugins by default; a correctly-checksummed but attacker-published artifact is accepted. Signature verification requires an add-on extension the operator installs. *(inferred, Q6)* **False friend:** a checksum is not a signature and not a MAC — it authenticates nothing about origin.
- **No defense against a poisoned local repository.** Once bytes are in `~/.m2/repository`, they are trusted. Maven does not re-validate them against a remote source on each build. *(inferred, Q11)*
- **Maven 3 credential encryption is obfuscation, not encryption.** The 3.x master-password scheme does not provide confidentiality against an attacker who reads both `settings.xml` and `settings-security.xml`. Use `mvnenc` (4.x). *(documented — whatsnewinmaven4: Maven 3's scheme "more accurately called password obfuscation".)*
- **The published (consumer) POM is deliberately not byte-identical to the source POM (Maven 4).** The transform is a feature. **A finding of the form "the deployed POM does not match the repo `pom.xml`" is `BY-DESIGN`, not tampering.** *(documented — whatsnewinmaven4.)* See §11a.
- **No resource-exhaustion / DoS defense.** No bound on transitive-dependency fan-out, POM/XML entity expansion, or hostile-archive decompression ("zip/tar bombs"). *(inferred, Q17)* Well-known classes Maven leaves to the operator/ecosystem: **dependency-confusion** (name-squatting across repositories), **typosquatting**, **XML entity expansion in POM/metadata parsing**, and **archive path traversal on unpack** (the Plexus Archiver class — *documented*).
- **No guarantee that `mvnup`'s rewrite is safe to apply unreviewed** — it rewrites your `pom.xml`; the operator must review the diff. *(inferred, Q8)*
- **No cross-build isolation for `mvnd`.** The daemon reuses a JVM across builds; it is not an isolation boundary between projects built by the same daemon. *(inferred, Q12)*
- **No integrity guarantee for shared build caches.** `maven-build-cache-extension` keys outputs by hashes; a shared/remote cache is only as trustworthy as its writers. *(inferred, Q14)*
- **No trust anchor in `maven-wrapper`.** `mvnw` downloads and executes whatever Maven distribution `distributionUrl` names; checksum pinning (`distributionSha256Sum`) is optional and operator-supplied. *(inferred, Q13)*

---

## §10 Downstream (operator) responsibilities

For Maven, "downstream" is the **build author / operator / CI owner.**

- **Only build code and POMs you trust** — or provide OS/container isolation yourself. *(documented — security.html.)*
- **Curate your repositories and mirrors.** Prefer HTTPS; keep the shipped `external:http:*` block; use a controlled proxy/mirror rather than arbitrary third-party repos. *(documented — security.html.)*
- **Choose your checksum/signature posture deliberately.** Enable `--strict-checksums` and add signature-verification tooling if your threat model needs publisher authenticity. *(inferred, Q4, Q6)*
- **Pin versions** (avoid version ranges / `LATEST`/`RELEASE` for plugins and dependencies) to reduce dependency-confusion and surprise-upgrade risk. *(inferred, Q17)*
- **Protect `settings.xml` and key material.** Use `mvnenc` (4.x); manage the master password / external vault; never commit credentials. *(inferred, Q9)*
- **Review `mvnup` diffs** before committing the rewritten `pom.xml`. *(inferred, Q8)*
- **Pin the wrapper** (`distributionSha256Sum`, `wrapperSha256Sum`) if you commit `mvnw` into a repo others run. *(inferred, Q13)*
- **Treat shared build caches as a trust boundary** — sign/authenticate cache writers. *(inferred, Q14)*

---

## §11 Known misuse patterns

- **Building an untrusted third-party project unsandboxed.** Cloning a random repo and running `mvn ...` (or opening it in an IDE that auto-imports) executes that project's plugins/extensions as you. *What to do instead:* build unknown projects only inside a disposable container/VM. *(inferred, Q10)*
- **Adding an untrusted `http://` repository or disabling the `external:http:*` block.** Reopens the MITM/substitution vector CVE-2021-26291 closed. *(documented — security.html.)*
- **Relying on Maven 3 password "encryption" as confidentiality.** It is obfuscation; an attacker with both files recovers the secret. *(documented — whatsnewinmaven4.)*
- **Assuming checksums authenticate the publisher.** They only detect transit corruption. *(inferred, Q6)*
- **Running `mvnd` across mutually-distrusting projects and assuming isolation.** The daemon JVM is shared. *(inferred, Q12)*
- **Committing a `mvnw` wrapper with an unpinned `distributionUrl`.** Anyone who moves/poisons that URL controls the Maven that runs. *(inferred, Q13)*

## §11a Known non-findings (recurring false positives)

Feed this list to scanners as a suppression / negative prompt.

- **"A Maven plugin / build extension / test / lifecycle goal executes arbitrary code."** `BY-DESIGN` per §9 — this is Maven's core function, not a vulnerability. Applies to `exec`-style goals, code-generating plugins, `surefire`-forked test code, `shade` bytecode rewriting, etc. *(documented — security.html.)*
- **"The deployed/published POM differs from the source `pom.xml`."** `BY-DESIGN` per §9 (Maven 4 consumer-POM transform). Not tampering. *(documented — whatsnewinmaven4.)*
- **Dependabot / SCA alerts on `test`-scope, `provided`-scope, or integration-test-only dependencies of the Maven repos themselves.** These do not ship in released Maven artifacts and are not reachable in a consuming build → `OUT-OF-MODEL: trusted-input`/unsupported-surface for the Maven product. *(inferred, Q17)*
- **Alerts on transitive dependencies pulled only by the build/test harness** (not by the plugin/core runtime artifact). Same routing. *(inferred, Q17)*
- **"`settings.xml` contains a credential."** Expected — that is what `settings.xml` is for; confidentiality is `mvnenc`'s job at rest, not a finding against the file's existence. *(inferred, Q9)*
- **"Checksum policy defaults to warn, not fail."** Routed by the Q4 ruling — pending confirmation this is `BY-DESIGN`/downstream, not a bug. *(inferred, Q4)*
- **"Maven downloads and runs code from the internet."** `BY-DESIGN` — the resolve-and-execute model per §4/§9. *(documented — security.html.)*

---

## §12 Conditions that would change this model

- Core adopts **default publisher-authenticity verification** (signature checking on by default) — rewrites §8/§9.
- The resolver **default checksum policy** changes from `warn` to `fail` — flips Q4's disposition.
- A new **Maven-4 transform** or tool that reads remote/untrusted input into a rewrite (beyond `mvnup`/consumer-POM).
- The **3.x line reaches EOL** or a split plugin drops its `*-3.x` branch — removes a branch-target column.
- `mvnd` or `build-cache-extension` gains a **network-shared** default surface (remote daemon, remote cache) — adds a network adversary.
- **Any finding that cannot be routed to a §13 disposition** — that is a `MODEL-GAP` and the model must be revised (add the property to §8/§9), not decided ad hoc.

## §13 Triage dispositions

| Disposition | Meaning | Licensed by |
| --- | --- | --- |
| `VALID` | Violates a claimed property via a repo/mirror/transport-controlled input or a Maven-4 transform surface. | §8, §6, §7 |
| `VALID-HARDENING` | No §8 property broken, but a §11 misuse is easy enough that the project elects to harden (e.g. tighten a default). | §11, §5a |
| `OUT-OF-MODEL: trusted-input` | Requires attacker control of `pom.xml`/`settings.xml`/`.mvn/`/`~/.m2` on an operator-authored build. | §6 |
| `OUT-OF-MODEL: adversary-not-in-scope` | Requires the operator, a local co-tenant, or the built project's author (when the operator chose to build it). | §7 |
| `OUT-OF-MODEL: unsupported-component` | Lands in parent POMs, `maven-studies`, site/skins, SVN mirrors, or CI infra. | §3 |
| `OUT-OF-MODEL: non-default-build` | Only manifests when a §5a default is flipped to the less-safe value (e.g. `external:http:*` block removed). | §5a |
| `BY-DESIGN: property-disclaimed` | Plugin/extension/test code execution; published-POM ≠ source-POM; checksum ≠ signature; resolve-and-run. | §9 |
| `KNOWN-NON-FINDING` | Matches a §11a entry (test-scope SCA alerts, `settings.xml`-holds-a-credential, etc.). | §11a |
| `MODEL-GAP` | Cannot be routed above — triggers a model revision. | §12 |

---

## §14 Open questions for the maintainers

Each *(inferred)* tag in the body routes to one question below. Every question states a **proposed answer** to confirm/correct. Grouped in waves.

**Wave 1 — scope & the 3.x/4.x axis (shapes everything else):**
- **Q1.** We treat the **3.x-vs-4.x split** as the primary axis: a finding is triaged against the Maven API line of the specific branch-target, and Maven-4-only surfaces (consumer POM, `mvnenc`, resolver 2.x, `mvnup`) are out of model for 3.x targets. *Proposed: correct.* (→ §2, §4, §6)
- **Q2.** Out-of-scope set — parent POMs, `maven-studies`, SVN mirrors, CI/infra, site/skins, and "the security of the artifacts your build produces" — is right and complete. *Proposed: correct.* (→ §3)
- **Q3.** `mvnup` and `mvnenc` ship inside the Maven 4 core distribution (repo #1 `master`) and are in model as part of core, not as separate products. *Proposed: correct.* (→ §2)
- **Q4.** *(Insecure-default.)* Resolver's default checksum policy is `warn`, not `fail`. Is `warn` the supported posture (silent-accept-on-mismatch = `BY-DESIGN`/downstream), or is `--strict-checksums` intended for trust-sensitive use (silent-accept = `VALID-HARDENING`)? (→ §5a, §8, §9, §11a)

**Wave 2 — inputs, trust, and the Maven-4 surfaces:**
- **Q5.** `pom.xml`, `settings.xml`, `.mvn/` config, and existing `~/.m2` contents are **trusted local inputs**; findings requiring attacker control of them (on an operator-authored build) are out of model. *Proposed: correct.* (→ §4, §6)
- **Q6.** Maven core/resolver does **not** verify PGP publisher signatures by default; authenticity verification is an operator-installed extension. *Proposed: correct — this is a disclaimed property (§9), and "no signature check" is not a core finding.* (→ §8, §9)
- **Q7.** The Maven-4 **consumer/build-POM transform** is by design; "published POM ≠ source POM" is a `BY-DESIGN` non-finding. Are there transform behaviors (e.g. what the consumer POM *retains*) that you *would* consider security-relevant? *Proposed: transform itself is by design; retention of internal repo URLs would be the concern.* (→ §9, §11a)
- **Q8.** `mvnup` rewrites the operator's `pom.xml` in place from trusted inputs; the operator is expected to review the diff. Does `mvnup` ever read **untrusted/remote** input into that rewrite (which would make it an in-model input surface)? *Proposed: inputs are trusted; operator reviews the diff.* (→ §4, §6, §9, §10)
- **Q9.** `mvnenc` provides at-rest credential encryption; master-key/vault management is a downstream responsibility, and "`settings.xml` contains a credential" is not a finding. *Proposed: correct.* (→ §5a, §9, §10, §11a)

**Wave 3 — adversary, resolver, runtime components:**
- **Q10.** Adversary model — in scope: malicious dependency/plugin/extension author, compromised repo/mirror, plaintext-transport MITM; out of scope: operator, local co-tenant, and the built project's author when the operator chose to build it. *Proposed: correct.* (→ §2, §7, §11)
- **Q11.** A **pre-poisoned local repository** (`~/.m2/repository`) is out of model (trusted-on-arrival); poisoning it *over the wire* during resolution is in model. *Proposed: correct.* (→ §4, §9)
- **Q12.** `mvnd` is **not** an isolation boundary between projects built by the same daemon; cross-build state isolation is not a claimed property. *Proposed: correct.* (→ §5, §9, §11)
- **Q13.** `maven-wrapper` executes whatever `distributionUrl` names; checksum pinning is optional/operator-supplied and there is no built-in trust anchor. *Proposed: correct — disclaimed.* (→ §9, §10, §11)
- **Q14.** `maven-build-cache-extension` makes **no integrity guarantee for shared/remote caches**; authenticating cache writers is a downstream responsibility. *Proposed: correct.* (→ §9, §10)

**Wave 4 — plugin branch-targets & non-findings:**
- **Q15.** For the seven **split plugins**, the `*-3.x` branch targets the Maven 3.9 API and shares most code with `master`; a finding present in shared code applies to **both** branch-targets, while a finding in Maven-4-only master code applies only to the 4.x target. *Proposed: correct.* (→ §2)
- **Q16.** The 13 **single-line `master` plugins** require Maven **3.9.x** at runtime despite living on `master` (no `*-3.x` branch needed). *Proposed: correct.* (→ §2)
- **Q17.** Known non-findings for **SCA/Dependabot noise** — `test`/`provided`/IT-only and build-harness-only transitive deps of the Maven repos are not reachable in the shipped product → out of model; and no resource-exhaustion (dependency-graph/XML-entity/archive-bomb) guarantee is made. *Proposed: correct and representative.* (→ §6, §8, §9, §11a)
- **Q18.** `maven-resolver-ant-tasks` and `maven-indexer` inherit the same repo-supplied-metadata trust posture as core resolver (index/metadata parsing is an in-model input). *Proposed: correct.* (→ §2, §6)

**Wave 5 — meta / process:**
- **Q19.** **There is no `SECURITY.md` in `apache/maven` `master`** (verified 404); the documented model lives on `maven.apache.org/security.html`. For the scan to mechanically discover this model, where should the canonical `THREAT_MODEL.md` live (per-repo vs one umbrella), and can an `AGENTS.md → SECURITY.md → THREAT_MODEL.md` discoverability chain be added? *Proposed: one umbrella model, linked from each repo's `SECURITY.md`/`AGENTS.md`.* (→ §1)
- **Q20.** **Version binding** — each branch-target's model binds to the releases cut from that branch; a report against Maven core 3.9.x is triaged against the 3.x profile, not the 4.x `master` surface. *Proposed: correct.* (→ §1)

---

## §15 Machine-readable companion

Deferred for v0. Once the PMC ratifies, emit a `threat-model.yaml` sidecar indexing: branch-target → Maven API line (§2); entry point → parameter trust (§6); §5a knobs + defaults; §8 properties (symptom + severity); §9 disclaimed properties + false friends; §11a non-findings; §13 dispositions. The prose remains canonical.
