# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased] — Java 25 / WildFly 40 / Elasticsearch 9.3.3 (25.104.x)

### Changed
- Updated `service-parent-pom` and `coredomain.version` to `25.104.1`, which brings Liquibase `5.0.3` and the consolidated Maven plugin versions.
- Upgraded to the released `service-parent-pom:25.104.0-M9` (Java 25 / WildFly 40 / Jakarta EE 11), which carries the Elasticsearch `9.3.3` client via `platform-libraries M10` / `common-bom M4`.
- Migrated `javax.*` → `jakarta.*` across all modules; `javax:javaee-api` → `jakarta.platform:jakarta.jakartaee-api`.
- Added the `jakarta.xml.bind:jakarta.xml.bind-api` override to the RAML client-generator plugin blocks.
- Elasticsearch embedded test version `7.16.2` → `9.3.3` (aligned with the released ES client and the 9.3.3 image/`elasticsearch-eck` chart in ACR; indexing via the migrated shared `unifiedsearch-client`).
- Bumped `referencedata.version` `17.104.136` → `17.104.137` (interface-version enforcer).

### Migrated — DeltaSpike Data → plain JPA
- `CourtOrderRepository` rewritten from a DeltaSpike Data `@Repository` interface to a concrete
  `@ApplicationScoped` JPA repository (`@PersistenceContext EntityManager`; JPQL/native queries preserve the
  original `@Query` semantics). DeltaSpike is unavailable under Jakarta EE 11 / CDI 4.
- `viewstore-persistence` pom: removed DeltaSpike / OpenEJB / `hibernate-entitymanager` / H2; added
  `hibernate-core`, `test-utils-hibernate`, `postgresql`.
- `CourtOrderRepositoryTest` (DeltaSpike `CdiTestRunner` + embedded H2) split into: `CourtOrderRepositoryIT`
  (real-Postgres `HibernateTestEntityManagerProvider`) moved into the **`applicationscourtorders-integration-test`**
  module so it runs alongside the other ITs (real Postgres is required — H2 cannot run the native
  `findByCaseAndDefendantId` `json_array_elements` query); and a Mockito `CourtOrderRepositoryTest` +
  `CourtOrderEntityTest` in the persistence module giving DB-less unit coverage of all 5 repository methods
  (query text + params) for the jacoco gate.
- `persistence.xml` (prod + test) updated to the Jakarta 3.0 namespace.

### Fixed
- **Integration-test harness:** removed the leftover `org.apache.tomee:openejb-cxf-rs` test dependency from
  `applicationscourtorders-integration-test`. It put Apache CXF's JAX-RS `ClientBuilder` on the test classpath
  alongside RestEasy, so `RestPoller`'s generic `ClientBuilder.newClient()` could bind to CXF, whose client
  silently failed to read the vendor `+json` response body — every query poll then saw a null payload and timed
  out (all ITs failing). Nothing used OpenEJB (the ITs run against a deployed WildFly), so it was dead weight.
- **Event-indexer NPE (`DomainToIndexMapper`):** guard against a null `defendantIds`. `defendantIds` is optional
  on the create-court-order command, but the indexer called `getDefendantIds().forEach(...)` unconditionally, so
  a court order with no defendants threw an NPE that failed the EVENT_INDEXER and stalled the shared
  `crime_case_index` ingestion stream — intermittently timing out other tests' ES `findBy` polls. A court order
  with no defendantIds is now indexed with no defendant party instead of crashing.
