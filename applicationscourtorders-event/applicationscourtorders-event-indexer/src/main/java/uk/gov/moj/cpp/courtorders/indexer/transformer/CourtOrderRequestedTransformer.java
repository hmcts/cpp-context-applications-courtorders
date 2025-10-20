package uk.gov.moj.cpp.courtorders.indexer.transformer;


import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.CourtOrderRequested;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;
import uk.gov.moj.cpp.courtorders.command.CreateCourtOrder;
import uk.gov.moj.cpp.courtorders.indexer.transformer.mapper.DomainToIndexMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;

import com.bazaarvoice.jolt.Transform;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class CourtOrderRequestedTransformer implements Transform {

    private DomainToIndexMapper domainToIndexMapper = new DomainToIndexMapper();

    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Override
    public Object transform(final Object input) {
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(input);

        final CourtOrderRequested courtOrderRequested = jsonObjectToObjectConverter.convert(jsonObject, CourtOrderRequested.class);

        final Map<UUID, CaseDetails> caseDocumentsMap = new HashMap<>();

        final CreateCourtOrder createCourtOrder = courtOrderRequested.getCourtOrder();
        final List<CourtOrderOffence> courtOrderOffences = createCourtOrder.getCourtOrderOffences();

        return domainToIndexMapper.courtOrderOffencesToCaseDetails(caseDocumentsMap, createCourtOrder, courtOrderOffences);
    }
}