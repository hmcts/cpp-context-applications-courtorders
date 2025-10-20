
package uk.gov.moj.cpp.courtorders.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.moj.cpp.courtorders.persistence.entity.CourtOrderEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CourtOrderRepositoryTest {

    @Inject
    private CourtOrderRepository courtOrderRepository;

    @Test
    public void shouldFindByDefendantId() {
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
        courtOrderEntity1.setExpiryDate(LocalDate.now());
        courtOrderRepository.save(courtOrderEntity1);

        courtOrderEntity2.setId(UUID.randomUUID());
        courtOrderEntity2.setCourtOrderId(courtOrderId2);
        courtOrderEntity2.setDefendantId(defendantId);
        courtOrderEntity2.setExpiryDate(LocalDate.now().minusDays(1));
        courtOrderRepository.save(courtOrderEntity2);

        courtOrderEntity3.setId(id);
        courtOrderEntity3.setCourtOrderId(courtOrderId3);
        courtOrderEntity3.setDefendantId(defendantId);
        courtOrderEntity3.setExpiryDate(LocalDate.now().plusDays(1));
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
        assertThat(result.get(0).getExpiryDate().equals(LocalDate.now().plusDays(1)), is(true));
        assertThat(result.get(0).getHearingId().equals(hearingId), is(true));
        assertThat(result.get(0).getSittingDate().equals(sittingDate), is(true));
        assertThat(result.get(0).getPayload().equals("{}"), is(true));
        assertThat(result.get(0).isRemoved(), is(false));

    }

}
