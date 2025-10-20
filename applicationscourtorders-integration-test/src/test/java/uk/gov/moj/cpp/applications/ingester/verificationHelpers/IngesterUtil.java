package uk.gov.moj.cpp.applications.ingester.verificationHelpers;

import uk.gov.justice.services.test.utils.core.messaging.Poller;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class IngesterUtil {

    private static final Poller poller = new Poller(100, 1000L);
    
    public static JsonObject jsonFromString(final String jsonObjectStr) {
        JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }

    public static Poller getPoller() {
        return poller;
    }
}
