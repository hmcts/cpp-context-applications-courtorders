package uk.gov.moj.cpp.courtorders.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.courtorders.command.CreateCourtOrder.createCourtOrder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.core.courts.CourtOrderRemoved;
import uk.gov.justice.core.courts.CourtOrderRequested;
import uk.gov.justice.core.courts.CourtOrderValidityUpdated;
import uk.gov.justice.core.courts.JudicialChildResults;
import uk.gov.justice.core.courts.JudicialChildResultsUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.courtorders.aggregate.CourtOrderAggregate;
import uk.gov.moj.cpp.courtorders.command.CreateCourtOrder;
import uk.gov.moj.cpp.courtorders.command.PatchUpdateJudicialChildResults;
import uk.gov.moj.cpp.courtorders.command.PatchUpdateJudicialChildResultsV2;
import uk.gov.moj.cpp.courtorders.command.RemoveCourtOrder;
import uk.gov.moj.cpp.courtorders.command.UpdateCourtOrderValidity;
import uk.gov.moj.cpp.courtorders.handler.service.ProgressionService;
import uk.gov.moj.cpp.courtorders.handler.service.ReferenceDataService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;


@ExtendWith(MockitoExtension.class)
public class CourtOrderRequestHandlerTest {

    private static final UUID JUDICIAL_RESULT_TYPE_ID = randomUUID();
    private static final LocalDate END_DATE = LocalDate.now().plusDays(10);
    private static final String UNPAID_WORK_RESULT_IDS = "9bec5977-1796-4645-9b9e-687d4f23d37d,5ab456c8-d272-4082-87ed-cd1f44a0603a";
    private static final String UNPAID_WORK_RESULT_ID = "9bec5977-1796-4645-9b9e-687d4f23d37d";

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private EventStream eventStream2;

    @Mock
    private EventStream eventStream3;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private ProgressionService progressionService;

    @InjectMocks
    private CourtOrderRequestHandler courtOrderRequestHandler;

    @Spy
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter();

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(CourtOrderRequested.class, CourtOrderRemoved.class, CourtOrderValidityUpdated.class, JudicialChildResultsUpdated.class);

    private final UUID courtOrderId = JUDICIAL_RESULT_TYPE_ID;
    private final UUID applicationId = JUDICIAL_RESULT_TYPE_ID;
    private final LocalDate newEndDate = END_DATE;

    @BeforeEach
    public void setup() {
        ReflectionUtil.setField(this.jsonToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void handleAddCourtRegister() {
        assertThat(new CourtOrderRequestHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleAddCourtRegister")
                        .thatHandles("courtorders.command.create-court-order")
                ));
    }


    @Test
    public void handleRemoveCourtRegister() {
        assertThat(new CourtOrderRequestHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleRemoveCourtRegister")
                        .thatHandles("courtorders.command.remove-court-order")
                ));
    }

