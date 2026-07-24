package uk.gov.moj.cpp.courtorders.indexer.transformer.mapper;

import static java.util.List.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;
import uk.gov.moj.cpp.courtorders.command.CreateCourtOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DomainToIndexMapperTest {

    private final DomainToIndexMapper domainToIndexMapper = new DomainToIndexMapper();

    @Mock
    private CreateCourtOrder createCourtOrder;

    @Mock
    private CourtOrderOffence courtOrderOffence;

    /**
     * defendantIds is optional on the create-court-order command. A court order with no defendantIds must be
     * indexed without any defendant party (and must NOT throw) — a thrown NPE here fails the event-indexer and
     * stalls the shared crime_case_index ingestion stream.
     */
    @Test
    public void shouldIndexCaseWithNoPartiesAndNotFailWhenDefendantIdsIsNull() {
        final UUID prosecutionCaseId = randomUUID();
        when(courtOrderOffence.getProsecutionCaseId()).thenReturn(prosecutionCaseId);
        when(createCourtOrder.getDefendantIds()).thenReturn(null);

        final Map<String, List<CaseDetails>> result =
                domainToIndexMapper.courtOrderOffencesToCaseDetails(new HashMap<>(), createCourtOrder, of(courtOrderOffence));

        final List<CaseDetails> caseDocuments = result.get("caseDocuments");
        assertThat(caseDocuments, hasSize(1));
        assertThat(caseDocuments.get(0).getCaseId(), is(prosecutionCaseId.toString()));
        assertThat(caseDocuments.get(0).getParties(), is(empty()));
    }
}
