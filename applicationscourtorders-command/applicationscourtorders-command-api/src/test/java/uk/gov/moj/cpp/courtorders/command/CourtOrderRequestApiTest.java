package uk.gov.moj.cpp.courtorders.command;


import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import javax.json.Json;
import javax.json.JsonObject;

@ExtendWith(MockitoExtension.class)
public class CourtOrderRequestApiTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @InjectMocks
    private CourtOrderRequestApi courtOrderRequestApi;

    @Test
    public void invokeCreateCourtOrderRequest() {
        assertThat(CourtOrderRequestApi.class, isHandlerClass(COMMAND_API)
                .with(method("createCourtOrderRequest").thatHandles("courtorders.create-court-order")));
    }

    @Test
    public void invokeRemoveCourtOrderRequest() {
        assertThat(CourtOrderRequestApi.class, isHandlerClass(COMMAND_API)
                .with(method("removeCourtOrderRequest").thatHandles("courtorders.remove-court-order")));
    }

    @Test
    public void createCourtOrderRequest() {
        final JsonEnvelope commandEnvelope = buildEnvelope("courtorders.create-court-order");
        courtOrderRequestApi.createCourtOrderRequest(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());
        final DefaultEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), is("courtorders.command.create-court-order"));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payloadAsJsonObject()));
    }

    @Test
    public void removeCourtOrderRequest() {
        final JsonEnvelope commandEnvelope = buildEnvelope("courtorders.remove-court-order");
        courtOrderRequestApi.removeCourtOrderRequest(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());
        final DefaultEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), is("courtorders.command.remove-court-order"));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payloadAsJsonObject()));
    }

    @Test
    public void updateCourtOrderRequest() {
        final JsonEnvelope commandEnvelope = buildEnvelope("courtorders.update-court-order-validity");
        courtOrderRequestApi.updateCourtOrderRequest(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());
        final DefaultEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), is("courtorders.command.update-court-order-validity"));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payloadAsJsonObject()));
    }

    @Test
    void shouldPatchUpdateJudicialChildResultsRequestRequest() {
        final JsonEnvelope commandEnvelope = buildEnvelope("courtorders.patch-update-judicial-child-results");
        courtOrderRequestApi.patchUpdateJudicialChildResultsRequest(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());
        final DefaultEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), is("courtorders.command.patch-update-judicial-child-results"));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payloadAsJsonObject()));
    }

    @Test
    void shouldPatchUpdateJudicialChildResultsRequestRequestV2() {
        final JsonEnvelope commandEnvelope = buildEnvelope("courtorders.patch-update-judicial-child-results-v2");
        courtOrderRequestApi.patchUpdateJudicialChildResultsRequestV2(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());
        final DefaultEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), is("courtorders.command.patch-update-judicial-child-results-v2"));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payloadAsJsonObject()));
    }


    private JsonEnvelope buildEnvelope(String eventName) {
        final JsonObject payload = Json.createObjectBuilder()
                .add("courtOrder", Json.createObjectBuilder().add("id", randomUUID().toString()).build())
                .build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(eventName)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();
        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }

}
