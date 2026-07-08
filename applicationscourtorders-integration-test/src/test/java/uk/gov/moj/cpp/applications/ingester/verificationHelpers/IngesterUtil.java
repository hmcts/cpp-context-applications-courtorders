package uk.gov.moj.cpp.applications.ingester.verificationHelpers;

import static uk.gov.justice.services.messaging.JsonObjects.createReader;

import uk.gov.justice.services.test.utils.core.messaging.Poller;

import java.io.StringReader;

import javax.json.JsonObject;
import javax.json.JsonReader;

public class IngesterUtil {

    private static final Poller poller = new Poller(100, 1000L);

    public static JsonObject jsonFromString(final String jsonObjectStr) {
        JsonReader jsonReader = createReader(new StringReader(jsonObjectStr));
        JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }

    public static Poller getPoller() {
        return poller;
    }
}
