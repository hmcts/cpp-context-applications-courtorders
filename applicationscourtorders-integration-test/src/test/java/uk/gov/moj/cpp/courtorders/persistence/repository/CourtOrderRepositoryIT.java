package uk.gov.moj.cpp.courtorders.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;

import uk.gov.moj.cpp.courtorders.persistence.entity.CourtOrderEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Real-Postgres repository IT (docker Postgres, production Liquibase schema). Migrated from the DeltaSpike
 * {@code CdiTestRunner} + embedded-H2 harness as part of the Java 25 / Jakarta EE 11 upgrade; each test rolls
 * back (HibernateTestEntityManagerProvider), so nothing is persisted.
 */
class CourtOrderRepositoryIT {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("applicationscourtorders-test-persistence-unit");

    private CourtOrderRepository courtOrderRepository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        courtOrderRepository = new CourtOrderRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(courtOrderRepository);
    }

    @Test
    void shouldFindByDefendantId() {
        final UUID id = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        final UUID courtOrderId1 = UUID.randomUUID();
        final UUID courtOrderId2 = UUID.randomUUID();
        final UUID courtOrderId3 = UUID.randomUUID();
        final LocalDate sittingDate = LocalDate.now().plusDays(10);
        final CourtOrderEntity courtOrderEntity1 = new CourtOrderEntity();
        final CourtOrderEntity courtOrderEntity2 = new CourtOrderEntity();
        final CourtOrderEntity courtOrderEntity3 = new CourtOrderEntity();

        courtOrderEntity1.setId(UUID.randomUUID());
        courtOrderEntity1.setCourtOrderId(courtOrderId1);
        courtOrderEntity1.setDefendantId(defendantId);
        courtOrderEntity1.setExpiryDate(LocalDate.now().minusDays(2));
        courtOrderRepository.save(courtOrderEntity1);

        courtOrderEntity2.setId(UUID.randomUUID());
        courtOrderEntity2.setCourtOrderId(courtOrderId2);
        courtOrderEntity2.setDefendantId(defendantId);
        courtOrderEntity2.setExpiryDate(LocalDate.now().minusDays(1));
        courtOrderRepository.save(courtOrderEntity2);

        courtOrderEntity3.setId(id);
        courtOrderEntity3.setCourtOrderId(courtOrderId3);
        courtOrderEntity3.setDefendantId(defendantId);
        courtOrderEntity3.setExpiryDate(LocalDate.now());
        courtOrderEntity3.setHearingId(hearingId);
        courtOrderEntity3.setSittingDate(sittingDate);
        courtOrderEntity3.setRemoved(false);
        courtOrderEntity3.setPayload("{}");
        courtOrderRepository.save(courtOrderEntity3);

        final List<CourtOrderEntity> result = courtOrderRepository.findByDefendantIdAndExpiryDate(defendantId, LocalDate.now());

        assertThat(result.size(), is(1));
        assertThat(result.stream().anyMatch(c -> c.getCourtOrderId().equals(courtOrderId1)), is(false));
        assertThat(result.stream().anyMatch(c -> c.getCourtOrderId().equals(courtOrderId2)), is(false));
        assertThat(result.get(0).getId().equals(id), is(true));
        assertThat(result.get(0).getCourtOrderId().equals(courtOrderId3), is(true));
        assertThat(result.get(0).getDefendantId().equals(defendantId), is(true));
        assertThat(result.get(0).getExpiryDate().equals(LocalDate.now()), is(true));
        assertThat(result.get(0).getHearingId().equals(hearingId), is(true));
        assertThat(result.get(0).getSittingDate().equals(sittingDate), is(true));
        assertThat(result.get(0).getPayload().equals("{}"), is(true));
        assertThat(result.get(0).isRemoved(), is(false));
    }

    /**
     * Exercises the Postgres-specific native query ({@code json_array_elements(payload::json ->'courtOrderOffences')}),
     * which H2 cannot run — the reason this test must target real Postgres. Verifies each of the query's three
     * filters: prosecutionCaseId (matched inside the JSON payload), defendant_id, and {@code is_removed is false}.
     */
    @Test
    void shouldFindByCaseAndDefendantId() {
        final String caseId = UUID.randomUUID().toString();
        final String otherCaseId = UUID.randomUUID().toString();
        final UUID defendantId = UUID.randomUUID();
        final UUID otherDefendantId = UUID.randomUUID();

        final UUID matchingCourtOrderId = UUID.randomUUID();
        // Matches: right case in the payload, right defendant, not removed.
        courtOrderRepository.save(courtOrder(matchingCourtOrderId, defendantId, false, payloadForCase(caseId)));
        // Right case + defendant but removed -> excluded by `is_removed is false`.
        courtOrderRepository.save(courtOrder(UUID.randomUUID(), defendantId, true, payloadForCase(caseId)));
        // Right defendant, different case in the payload -> excluded by the json prosecutionCaseId match.
        courtOrderRepository.save(courtOrder(UUID.randomUUID(), defendantId, false, payloadForCase(otherCaseId)));
        // Right case, different defendant -> excluded by the defendant_id match.
        courtOrderRepository.save(courtOrder(UUID.randomUUID(), otherDefendantId, false, payloadForCase(caseId)));

        final List<CourtOrderEntity> result = courtOrderRepository.findByCaseAndDefendantId(caseId, defendantId);

        assertThat(result.size(), is(1));
        assertThat(result.get(0).getCourtOrderId(), is(matchingCourtOrderId));
        assertThat(result.get(0).getDefendantId(), is(defendantId));
        assertThat(result.get(0).isRemoved(), is(false));
    }

    private static CourtOrderEntity courtOrder(final UUID courtOrderId, final UUID defendantId, final boolean removed, final String payload) {
        final CourtOrderEntity entity = new CourtOrderEntity();
        entity.setId(UUID.randomUUID());
        entity.setCourtOrderId(courtOrderId);
        entity.setDefendantId(defendantId);
        entity.setRemoved(removed);
        entity.setPayload(payload);
        entity.setExpiryDate(LocalDate.now());
        return entity;
    }

    private static String payloadForCase(final String caseId) {
        return "{\"courtOrderOffences\":[{\"prosecutionCaseId\":\"" + caseId + "\"}]}";
    }
}
