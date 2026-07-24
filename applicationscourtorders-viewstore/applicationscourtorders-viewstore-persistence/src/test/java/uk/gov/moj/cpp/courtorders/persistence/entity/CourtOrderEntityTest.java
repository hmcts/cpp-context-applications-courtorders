package uk.gov.moj.cpp.courtorders.persistence.entity;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class CourtOrderEntityTest {

    @Test
    public void shouldHoldCourtOrderFieldsViaAccessors() {
        final UUID id = randomUUID();
        final UUID courtOrderId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final LocalDate sittingDate = LocalDate.of(2026, 7, 28);
        final LocalDate expiryDate = LocalDate.of(2027, 1, 1);
        final String payload = "{\"prosecutionCaseId\":\"case-1\"}";

        final CourtOrderEntity entity = new CourtOrderEntity();
        entity.setId(id);
        entity.setCourtOrderId(courtOrderId);
        entity.setDefendantId(defendantId);
        entity.setHearingId(hearingId);
        entity.setSittingDate(sittingDate);
        entity.setExpiryDate(expiryDate);
        entity.setPayload(payload);
        entity.setRemoved(true);

        assertThat(entity.getId(), is(id));
        assertThat(entity.getCourtOrderId(), is(courtOrderId));
        assertThat(entity.getDefendantId(), is(defendantId));
        assertThat(entity.getHearingId(), is(hearingId));
        assertThat(entity.getSittingDate(), is(sittingDate));
        assertThat(entity.getExpiryDate(), is(expiryDate));
        assertThat(entity.getPayload(), is(payload));
        assertThat(entity.isRemoved(), is(true));
    }
}