    @Test
    public void raiseCourtOrderRequested() throws Exception {
        final CourtOrderAggregate aggregate = new CourtOrderAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtOrderAggregate.class)).thenReturn(aggregate);
        when(referenceDataService.getResultDefinition(any(), any()))
                .thenReturn(createObjectBuilder().build());
        setField(courtOrderRequestHandler, "unpaidWorkJudicialTypeIds", UNPAID_WORK_RESULT_IDS);
        courtOrderRequestHandler.handleAddCourtRegister(buildEnvelope());
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("applicationscourtorders.event.court-order-requested"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtOrder.id", is(courtOrderId.toString())),
                                withJsonPath("$.courtOrder.orderDate", is(LocalDate.now().toString()))
                                )
                        ))
                )
        );
    }

    @Test
    void shouldRaiseCourtOrderRequestedWhenHasUnpaidWork() throws Exception {
        final CourtOrderAggregate aggregate =  new CourtOrderAggregate();
        setField(courtOrderRequestHandler, "unpaidWorkJudicialTypeIds", UNPAID_WORK_RESULT_IDS);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtOrderAggregate.class)).thenReturn(aggregate);
        when(referenceDataService.getResultDefinition(any(), eq(JUDICIAL_RESULT_TYPE_ID)))
                .thenReturn(createObjectBuilder().add("unpaidWorkExtensionCommunityOrdYro", true).build());

        courtOrderRequestHandler.handleAddCourtRegister(buildUnpaidWorkEnvelope());
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("applicationscourtorders.event.court-order-requested"),
                                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                                withJsonPath("$.courtOrder.id", is(courtOrderId.toString())),
                                                withJsonPath("$.courtOrder.orderDate", is(LocalDate.now().toString())),
                                                withJsonPath("$.courtOrder.endDate", is(END_DATE.toString())),
                                                withJsonPath("$.courtOrder.expiryDate", is(END_DATE.plusYears(1).toString()))
                                        )
                                ))
                )
        );
    }


    @Test
    public void raiseRemoveCourtRegisterRequested() throws Exception {
        final CourtOrderAggregate aggregate = new CourtOrderAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtOrderAggregate.class)).thenReturn(aggregate);

        courtOrderRequestHandler.handleRemoveCourtRegister(buildRemoveCourtRegisterEnvelope());
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("applicationscourtorders.event.court-order-removed"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtOrderId", is(courtOrderId.toString()))
                                )
                        ))
                )
        );
    }

    @Test
    public void updateCourtRegisterRequested() throws Exception {
        final CourtOrderAggregate aggregate = new CourtOrderAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtOrderAggregate.class)).thenReturn(aggregate);
        setField(aggregate, "orderEndDate", END_DATE);
        setField(aggregate, "actualCourtOrder", createCourtOrder().withIsUnpaidWork(false).build());
        courtOrderRequestHandler.handleUpdateCourtRegister(buildUpdateCourtRegisterEnvelope());
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("applicationscourtorders.event.court-order-validity-updated"),
                                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                                withJsonPath("$.courtOrderId", is(courtOrderId.toString())),
                                                withJsonPath("$.applicationId", is(applicationId.toString())),
                                                withJsonPath("$.newEndDate", is(END_DATE.toString()))
                                        )
                                ))
                )
        );
    }

    @Test
    public void resetCourtRegisterRequested() throws Exception {
        final CourtOrderAggregate aggregate = new CourtOrderAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtOrderAggregate.class)).thenReturn(aggregate);
        final Map<UUID, LocalDate> mapApplicationIdOriginalEndDate = new HashMap<>();
        mapApplicationIdOriginalEndDate.put(applicationId,LocalDate.now().minusDays(10));
        setField(aggregate, "mapApplicationIdOriginalEndDate", mapApplicationIdOriginalEndDate);
        setField(aggregate, "orderEndDate", LocalDate.now().minusDays(10));
        courtOrderRequestHandler.handleUpdateCourtRegister(buildResetCourtRegisterEnvelope());
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("applicationscourtorders.event.court-order-validity-updated"),
                                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                                withJsonPath("$.courtOrderId", is(courtOrderId.toString())),
                                        withJsonPath("$.applicationId", is(applicationId.toString())),
                                        withJsonPath("$.newEndDate", is(LocalDate.now().minusDays(10).toString()))
                                        )
                                ))
                )
        );
    }

    @Test
    void shouldHandlePatchUpdateJudicialChildResults() throws EventStreamException {
        setField(courtOrderRequestHandler, "unpaidWorkJudicialTypeIds", UNPAID_WORK_RESULT_IDS);
        final UUID courtOrderId1 = UUID.randomUUID();
        final UUID courtOrderId2 = UUID.randomUUID();
        final UUID courtOrderId3 = UUID.randomUUID();
        final UUID hearingId1 = UUID.randomUUID();
        final UUID hearingId2 = UUID.randomUUID();
        final UUID hearingId3 = UUID.randomUUID();
        final UUID masterDefendantId1 = UUID.randomUUID();
        final UUID masterDefendantId2 = UUID.randomUUID();
        final UUID masterDefendantId3 = UUID.randomUUID();
        final CourtOrderAggregate aggregate1 = new CourtOrderAggregate();
        final CourtOrderAggregate aggregate2 = new CourtOrderAggregate();
        final CourtOrderAggregate aggregate3 = new CourtOrderAggregate();
        setField(aggregate1, "actualCourtOrder", createCourtOrder().withMasterDefendantId(masterDefendantId1).withOrderingHearingId(hearingId1).withEndDate(END_DATE).withJudicialResultTypeId(JUDICIAL_RESULT_TYPE_ID).build());
        setField(aggregate2, "actualCourtOrder", createCourtOrder().withMasterDefendantId(masterDefendantId2).withOrderingHearingId(hearingId2).withEndDate(END_DATE).withJudicialResultTypeId(JUDICIAL_RESULT_TYPE_ID).build());
        setField(aggregate3, "actualCourtOrder", createCourtOrder().withMasterDefendantId(masterDefendantId3).withOrderingHearingId(hearingId3).withEndDate(END_DATE).withJudicialResultTypeId(JUDICIAL_RESULT_TYPE_ID).build());
        when(progressionService.getJudicialChildResults(any(), eq(hearingId1), eq(masterDefendantId1), eq(JUDICIAL_RESULT_TYPE_ID)))
                .thenReturn(createObjectBuilder().add("judicialChildResults",
                        createArrayBuilder().add(createObjectBuilder().add("judicialResultId", randomUUID().toString()).add("judicialResultTypeId", UNPAID_WORK_RESULT_ID).add("label", "some").build())).build());

        when(progressionService.getJudicialChildResults(any(), eq(hearingId2), eq(masterDefendantId2), eq(JUDICIAL_RESULT_TYPE_ID)))
                .thenReturn(createObjectBuilder().add("judicialChildResults",
                        createArrayBuilder().build()).build());

        when(progressionService.getJudicialChildResults(any(), eq(hearingId3), eq(masterDefendantId3), eq(JUDICIAL_RESULT_TYPE_ID)))
                .thenReturn(createObjectBuilder().add("judicialChildResults",
                        createArrayBuilder().add(createObjectBuilder().add("judicialResultId", randomUUID().toString()).add("judicialResultTypeId", randomUUID().toString()).build())).build());

        when(eventSource.getStreamById(courtOrderId1)).thenReturn(eventStream);
        when(eventSource.getStreamById(courtOrderId2)).thenReturn(eventStream2);
        when(eventSource.getStreamById(courtOrderId3)).thenReturn(eventStream3);
        when(aggregateService.get(eventStream, CourtOrderAggregate.class)).thenReturn(aggregate1);
        when(aggregateService.get(eventStream2, CourtOrderAggregate.class)).thenReturn(aggregate2);
        when(aggregateService.get(eventStream3, CourtOrderAggregate.class)).thenReturn(aggregate3);
        when(referenceDataService.getResultDefinition(any(), eq(JUDICIAL_RESULT_TYPE_ID)))
                .thenReturn(createObjectBuilder().add("unpaidWorkExtensionCommunityOrdYro", true).build());
        courtOrderRequestHandler.handlePatchUpdateJudicialChildResults(envelope("courtorders.command.patch-update-judicial-child-results", PatchUpdateJudicialChildResults.patchUpdateJudicialChildResults()
                .withCourtOrderIds(Arrays.asList(courtOrderId1, courtOrderId2, courtOrderId3))
                .build()
        ));
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        verify(eventStream2, times(0)).append(any());
        verify(eventStream3, times(1)).append(any());
        assertThat(envelopeStream, streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("applicationscourtorders.event.judicial-child-results-updated"),
                                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                                withJsonPath("$.courtOrderId", is(courtOrderId1.toString())),
                                                withJsonPath("$.judicialChildResults[0].judicialResultTypeId", is(UNPAID_WORK_RESULT_ID)),
                                                withJsonPath("$.isUnpaidWork", is(true)),
                                                withJsonPath("$.expiryDate", is(END_DATE.plusYears(1).toString()))
                                        )
                                ))
                )
        );
    }

    @Test
    void shouldHandlePatchUpdateJudicialChildResultsV2() throws EventStreamException {
        setField(courtOrderRequestHandler, "unpaidWorkJudicialTypeIds", UNPAID_WORK_RESULT_IDS);
        final UUID courtOrderId1 = UUID.randomUUID();
        final UUID hearingId1 = UUID.randomUUID();
        final UUID masterDefendantId1 = UUID.randomUUID();
        final LocalDate latestEndDate= LocalDate.now().plusDays(1);
        final CourtOrderAggregate aggregate1 = new CourtOrderAggregate();
        setField(aggregate1, "actualCourtOrder", createCourtOrder().withMasterDefendantId(masterDefendantId1).withOrderingHearingId(hearingId1).withEndDate(END_DATE).withJudicialResultTypeId(JUDICIAL_RESULT_TYPE_ID).build());
         when(progressionService.getJudicialChildResultsV2(any(), eq(hearingId1), eq(masterDefendantId1), eq(JUDICIAL_RESULT_TYPE_ID)))
                .thenReturn(createObjectBuilder()
                        .add("latestEndDate",latestEndDate.toString())
                        .add("judicialChildResults",
                        createArrayBuilder().add(createObjectBuilder().add("judicialResultId", randomUUID().toString()).add("judicialResultTypeId", UNPAID_WORK_RESULT_ID).add("label", "some").build())).build());


        when(eventSource.getStreamById(courtOrderId1)).thenReturn(eventStream);

        when(aggregateService.get(eventStream, CourtOrderAggregate.class)).thenReturn(aggregate1);
        when(referenceDataService.getResultDefinition(any(), eq(JUDICIAL_RESULT_TYPE_ID)))
                .thenReturn(createObjectBuilder().add("unpaidWorkExtensionCommunityOrdYro", true).build());
        courtOrderRequestHandler.handlePatchUpdateJudicialChildResultsV2(envelope("courtorders.command.patch-update-judicial-child-results-v2", PatchUpdateJudicialChildResultsV2.patchUpdateJudicialChildResultsV2()
                .withCourtOrderIds(List.of(courtOrderId1))
                .build()
        ));
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("applicationscourtorders.event.judicial-child-results-updated"),
                                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                                withJsonPath("$.courtOrderId", is(courtOrderId1.toString())),
                                                withJsonPath("$.judicialChildResults[0].judicialResultTypeId", is(UNPAID_WORK_RESULT_ID)),
                                                withJsonPath("$.isUnpaidWork", is(true)),
                                                withJsonPath("$.expiryDate", is(latestEndDate.plusYears(1).toString()))
                                        )
                                ))
                )
        );
    }

    private Envelope<CreateCourtOrder> buildEnvelope() {

        final CreateCourtOrder createCourtOrder = createCourtOrder()
                .withId(courtOrderId)
                .withOrderDate(LocalDate.now())
                .build();

        return envelope("courtorders.command.create-court-order", createCourtOrder);
    }

    private Envelope<CreateCourtOrder> buildUnpaidWorkEnvelope() {

        final CreateCourtOrder createCourtOrder = createCourtOrder()
                .withId(courtOrderId)
                .withOrderDate(LocalDate.now())
                .withJudicialResultTypeId(JUDICIAL_RESULT_TYPE_ID)
                .withEndDate(END_DATE)
                .withJudicialChildResults(singletonList(JudicialChildResults.judicialChildResults()
                        .withJudicialResultTypeId(UUID.fromString(UNPAID_WORK_RESULT_ID))
                        .build()))
                .build();

        return envelope("courtorders.command.create-court-order", createCourtOrder);
    }

    private Envelope<RemoveCourtOrder> buildRemoveCourtRegisterEnvelope() {

        final RemoveCourtOrder createCourtOrder = RemoveCourtOrder.removeCourtOrder()
                .withCourtOrderId(courtOrderId)
                .build();

        return envelope("courtorders.command.remove-court-order", createCourtOrder);
    }

    private Envelope<UpdateCourtOrderValidity> buildUpdateCourtRegisterEnvelope() {

        final UpdateCourtOrderValidity updateCourtOrder = UpdateCourtOrderValidity.updateCourtOrderValidity()
                .withCourtOrderId(courtOrderId)
                .withApplicationId(applicationId)
                .withNewEndDate(newEndDate)
                .build();

        return envelope("courtorders.command.update-court-order-validity", updateCourtOrder);
    }

    private Envelope<UpdateCourtOrderValidity> buildResetCourtRegisterEnvelope() {

        final UpdateCourtOrderValidity updateCourtOrder = UpdateCourtOrderValidity.updateCourtOrderValidity()
                .withCourtOrderId(courtOrderId)
                .withApplicationId(applicationId)
                .withResetToOriginalEndDate(true)
                .build();

        return envelope("courtorders.command.update-court-order-validity", updateCourtOrder);
    }

    private <T> Envelope<T> envelope(final String name, final T t) {
        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID(name).withUserId(JUDICIAL_RESULT_TYPE_ID.toString()).build());
        return envelopeFrom(metadataBuilder, t);
    }
}
