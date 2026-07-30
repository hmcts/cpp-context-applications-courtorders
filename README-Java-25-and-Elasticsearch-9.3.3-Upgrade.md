# Java 25 / WildFly 40 / Elasticsearch 9.3.3 upgrade — guide for the applications-courtorders team

This branch (`dev/java-25-es-9.2.2`, draft PR **#37**) upgrades applications-courtorders to the **25.104.x** line
(Java 25 / WildFly 40 / Jakarta EE 11) **and** to **Elasticsearch 9.3.3**. It was prepared by the platform/framework
upgrade effort (ticket **PEG-3408**, mirroring the Java-17 ES 9.2 work in **DD-41592**) as the second ES *proving*
context, after unifiedsearch-query.

**The decision to accept, finish and release these changes is yours.** This document explains what we changed, the
decisions we made and why, the gotchas we hit, and exactly what you need to do to take it over the line.

This context is an ES **indexing** context: `applicationscourtorders-event-indexer` transforms `court-order-requested`
events and writes them, via the shared `unifiedsearch-client`, into the crime **`crime_case_index`**. It owns no ES
index mappings of its own.

---

## Why

- Prove Elasticsearch 9.3.3 works on the Java-25 stack for an *indexing* context (unifiedsearch-query covered querying).
- Folded into the July 2026 security-hardening work.

## What changed (summary)

- **Parent** → `service-parent-pom:25.104.0-M9`; **`javax.*` → `jakarta.*`** across all modules;
  `javax:javaee-api` → `jakarta.platform:jakarta.jakartaee-api`; `jakarta.xml.bind-api` override on the RAML
  client-generator plugins; embedded-ES test version `7.16.2` → `9.3.3`; `referencedata.version` `17.104.136` →
  `17.104.137` (interface-version enforcer).
- **DeltaSpike Data → plain JPA** repository rewrite (below) — the biggest change.
- Two **integration-test fixes** (below) — both were test/wiring problems, not application-logic bugs.

---

## Decisions we made — please review these

### 1. DeltaSpike Data → plain JPA (production repository rewrite)

`CourtOrderRepository` was a **DeltaSpike Data `@Repository` interface** (`EntityRepository` + `@Query` methods).
DeltaSpike is end-of-life and **not available under Jakarta EE 11 / CDI 4**, and the 25.104.x parent no longer manages
its versions (the old poms fail at POM-parse time). This is the same migration the other Java-25 contexts (listing,
mi-reportdata) had to make.

We rewrote it as a **concrete `@ApplicationScoped` JPA repository** with `@PersistenceContext EntityManager`. Each
`@Query` method became an explicit `entityManager.createQuery(...)` / `createNativeQuery(...)`, **preserving the exact
original query semantics**; `save()` uses `entityManager.merge(...)`. The two callers (`CourtOrderQueryView`,
`CourtOrderEventListener`) inject it *by type*, so they're unaffected, and their unit tests (which `@Mock` it) still
pass unchanged.

Supporting changes:
- `viewstore-persistence` pom: removed DeltaSpike / OpenEJB / `hibernate-entitymanager` / H2; added
  `org.hibernate.orm:hibernate-core`, `test-utils-hibernate`, `postgresql`.
- `CourtOrderRepositoryTest` (DeltaSpike `CdiTestRunner` + embedded H2) → **`CourtOrderRepositoryIT`** using the
  real-Postgres `HibernateTestEntityManagerProvider`, **gated behind the `applicationscourtorders-integration-test`
  Maven profile** so it does not run in the DB-less PR build.
- `persistence.xml` (prod + test) updated to the **Jakarta 3.0** namespace.

**Please review** the rewritten `CourtOrderRepository` — the queries are 1:1 with the old `@Query` strings, but a
second pair of eyes from the team that owns this domain is worthwhile.

### 2. Indexer now tolerates a court order with no `defendantIds`

`defendantIds` is **optional** on the create-court-order command (it is *not* in the schema's `required` list). The
indexer's `DomainToIndexMapper` used to call `getDefendantIds().forEach(...)` unconditionally, so a court order with no
defendants threw a `NullPointerException`. We added a null-guard: such a court order is indexed **with no defendant
party** rather than crashing. (See gotcha #2 for why this mattered so much.) Covered by a new `DomainToIndexMapperTest`.

---

## Gotchas we hit (so you don't have to)

These two cost the most time. Both were **test/wiring issues, not application bugs** — throughout, the application
returned the correct data (a live probe confirmed the query endpoint returned HTTP 200 with the exact expected content
during the whole poll window). They're written up here because **other teams doing this upgrade will likely hit them.**

### 1. Leftover `org.apache.tomee:openejb-cxf-rs` broke the IT poller

The integration-test module carried a leftover `openejb-cxf-rs` test dependency (from an old OpenEJB harness; nothing
uses it — the ITs run against a deployed WildFly). It put **Apache CXF's** JAX-RS `ClientBuilder` on the test classpath
**alongside RestEasy**. The test poller (`RestPoller` → `RestClient`) uses the generic `ClientBuilder.newClient()`,
which then bound to CXF — whose client silently failed to read the vendor `+json` response body, so **every query poll
returned a null payload and timed out** (all ITs red). unifiedsearch-query was green only because it has RestEasy alone.
**Fix:** removed the dependency. **Rule of thumb:** keep RestEasy the *sole* JAX-RS client on IT classpaths.

### 2. An indexer exception stalls the *shared* ingestion stream (this was the "flaky" test)

The EVENT_INDEXER processes **all** `court-order-requested` events — including those created by `CourtOrderIT`, not just
the ingester tests. One `CourtOrderIT` fixture creates a court order with **no `defendantIds`**, which hit the NPE in
decision #2. Because event **self-healing** is enabled, the failing event is retried in a way that **stalls the shared
`crime_case_index` ingestion stream**, so an *unrelated* ingester test's Elasticsearch `findBy` polls intermittently
timed out (it passed when run alone, flaked in the full suite). Fixing the NPE removed the stall.

**General lesson:** if an ingester IT flakes with a `findBy` timeout, look in `server.log` for an **indexer exception
stalling the stream** — it's usually a bad/edge-case event payload, not a fault in the test being verified.

---

## Test evidence

- **Full-stack integration tests: 7 / 7** — `CourtOrderIT` 2/2, `CourtOrderRequestedIngesterIT` 3/3, `CourtOrderRepositoryIT` 2/2 — against a live
  ES 9.3.3 container + WildFly 40 on JDK 25 (run via `./runIntegrationTests.sh`).
- ~65 unit tests green (including the new `DomainToIndexMapperTest` and the ES-indexer transformer tests).

---

## What you need to do to accept & release

1. **Review decision #1** (the JPA repository rewrite) and **#2** (null-`defendantIds` indexing behaviour).
2. Wait for the **25.104.x framework/platform milestones to be released** to Artifactory (this PR is a **draft**
   because it references a `-SNAPSHOT` parent, so CI can't build it yet).
3. Bump the **parent** from `25.104.0-M8-SNAPSHOT` to `25.104.0-M9`; likewise any other SNAPSHOT deps.
4. Run the build + ITs (`./runIntegrationTests.sh`) against a current local stack; expect the same green result.
5. Set the project version per the release scheme, mark PR **#37** ready, and release.

## References

- Draft PR: **#37** · Tickets: **PEG-3408**, **DD-41592**
- Living upgrade page: *Elasticsearch 9.3.3 Upgrade — Java 25* (Confluence, space PETTA, page 1990251336)
- AI-assistant notes for this repo: `CLAUDE.md`; change list: `CHANGELOG.md`.
