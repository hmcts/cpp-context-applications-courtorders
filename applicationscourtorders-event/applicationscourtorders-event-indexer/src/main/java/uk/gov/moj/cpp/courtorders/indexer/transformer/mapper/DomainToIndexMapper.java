package uk.gov.moj.cpp.courtorders.indexer.transformer.mapper;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.IndicatedPlea;
import uk.gov.justice.core.courts.IndicatedPleaValue;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;
import uk.gov.justice.services.unifiedsearch.client.domain.CourtOrder;
import uk.gov.justice.services.unifiedsearch.client.domain.LaaReference;
import uk.gov.justice.services.unifiedsearch.client.domain.Offence;
import uk.gov.justice.services.unifiedsearch.client.domain.Party;
import uk.gov.justice.services.unifiedsearch.client.domain.Plea;
import uk.gov.justice.services.unifiedsearch.client.domain.Verdict;
import uk.gov.justice.services.unifiedsearch.client.domain.VerdictType;
import uk.gov.moj.cpp.courtorders.command.CreateCourtOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class DomainToIndexMapper {

    public static final String PROSECUTION = "PROSECUTION";

    public Map<String, List<CaseDetails>> courtOrderOffencesToCaseDetails(final Map<UUID, CaseDetails> caseDocumentsMap,
                                                                          final CreateCourtOrder createCourtOrder,
                                                                          final List<CourtOrderOffence> courtOrderOffences) {
        courtOrderOffences.forEach(courtOrderOffence -> {
            final UUID prosecutionCaseId = courtOrderOffence.getProsecutionCaseId();
            CaseDetails caseDetailsExisting = caseDocumentsMap.get(prosecutionCaseId);
            if (isNull(caseDetailsExisting)) {
                final List<Party> parties = new ArrayList<>();
                final CaseDetails caseDetails = new CaseDetails();
                caseDetails.setCaseId(prosecutionCaseId.toString());
                caseDetails.set_case_type(PROSECUTION);
                caseDetails.setParties(parties);
                caseDetailsExisting = caseDetails;
            }

            addPartyToCaseDetails(createCourtOrder, courtOrderOffence, caseDetailsExisting);
            caseDocumentsMap.put(prosecutionCaseId, caseDetailsExisting);
        });

        final List<CaseDetails> caseDetails = caseDocumentsMap.values().stream().collect(Collectors.toList());
        final HashMap<String, List<CaseDetails>> caseDocuments = new HashMap<>();
        caseDocuments.put("caseDocuments", caseDetails);
        return caseDocuments;
    }

    private void addPartyToCaseDetails(final CreateCourtOrder createCourtOrder,
                                       final CourtOrderOffence courtOrderOffence,
                                       final CaseDetails caseDetailsExisting) {
        createCourtOrder.getDefendantIds().forEach(defendantId -> {
            final Party party = new Party();
            party.set_party_type("DEFENDANT");
            party.setPartyId(defendantId);
            party.setMasterPartyId(createCourtOrder.getMasterDefendantId().toString());

            if (containsThisParty(caseDetailsExisting, defendantId)) {
                caseDetailsExisting.getParties()
                        .stream()
                        .filter(existingParty -> existingParty.getPartyId().equals(defendantId))
                        .findAny()
                        .ifPresent(existingParty -> existingParty.getOffences()
                                .add(courtOrderOffencesToIndexOffences(courtOrderOffence, createCourtOrder)));
            } else {
                final List<Offence> offences = new ArrayList<>();
                offences.add(courtOrderOffencesToIndexOffences(courtOrderOffence, createCourtOrder));

                party.setOffences(offences);
                caseDetailsExisting.getParties().add(party);
            }
        });
    }

    private boolean containsThisParty(final CaseDetails caseDetails, final String defendantId) {
        return caseDetails.getParties()
                .stream()
                .anyMatch(party -> party.getPartyId().equals(defendantId));
    }


    private Offence courtOrderOffencesToIndexOffences(final CourtOrderOffence courtOrderOffence, final CreateCourtOrder createCourtOrder) {

        final Offence indexOffence = new Offence();
        if (nonNull(courtOrderOffence)) {

            mapNullableOffenceAttributes(courtOrderOffence.getOffence(), indexOffence);

            indexOffence.setWording(courtOrderOffence.getOffence().getWording());
            indexOffence.setLaaReference(laaReference(courtOrderOffence.getOffence()));
            indexOffence.setCourtOrders(courtOrders(createCourtOrder));
            indexOffence.setModeOfTrial(courtOrderOffence.getOffence().getModeOfTrial());
            indexOffence.setOffenceCode(courtOrderOffence.getOffence().getOffenceCode());
            indexOffence.setOffenceLegislation(courtOrderOffence.getOffence().getOffenceLegislation());
            indexOffence.setOffenceTitle(courtOrderOffence.getOffence().getOffenceTitle());
        }

        return indexOffence;
    }

    private List<CourtOrder> courtOrders(final CreateCourtOrder createCourtOrder) {
        final CourtOrder courtOrder = new CourtOrder();
        courtOrder.setId(createCourtOrder.getId().toString());
        courtOrder.setJudicialResultTypeId(createCourtOrder.getJudicialResultTypeId().toString());
        courtOrder.setLabel(createCourtOrder.getLabel());
        courtOrder.setStartDate(createCourtOrder.getStartDate().toString());
        courtOrder.setCanBeSubjectOfBreachProceedings(createCourtOrder.getCanBeSubjectOfBreachProceedings());
        courtOrder.setCanBeSubjectOfVariationProceedings(createCourtOrder.getCanBeSubjectOfVariationProceedings());
        courtOrder.setIsSJPOrder(createCourtOrder.getIsSJPOrder());
        courtOrder.setOrderDate(createCourtOrder.getOrderDate().toString());
        courtOrder.setOrderingHearingId(createCourtOrder.getOrderingHearingId().toString());

        if (nonNull(createCourtOrder.getEndDate())) {
            courtOrder.setEndDate(createCourtOrder.getEndDate().toString());
        }

        return Arrays.asList(courtOrder);
    }

    private LaaReference laaReference(final uk.gov.justice.core.courts.Offence offence) {
        final LaaReference laaReference = new LaaReference();

        final uk.gov.justice.core.courts.LaaReference laaApplnReference = offence.getLaaApplnReference();

        if (nonNull(laaApplnReference) && nonNull(laaApplnReference.getApplicationReference())) {
            laaReference.setApplicationReference(laaApplnReference.getApplicationReference());
        }

        if (nonNull(laaApplnReference) && nonNull(laaApplnReference.getStatusDescription())) {
            laaReference.setStatusDescription(laaApplnReference.getStatusDescription());
        }

        if (nonNull(laaApplnReference) && nonNull(laaApplnReference.getStatusDescription())) {
            laaReference.setStatusCode(laaApplnReference.getStatusCode());
        }

        if (nonNull(laaApplnReference) && nonNull(laaApplnReference.getStatusDescription())) {
            laaReference.setStatusId(laaApplnReference.getStatusId().toString());
        }
        return laaReference;
    }

    private void mapNullableOffenceAttributes(final uk.gov.justice.core.courts.Offence offence, final uk.gov.justice.services.unifiedsearch.client.domain.Offence offence1) {
        if (nonNull(offence.getArrestDate())) {
            offence1.setArrestDate(offence.getArrestDate().toString());
        }
        if (nonNull(offence.getChargeDate())) {
            offence1.setChargeDate(offence.getChargeDate().toString());
        }
        if (nonNull(offence.getEndDate())) {
            offence1.setEndDate(offence.getEndDate().toString());
        }
        if (nonNull(offence.getStartDate())) {
            offence1.setStartDate(offence.getStartDate().toString());
        }
        if (nonNull(offence.getDateOfInformation())) {
            offence1.setDateOfInformation(offence.getDateOfInformation().toString());
        }
        if (nonNull(offence.getId())) {
            offence1.setOffenceId(offence.getId().toString());
        }
        if (nonNull(offence.getOrderIndex())) {
            offence1.setOrderIndex(offence.getOrderIndex());
        }
        if (nonNull(offence.getProceedingsConcluded())) {
            offence1.setProceedingsConcluded(offence.getProceedingsConcluded());
        }

        ofNullable(offence.getVerdict()).ifPresent(v -> offence1.setVerdict(verdict(v)));
        ofNullable(offence.getPlea()).ifPresent(plea -> offence1.setPleas(plea(plea)));
        if ( ofNullable(offence.getIndicatedPlea()).isPresent()
                && IndicatedPleaValue.INDICATED_GUILTY.toString().equals(offence.getIndicatedPlea().getIndicatedPleaValue().toString())
                && ofNullable(offence.getPlea()).isEmpty()) {
            ofNullable(offence.getIndicatedPlea()).ifPresent(indicatedPlea -> offence1.setPleas(pleaGuilty(indicatedPlea)));
        }
    }

    private List<Plea> plea(final uk.gov.justice.core.courts.Plea pleSrc) {
        final Plea plea = new Plea();

        if (pleSrc != null) {
            if (pleSrc.getOriginatingHearingId() != null) {
                plea.setOriginatingHearingId(pleSrc.getOriginatingHearingId().toString());
            }

            if (pleSrc.getPleaValue() != null) {
                plea.setPleaValue(pleSrc.getPleaValue());
            }

            if (pleSrc.getPleaDate() != null) {
                plea.setPleaDate(pleSrc.getPleaDate().toString());
            }
        }

        return Collections.singletonList(plea);
    }

    private List<Plea> pleaGuilty(final IndicatedPlea indicatedPlea) {
        final Plea plea = new  uk.gov.justice.services.unifiedsearch.client.domain.Plea();
        plea.setPleaValue("INDICATED_GUILTY");
        plea.setPleaDate(indicatedPlea.getIndicatedPleaDate().toString());
        plea.setOriginatingHearingId(indicatedPlea.getOriginatingHearingId().toString());
        return Collections.singletonList(plea);

    }
    private Verdict verdict(final uk.gov.justice.core.courts.Verdict orgVerdict) {
        final VerdictType verdictType = new VerdictType();
        verdictType.setVerdictTypeId(orgVerdict.getVerdictType().getId().toString());
        verdictType.setCategory(orgVerdict.getVerdictType().getCategory());
        verdictType.setCategoryType(orgVerdict.getVerdictType().getCategoryType());
        ofNullable(orgVerdict.getVerdictType().getDescription()).ifPresent(verdictType::setDescription);
        ofNullable(orgVerdict.getVerdictType().getSequence()).ifPresent(verdictType::setSequence);

        final Verdict verdict = new Verdict();
        ofNullable(orgVerdict.getVerdictDate()).ifPresent(date -> verdict.setVerdictDate(date.toString()));
        verdict.setVerdictType(verdictType);
        ofNullable(orgVerdict.getOriginatingHearingId()).ifPresent(id -> verdict.setOriginatingHearingId(id.toString()));

        return verdict;
    }
}
