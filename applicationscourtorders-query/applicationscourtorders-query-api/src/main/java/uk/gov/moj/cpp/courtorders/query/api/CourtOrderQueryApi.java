package uk.gov.moj.cpp.courtorders.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.courtorders.query.CourtOrderQueryView;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class CourtOrderQueryApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private CourtOrderQueryView courtOrderQueryView;

    @Handles("applicationscourtorders.query.court-order-by-defendant-id")
    public JsonEnvelope getCourtOrdersByDefendant(final JsonEnvelope envelope) {
        return courtOrderQueryView.getCourtOrdersByDefendant(envelope);
    }

    @Handles("applicationscourtorders.query.court-order-by-defendant-id-and-offence-date")
    public JsonEnvelope getCourtOrdersByDefendantIdAndOffenceDate(final JsonEnvelope envelope) {
        return courtOrderQueryView.getCourtOrdersByDefendantAndOffenceDate(envelope);
    }

    @Handles("applicationscourtorders.query.court-order-by-hearing-and-defendant-id")
    public JsonEnvelope getCourtOrdersByHearingAndDefendant(final JsonEnvelope envelope) {
        return courtOrderQueryView.getCourtOrdersByHearingAndDefendant(envelope);
    }

    @Handles("applicationscourtorders.query.court-order-by-case-and-defendant-id")
    public JsonEnvelope getCourtOrdersByCaseId(final JsonEnvelope envelope) {
        return courtOrderQueryView.getCourtOrdersByCase(envelope);
    }
}
