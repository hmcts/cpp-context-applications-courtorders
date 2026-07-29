# CLAUDE.md — cpp-context-applications-courtorders

Context-specific notes for the **Java 25 / WildFly 40 / Elasticsearch 9.2.2** upgrade (25.104.x). See the
workspace root `CLAUDE.md` for platform-wide guidance.

## ⚠️ DeltaSpike Data → plain JPA migration (production code)

This context's `CourtOrderRepository` was a **DeltaSpike Data `@Repository` interface**
(`org.apache.deltaspike.data.api.EntityRepository` with `@Query` proxy methods). DeltaSpike is EOL and
**not available under Jakarta EE 11 / CDI 4**, and the 25.104.x `service-parent-pom` no longer manages the
DeltaSpike / OpenEJB / `hibernate-entitymanager` versions — so the old code fails at POM parse time
(`'dependencies.dependency.version' ... is missing`).

It was rewritten to the standard Java-25 pattern (mirroring `cpp-context-listing`):

- **Repository** → concrete `@ApplicationScoped` class with `@PersistenceContext(unitName = "applicationscourtorders-persistence-unit") EntityManager entityManager`. Each `@Query` becomes an explicit `entityManager.createQuery(...)` (JPQL) or `createNativeQuery(..., CourtOrderEntity.class)` (native), preserving the exact original semantics. `save()` → `entityManager.merge(entity)`.
- The two callers (`CourtOrderQueryView`, `CourtOrderEventListener`) inject `CourtOrderRepository` **by type**, so the interface→class change is transparent to them. Their unit tests use Mockito `@Mock` (which mocks a concrete class fine) — no change needed.
- **`viewstore-persistence/pom.xml`** — removed `persistence-deltaspike`, all `deltaspike-*`, `openejb-*`, `activemq-ra`, `hibernate-entitymanager`, legacy `org.hibernate:hibernate-core`, `guava-testlib`, H2. Added `org.hibernate.orm:hibernate-core` (provided), `test-utils-hibernate` + `test-utils-common` + `postgresql` (test).
- **Repository tests** — the old DeltaSpike `@RunWith(CdiTestRunner.class)` + embedded-H2 `CourtOrderRepositoryTest` split into two:
    - **`CourtOrderRepositoryIT`** (real-Postgres, `HibernateTestEntityManagerProvider` via JUnit 5 `@RegisterExtension`) now lives in the **`applicationscourtorders-integration-test`** module (package `uk.gov.moj.cpp.courtorders.persistence.repository`), so it runs with the other ITs under the `applicationscourtorders-integration-test` failsafe profile / `rit` — NOT in the DB-less PR/`mmse` build. The `CourtOrderRepository`/`CourtOrderEntity` come from the `applicationscourtorders-viewstore-persistence` test-scope dep; its test `persistence.xml` (unit `applicationscourtorders-test-persistence-unit` → `applicationscourtordersviewstore` DB) lists `CourtOrderEntity` explicitly (`<class>`) since the entity is now on the classpath as a JAR, not `../classes`. **The Postgres-specific native `findByCaseAndDefendantId` (`json_array_elements`) query is validated against real Postgres end-to-end by `CourtOrderIT`'s `court-order-by-case-and-defendant-id` calls (H2 cannot run it).**
    - **`CourtOrderRepositoryTest`** (Mockito, in the persistence module) mocks the `EntityManager` and asserts each of the 5 methods' query text + parameters — this is the DB-less unit coverage that satisfies the jacoco gate. `CourtOrderEntityTest` covers the entity accessors.
- **`persistence.xml`** (both prod and test) updated to the Jakarta 3.0 namespace (`https://jakarta.ee/xml/ns/persistence`, `version="3.0"`); prod was javax `version="1.0"`.

## Jakarta / Java 25 mechanics

- `javax.*` → `jakarta.*` across all modules (json, ws.rs, inject, jms, persistence, transaction, enterprise).
- `javax:javaee-api` → `jakarta.platform:jakarta.jakartaee-api` in every module pom.
- RAML client-generator plugins (`rest-client-generator-plugin`, `messaging-client-generator-plugin`) need the `jakarta.xml.bind:jakarta.xml.bind-api` (`${jakarta.xml.bind-api.raml.version}`, inherited from parent) override in their `<dependencies>` block, alongside the jakartaee-api plugin dep.
- Parent → `service-parent-pom:25.104.0-M8-SNAPSHOT`.

## Elasticsearch 9.2.2

- This is an ES **indexing** context: `applicationscourtorders-event-indexer` transforms `court-order-requested`
  events and writes documents via the shared `unifiedsearch-client` (in `cpp-platform-libraries`, already migrated
  to `co.elastic.clients:elasticsearch-java`). This context owns no ES index mappings.
- `elasticsearch.embedded.version` → `9.2.2` (embedded-ES for unit tests); `elasticsearch-maven-plugin` stays `6.13`.
- ITs run against the real ES 9.2.2 container in `cpp-developers-docker`. `CourtOrderRequestedIngesterIT` is the
  end-to-end ES indexing proof.

## Interface-version enforcer

The custom `enforce-moj-latest-interfaces` rule does NOT honour `-Denforcer.skip=true`. Keep MoJ interface
version properties in the **root pom** current (e.g. `referencedata.version`) — the rule fails the build if a
newer released version exists in Artifactory.

## Integration-test gotchas (Java 25 upgrade)

- **No CXF on the IT classpath.** The IT poller (`RestPoller` → `RestClient`) uses the generic JAX-RS
  `ClientBuilder.newClient()`. Two `ClientBuilder` providers on the classpath (RestEasy + Apache CXF) makes the
  bound client non-deterministic; if CXF wins, it fails to read the vendor `+json` body and every poll returns a
  null payload → all ITs time out. A leftover `org.apache.tomee:openejb-cxf-rs` test dependency was removed for
  this reason. Keep RestEasy as the sole JAX-RS client provider in the IT module.
- **The EVENT_INDEXER indexes ALL `court-order-requested` events**, including those from `CourtOrderIT` (not just
  the ingester tests). An event that throws in the indexer (e.g. the null-`defendantIds` NPE, now fixed) fails and,
  under self-healing, **stalls the shared `crime_case_index` stream**, so unrelated tests' ES `findBy` polls time
  out intermittently. If ingester ITs flake with `findBy` timeouts, check `server.log` for an indexer exception
  stalling the stream — it is usually a bad/edge-case event payload, not the test being verified.
