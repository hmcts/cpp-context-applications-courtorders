package uk.gov.moj.cpp.courtorders.healthchecks;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.healthcheck.healthchecks.JobStoreHealthcheck.JOB_STORE_HEALTHCHECK_NAME;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtOrdersIgnoredHealthcheckNamesProviderTest {

    @InjectMocks
    private CourtOrdersIgnoredHealthcheckNamesProvider courtOrdersIgnoredHealthcheckNamesProvider;

    @Test
    public void shouldIgnoreJobStoreHealthchecks() throws Exception {

        final List<String> namesOfIgnoredHealthChecks = courtOrdersIgnoredHealthcheckNamesProvider.getNamesOfIgnoredHealthChecks();

        assertThat(namesOfIgnoredHealthChecks.size(), is(1));
        assertThat(namesOfIgnoredHealthChecks, hasItems(JOB_STORE_HEALTHCHECK_NAME));
    }
}