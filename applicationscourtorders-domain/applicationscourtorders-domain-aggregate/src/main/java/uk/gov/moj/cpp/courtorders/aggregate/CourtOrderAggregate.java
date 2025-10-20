package uk.gov.moj.cpp.courtorders.aggregate;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.core.courts.CourtOrderRemoved;
import uk.gov.justice.core.courts.CourtOrderRequested;
import uk.gov.justice.core.courts.CourtOrderValidityUpdated;
import uk.gov.justice.core.courts.JudicialChildResults;
import uk.gov.justice.core.courts.JudicialChildResultsUpdated;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.courtorders.command.CreateCourtOrder;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class CourtOrderAggregate implements Aggregate {
    private static final long serialVersionUID = -7950876778016335077L;

    private CreateCourtOrder actualCourtOrder;

    private boolean isRemoved;
    private LocalDate orderEndDate;
    private LocalDate newEndDate;
    private Boolean isReset = false;
    private final Map<UUID, LocalDate> mapApplicationIdOriginalEndDate = new HashMap<>();

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(CourtOrderRequested.class).apply(e -> {
                            actualCourtOrder = e.getCourtOrder();
                            isRemoved = false;
                            orderEndDate = actualCourtOrder.getEndDate();
                        }
                ),
                when(CourtOrderRemoved.class)
                        .apply(e -> isRemoved = true),
                when(CourtOrderValidityUpdated.class)
                        .apply(e -> {
                            mapApplicationIdOriginalEndDate.putIfAbsent(e.getApplicationId(), e.getOriginalEndDate());
                            newEndDate = e.getNewEndDate();
                            isReset = newEndDate.isEqual(orderEndDate);
                        }),
                when(JudicialChildResultsUpdated.class).apply(e ->
                        actualCourtOrder = CreateCourtOrder.createCourtOrder()
                                .withValuesFrom(actualCourtOrder)
                                .withJudicialChildResults(e.getJudicialChildResults())
                                .withExpiryDate(e.getExpiryDate())
                                .withIsUnpaidWork(e.getIsUnpaidWork())
                                .build()
                ),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> createCourtOrder(final UUID id, final CreateCourtOrder courtOrder, final boolean isUnpaidWorkCommunityOrd, final List<UUID> unpaidWorkJudicialTypeIds) {
        final Stream.Builder<Object> builder = Stream.builder();
        if (nonNull(actualCourtOrder) && !isRemoved) {
            final CourtOrderRemoved courtOrderRemoved = CourtOrderRemoved.courtOrderRemoved()
                    .withCourtOrderId(id)
                    .build();
            builder.add(courtOrderRemoved);
        }

        final boolean isUnpaidWork = isUnpaidWorkCommunityOrd && hasUnpaidWorkJudicialResultType(courtOrder.getJudicialChildResults(), unpaidWorkJudicialTypeIds);
        if (nonNull(newEndDate) && !courtOrder.getEndDate().isEqual(newEndDate)) {
            builder.add(CourtOrderRequested.courtOrderRequested()
                    .withCourtOrderId(id)
                    .withCourtOrder(CreateCourtOrder.createCourtOrder()
                            .withValuesFrom(courtOrder)
                            .withEndDate(newEndDate)
                            .withIsUnpaidWork(isUnpaidWork)
                            .withExpiryDate(calculateExpiryDate(newEndDate, isUnpaidWork))
                            .build())

                    .build());
        } else {
            final CourtOrderRequested courtOrderRequested = CourtOrderRequested.courtOrderRequested()
                    .withCourtOrderId(id)
                    .withCourtOrder(CreateCourtOrder.createCourtOrder()
                            .withValuesFrom(courtOrder)
                            .withIsUnpaidWork(isUnpaidWork)
                            .withExpiryDate(calculateExpiryDate(courtOrder.getEndDate(), isUnpaidWork))
                            .build()).build();
            builder.add(courtOrderRequested);
        }
        return apply(builder.build());
    }

    public Stream<Object> removeCourtOrder(final UUID id) {
        final Stream.Builder<Object> builder = Stream.builder();
        final CourtOrderRemoved courtOrderRemoved = CourtOrderRemoved.courtOrderRemoved()
                .withCourtOrderId(id)
                .build();
        builder.add(courtOrderRemoved);
        return apply(builder.build());
    }

    /**
     * Updates the validity of an existing court order.
     *
     * @param courtOrderId  the ID of the court order to update
     * @param applicationId the ID of the application associated with the court order
     * @param newEndDate    the new end date of the court order
     * @return a stream of events resulting from the update of the court order
     */
    public Stream<Object> updateCourtOrder(final UUID courtOrderId, final UUID applicationId, final LocalDate newEndDate) {
        final Stream.Builder<Object> builder = Stream.builder();
        if (!isRemoved && (isNull(this.newEndDate) || !this.newEndDate.isEqual(newEndDate))) {
            CourtOrderValidityUpdated courtOrderValidityUpdated = CourtOrderValidityUpdated.courtOrderValidityUpdated()
                    .withCourtOrderId(courtOrderId)
                    .withApplicationId(applicationId)
                    .withNewEndDate(newEndDate)
                    .withOriginalEndDate(orderEndDate)
                    .withExpiryDate(calculateExpiryDate(orderEndDate, actualCourtOrder.getIsUnpaidWork()))
                    .build();
            builder.add(courtOrderValidityUpdated);
        }
        return apply(builder.build());
    }

    /**
     * Resets the court order to its original end date.
     * @param courtOrderId  the ID of the court order to reset
     * @param applicationId the ID of the application associated with the court order
     * @return a stream of events resulting from the reset of the court order
     */
    public Stream<Object> resetCourtOrder(final UUID courtOrderId, final UUID applicationId) {
        final Stream.Builder<Object> builder = Stream.builder();
        if (!isRemoved && nonNull(mapApplicationIdOriginalEndDate.get(applicationId)) && !isReset) {
            CourtOrderValidityUpdated courtOrderValidityUpdated = CourtOrderValidityUpdated.courtOrderValidityUpdated()
                    .withCourtOrderId(courtOrderId)
                    .withApplicationId(applicationId)
                    .withNewEndDate(mapApplicationIdOriginalEndDate.get(applicationId))
                    .withOriginalEndDate(orderEndDate)
                    .build();
            builder.add(courtOrderValidityUpdated);
        }
        return apply(builder.build());
    }

    public Stream<Object> updateJudicialChildResult(final UUID courtOrderId, final List<JudicialChildResults> judicialChildResults, final boolean isUnpaidWorkCommunityOrd, final List<UUID> unpaidWorkJudicialTypeIds) {
        final boolean isUnpaidWork = isUnpaidWorkCommunityOrd && hasUnpaidWorkJudicialResultType(judicialChildResults, unpaidWorkJudicialTypeIds);
        return apply(Stream.of(JudicialChildResultsUpdated.judicialChildResultsUpdated()
                .withCourtOrderId(courtOrderId)
                .withJudicialChildResults(judicialChildResults)
                .withIsUnpaidWork(isUnpaidWork)
                .withExpiryDate(calculateExpiryDate(nonNull(this.newEndDate) ? this.newEndDate : actualCourtOrder.getEndDate(), isUnpaidWork))
                .build()));
    }

    public Stream<Object> updateJudicialChildResultV2(final UUID courtOrderId, final List<JudicialChildResults> judicialChildResults, final boolean isUnpaidWorkCommunityOrd, final List<UUID> unpaidWorkJudicialTypeIds, final LocalDate latestEndDate) {
        final boolean isUnpaidWork = isUnpaidWorkCommunityOrd && hasUnpaidWorkJudicialResultType(judicialChildResults, unpaidWorkJudicialTypeIds);
        return apply(Stream.of(JudicialChildResultsUpdated.judicialChildResultsUpdated()
                .withCourtOrderId(courtOrderId)
                .withJudicialChildResults(judicialChildResults)
                .withIsUnpaidWork(isUnpaidWork)
                .withExpiryDate(calculateExpiryDate(latestEndDate, isUnpaidWork))
                .build()));
    }

    public CreateCourtOrder getActualCourtOrder() {
        return this.actualCourtOrder;
    }


    private LocalDate calculateExpiryDate(final LocalDate endDate, final Boolean isUnpaidWork) {
        if (nonNull(isUnpaidWork) && isUnpaidWork) {
            return endDate.plusYears(1);
        }
        return endDate;
    }

    private boolean hasUnpaidWorkJudicialResultType(final List<JudicialChildResults> judicialChildResults, final List<UUID> unpaidWorkJudicialTypeIds) {
        return nonNull(judicialChildResults) && judicialChildResults.stream()
                .anyMatch(judicialChildResult ->
                        unpaidWorkJudicialTypeIds.contains(judicialChildResult.getJudicialResultTypeId()));
    }

}