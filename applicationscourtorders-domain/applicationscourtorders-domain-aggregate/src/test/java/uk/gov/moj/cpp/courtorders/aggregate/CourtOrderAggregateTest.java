package uk.gov.moj.cpp.courtorders.aggregate;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import uk.gov.justice.core.courts.CourtOrderRemoved;
import uk.gov.justice.core.courts.CourtOrderRequested;
import uk.gov.justice.core.courts.CourtOrderValidityUpdated;
import uk.gov.justice.core.courts.JudicialChildResults;
import uk.gov.justice.core.courts.JudicialChildResultsUpdated;
import uk.gov.moj.cpp.courtorders.command.CreateCourtOrder;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CourtOrderAggregateTest {

    @InjectMocks
    private CourtOrderAggregate aggregate;

    private final UUID courtOrderId = UUID.randomUUID();
    private final UUID applicationId = UUID.randomUUID();
    private final LocalDate newEndDate = LocalDate.now().plusDays(10);
    private final LocalDate originalOrderEndDate = LocalDate.now();

    @BeforeEach
    public void setUp() {
        aggregate = new CourtOrderAggregate();
    }

    @Test
    public void raiseCourtOrderRequested() {
        final LocalDate now = LocalDate.now();
        final CreateCourtOrder createCourtOrder = CreateCourtOrder.createCourtOrder()
                .withId(courtOrderId)
                .withOrderDate(now)
                .withMasterDefendantId(UUID.randomUUID())
                .withEndDate(now)
                .build();
        final List<Object> eventStream = aggregate.createCourtOrder(courtOrderId, createCourtOrder, false, List.of(UUID.randomUUID())).toList();;
        assertThat(eventStream.size(), is(1));
        final CourtOrderRequested event = (CourtOrderRequested) eventStream.get(0);

        assertThat(event.getCourtOrderId(), is(courtOrderId));
        assertThat(event.getCourtOrder().getEndDate(), is(now));
        assertThat(event.getCourtOrder().getExpiryDate(), is(now));

    }

    @Test
    void shouldAddOneYearToExpiryDateWhenUnpaidWorkExistsInChildResultAndUnpaidWorkCommunityOrderResult() {
        final LocalDate now = LocalDate.now();
        final CreateCourtOrder createCourtOrder = CreateCourtOrder.createCourtOrder()
                .withId(courtOrderId)
                .withOrderDate(now)
                .withMasterDefendantId(UUID.randomUUID())
                .withEndDate(now)
                .withJudicialChildResults(singletonList(JudicialChildResults.judicialChildResults().withJudicialResultTypeId(UUID.fromString("9bec5977-1796-4645-9b9e-687d4f23d37d")).build()))
                .build();
        final List<Object> eventStream = aggregate.createCourtOrder(courtOrderId, createCourtOrder, true, List.of(UUID.fromString("9bec5977-1796-4645-9b9e-687d4f23d37d"))).toList();;
        assertThat(eventStream.size(), is(1));
        final CourtOrderRequested event = (CourtOrderRequested) eventStream.get(0);

        assertThat(event.getCourtOrderId(), is(courtOrderId));
        assertThat(event.getCourtOrder().getEndDate(), is(now));
        assertThat(event.getCourtOrder().getExpiryDate(), is(now.plusYears(1)));

    }

    @Test
    void shouldAddOneYearToExpiryDateWhenUnpaidWorkExistsInChildResultAndNotUnpaidWorkCommunityOrderResult(){
        final LocalDate now = LocalDate.now();
        final CreateCourtOrder createCourtOrder = CreateCourtOrder.createCourtOrder()
                .withId(courtOrderId)
                .withOrderDate(now)
                .withMasterDefendantId(UUID.randomUUID())
                .withEndDate(now)
                .withJudicialChildResults(singletonList(JudicialChildResults.judicialChildResults().withJudicialResultTypeId(UUID.fromString("9bec5977-1796-4645-9b9e-687d4f23d37d")).build()))
                .build();
        final List<Object> eventStream = aggregate.createCourtOrder(courtOrderId, createCourtOrder, false, List.of(UUID.fromString("9bec5977-1796-4645-9b9e-687d4f23d37d"))).toList();;
        assertThat(eventStream.size(), is(1));
        final CourtOrderRequested event = (CourtOrderRequested) eventStream.get(0);

        assertThat(event.getCourtOrderId(), is(courtOrderId));
        assertThat(event.getCourtOrder().getEndDate(), is(now));
        assertThat(event.getCourtOrder().getExpiryDate(), is(now));
    }

    @Test
    public void courtOrderRemoved() {
        final CreateCourtOrder createCourtOrder = CreateCourtOrder.createCourtOrder()
            .withId(courtOrderId)
            .withOrderDate(LocalDate.now())
            .withMasterDefendantId(UUID.randomUUID())
            .build();
        aggregate.createCourtOrder(courtOrderId, createCourtOrder,false, List.of(UUID.fromString("9bec5977-1796-4645-9b9e-687d4f23d37d")));

        final List<Object> eventStream = aggregate.removeCourtOrder(courtOrderId).toList();;
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(CourtOrderRemoved.class)));
    }

    @Test
    public void raiseCourtOrderRemoved() {
        final CreateCourtOrder createCourtOrder = CreateCourtOrder.createCourtOrder()
                .withId(courtOrderId)
                .withOrderDate(LocalDate.now())
                .withMasterDefendantId(UUID.randomUUID())
                .build();
        aggregate.createCourtOrder(courtOrderId, createCourtOrder, false, List.of(UUID.fromString("9bec5977-1796-4645-9b9e-687d4f23d37d")));
        final List<Object> eventStream = aggregate.createCourtOrder(courtOrderId, createCourtOrder, false, List.of(UUID.fromString("9bec5977-1796-4645-9b9e-687d4f23d37d"))).toList();;
        assertThat(eventStream.size(), is(2));
        assertThat(eventStream.get(0).getClass(), is(CoreMatchers.equalTo(CourtOrderRemoved.class)));
        assertThat(eventStream.get(1).getClass(), is(CoreMatchers.equalTo(CourtOrderRequested.class)));
    }

    @Test
    public void updateCourtRegisterRequested() throws Exception {
        setField(aggregate, "orderEndDate", originalOrderEndDate);
        setField(aggregate, "actualCourtOrder", CreateCourtOrder.createCourtOrder().build());
        final List<Object> eventStream = aggregate.updateCourtOrder(courtOrderId, applicationId, newEndDate).toList();
        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass(), is(CoreMatchers.equalTo(CourtOrderValidityUpdated.class)));
    }

    @Test
    public void amendOldUpdateActiveOrderRequestAndAddOnlyNewResultsDuplicateUpdateCallReceived() throws Exception {
        setField(aggregate, "orderEndDate", originalOrderEndDate);
        aggregate.apply(CourtOrderValidityUpdated.courtOrderValidityUpdated().withCourtOrderId(courtOrderId).withApplicationId(applicationId).withNewEndDate(newEndDate).build());
        final List<Object> eventStream = aggregate.updateCourtOrder(courtOrderId, applicationId, newEndDate).toList();
        assertThat(eventStream.size(), is(0));
    }

    @Test
    public void resetCourtRegisterRequested() throws Exception {
        final Map<UUID, LocalDate> mapApplicationIdOriginalEndDate = new HashMap<>();
        mapApplicationIdOriginalEndDate.put(applicationId,LocalDate.now());
        setField(aggregate, "mapApplicationIdOriginalEndDate", mapApplicationIdOriginalEndDate);
        setField(aggregate, "orderEndDate", originalOrderEndDate);
        final List<Object> eventStream = aggregate.resetCourtOrder(courtOrderId, applicationId).toList();
        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass(), is(CoreMatchers.equalTo(CourtOrderValidityUpdated.class)));
    }

    @Test
    public void amendOldUpdateActiveOrderRequestAndAddOnlyNewResultsDuplicateResetCallReceived() throws Exception {
        setField(aggregate, "orderEndDate", originalOrderEndDate);
        aggregate.apply(CourtOrderValidityUpdated.courtOrderValidityUpdated().withCourtOrderId(courtOrderId).withApplicationId(applicationId).withNewEndDate(originalOrderEndDate).build());
        final List<Object> eventStream = aggregate.resetCourtOrder(courtOrderId, applicationId).toList();
        assertThat(eventStream.size(), is(0));
    }

    @Test
    public void raiseCourtOrderRemovedAndCourtOrderRequestedWhenCaseIsReResultedAfterEndDateAmendedViaApplication() {
        final CreateCourtOrder createCourtOrder = CreateCourtOrder.createCourtOrder()
                .withId(courtOrderId)
                .withOrderDate(LocalDate.now())
                .withEndDate(originalOrderEndDate)
                .withMasterDefendantId(UUID.randomUUID())
                .build();
        setField(aggregate, "actualCourtOrder", createCourtOrder);
        setField(aggregate, "isRemoved", false);
        setField(aggregate, "newEndDate", newEndDate);
        final List<Object> eventStream = aggregate.createCourtOrder(courtOrderId, createCourtOrder, false, List.of(UUID.fromString("9bec5977-1796-4645-9b9e-687d4f23d37d"))).toList();
        assertThat(eventStream.size(), is(2));
        final Object object = eventStream.get(1);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(CourtOrderRequested.class)));
        assertThat(((CourtOrderRequested)object).getCourtOrder().getEndDate(), is(newEndDate));
    }

    @Test
    void shouldUpdateJudicialChildResultWhenIsUnpaidWorkCommunityOrdIsFalse(){
        final CreateCourtOrder createCourtOrder = CreateCourtOrder.createCourtOrder()
                .withId(courtOrderId)
                .withOrderDate(LocalDate.now())
                .withEndDate(originalOrderEndDate)
                .withMasterDefendantId(UUID.randomUUID())
                .build();
        setField(aggregate, "actualCourtOrder", createCourtOrder);
        final UUID courtOrderId = UUID.randomUUID();
        final List<JudicialChildResultsUpdated> eventStream = aggregate.updateJudicialChildResult(courtOrderId, singletonList(
                        JudicialChildResults.judicialChildResults().withJudicialResultTypeId(UUID.fromString("9bec5977-1796-4645-9b9e-687d4f23d37d")).build()), false, List.of(UUID.fromString("9bec5977-1796-4645-9b9e-687d4f23d37d"))
                ).map(JudicialChildResultsUpdated.class::cast)
                .toList();
        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getCourtOrderId(), is(courtOrderId));
        assertThat(eventStream.get(0).getIsUnpaidWork(), is(false));
        assertThat(eventStream.get(0).getExpiryDate(), is(originalOrderEndDate));

    }

    @Test
    void shouldUpdateJudicialChildResultWhenNewEndDateNotExist(){
        final CreateCourtOrder createCourtOrder = CreateCourtOrder.createCourtOrder()
                .withId(courtOrderId)
                .withOrderDate(LocalDate.now())
                .withEndDate(originalOrderEndDate)
                .withMasterDefendantId(UUID.randomUUID())
                .build();
        setField(aggregate, "actualCourtOrder", createCourtOrder);
        final UUID courtOrderId = UUID.randomUUID();
        final List<JudicialChildResultsUpdated> eventStream = aggregate.updateJudicialChildResult(courtOrderId, singletonList(
                JudicialChildResults.judicialChildResults().withJudicialResultTypeId(UUID.fromString("9bec5977-1796-4645-9b9e-687d4f23d37d")).build()), true, List.of(UUID.fromString("9bec5977-1796-4645-9b9e-687d4f23d37d"))
        ).map(JudicialChildResultsUpdated.class::cast)
                .toList();
        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getCourtOrderId(), is(courtOrderId));
        assertThat(eventStream.get(0).getIsUnpaidWork(), is(true));
        assertThat(eventStream.get(0).getExpiryDate(), is(originalOrderEndDate.plusYears(1)));

    }

    @Test
    void shouldUpdateJudicialChildResultWhenNewEndDateExist(){
        final CreateCourtOrder createCourtOrder = CreateCourtOrder.createCourtOrder()
                .withId(courtOrderId)
                .withOrderDate(LocalDate.now())
                .withEndDate(originalOrderEndDate)
                .withMasterDefendantId(UUID.randomUUID())
                .build();
        setField(aggregate, "actualCourtOrder", createCourtOrder);
        setField(aggregate, "newEndDate", newEndDate);
        final UUID courtOrderId = UUID.randomUUID();
        final List<JudicialChildResultsUpdated> eventStream = aggregate.updateJudicialChildResult(courtOrderId, singletonList(
                        JudicialChildResults.judicialChildResults().withJudicialResultTypeId(UUID.fromString("9bec5977-1796-4645-9b9e-687d4f23d37d")).build()), true, List.of(UUID.fromString("9bec5977-1796-4645-9b9e-687d4f23d37d"))
                ).map(JudicialChildResultsUpdated.class::cast)
                .toList();
        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getCourtOrderId(), is(courtOrderId));
        assertThat(eventStream.get(0).getIsUnpaidWork(), is(true));
        assertThat(eventStream.get(0).getExpiryDate(), is(newEndDate.plusYears(1)));

    }


    @Test
    void shouldUpdateJudicialChildResultV2WhenUnpaidWorkExist(){
        final LocalDate latestEndDate = LocalDate.now().plusYears(1);
        final CreateCourtOrder createCourtOrder = CreateCourtOrder.createCourtOrder()
                .withId(courtOrderId)
                .withOrderDate(LocalDate.now())
                .withEndDate(originalOrderEndDate)
                .withMasterDefendantId(UUID.randomUUID())
                .build();
        setField(aggregate, "actualCourtOrder", createCourtOrder);
        setField(aggregate, "newEndDate", newEndDate);
        final UUID courtOrderId = UUID.randomUUID();
        final List<JudicialChildResults> judicialChildResults = singletonList(
                JudicialChildResults.judicialChildResults().withJudicialResultTypeId(UUID.fromString("9bec5977-1796-4645-9b9e-687d4f23d37d")).build());
        final List<UUID> unpaidWorkJudicialTypeIds = List.of(UUID.fromString("9bec5977-1796-4645-9b9e-687d4f23d37d"));
        final List<JudicialChildResultsUpdated> eventStream = aggregate.updateJudicialChildResultV2(courtOrderId, judicialChildResults, true, unpaidWorkJudicialTypeIds, latestEndDate)
                .map(JudicialChildResultsUpdated.class::cast)
                .toList();
        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getCourtOrderId(), is(courtOrderId));
        assertThat(eventStream.get(0).getIsUnpaidWork(), is(true));
        assertThat(eventStream.get(0).getExpiryDate(), is(latestEndDate.plusYears(1)));

    }

    @Test
    void shouldUpdateJudicialChildResultV2WhenUnpaidWorkNotExist(){
        final LocalDate latestEndDate = LocalDate.now().plusYears(1);
        final CreateCourtOrder createCourtOrder = CreateCourtOrder.createCourtOrder()
                .withId(courtOrderId)
                .withOrderDate(LocalDate.now())
                .withEndDate(originalOrderEndDate)
                .withMasterDefendantId(UUID.randomUUID())
                .build();
        setField(aggregate, "actualCourtOrder", createCourtOrder);
        setField(aggregate, "newEndDate", newEndDate);
        final UUID courtOrderId = UUID.randomUUID();
        final List<JudicialChildResults> judicialChildResults = singletonList(
                JudicialChildResults.judicialChildResults().withJudicialResultTypeId(UUID.fromString("3d964424-99f8-4345-a89b-3497ba118b25")).build());
        final List<UUID> unpaidWorkJudicialTypeIds = List.of(UUID.fromString("9bec5977-1796-4645-9b9e-687d4f23d37d"));
        final List<JudicialChildResultsUpdated> eventStream = aggregate.updateJudicialChildResultV2(courtOrderId, judicialChildResults, true, unpaidWorkJudicialTypeIds, latestEndDate)
                .map(JudicialChildResultsUpdated.class::cast)
                .toList();
        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getCourtOrderId(), is(courtOrderId));
        assertThat(eventStream.get(0).getIsUnpaidWork(), is(false));
        assertThat(eventStream.get(0).getExpiryDate(), is(latestEndDate));

    }

}
