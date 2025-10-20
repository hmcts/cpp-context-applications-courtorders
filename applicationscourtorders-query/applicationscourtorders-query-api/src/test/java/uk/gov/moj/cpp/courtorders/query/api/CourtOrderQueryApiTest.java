package uk.gov.moj.cpp.courtorders.query.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.courtorders.query.CourtOrderQueryView;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtOrderQueryApiTest {


    @Mock
    private JsonEnvelope envelope;

    @Mock
    private CourtOrderQueryView courtOrderQueryView;

    @Mock
    private JsonEnvelope response;


    @InjectMocks
    private CourtOrderQueryApi courtOrderQueryApi;


    @Test
    public void getCourtOrdersByDefendant() {
        when(courtOrderQueryView.getCourtOrdersByDefendant(envelope)).thenReturn(response);
        assertThat(courtOrderQueryApi.getCourtOrdersByDefendant(envelope), equalTo(response));
    }

    @Test
    public void getCourtOrdersByDefendantAndOffenceDate() {
        when(courtOrderQueryView.getCourtOrdersByDefendantAndOffenceDate(envelope)).thenReturn(response);
        assertThat(courtOrderQueryApi.getCourtOrdersByDefendantIdAndOffenceDate(envelope), equalTo(response));
    }

    @Test
    public void getCourtOrdersByCaseId() {
        when(courtOrderQueryView.getCourtOrdersByCase(envelope)).thenReturn(response);
        assertThat(courtOrderQueryApi.getCourtOrdersByCaseId(envelope), equalTo(response));
    }

    @Test
    public void getCourtOrdersByHearingAndDefendant() {
        when(courtOrderQueryView.getCourtOrdersByHearingAndDefendant(envelope)).thenReturn(response);
        assertThat(courtOrderQueryApi.getCourtOrdersByHearingAndDefendant(envelope), equalTo(response));
    }
}
