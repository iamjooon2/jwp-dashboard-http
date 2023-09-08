package org.apache.coyote.http11.request;

import java.util.HashMap;
import java.util.Map;

public class RequestBody {
    private static final int KEY_INDEX = 0;
    private static final int VALUE_INDEX = 1;
    private static final String NEXT_SIGN = "&";
    private static final String KEY_VALUE_DELIMITER = "=";

    private final Map<String, String> values;

    private RequestBody() {
        this(new HashMap<>());
    }

    private RequestBody(Map<String, String> values) {
        this.values = values;
    }

    public static RequestBody from(String body) {
        if (body.isBlank()) {
            return new RequestBody();
        }
        HashMap<String, String> values = new HashMap<>();
        String[] keyAndValue = body.split(NEXT_SIGN);
        for (String element : keyAndValue) {
            String[] split = element.split(KEY_VALUE_DELIMITER);
            values.put(split[KEY_INDEX], split[VALUE_INDEX]);
        }
        return new RequestBody(values);
    }

    public String getValueOf(String key) {
        return values.get(key);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

}
