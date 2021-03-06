package org.mockserver.matchers;

import io.netty.handler.codec.http.QueryStringDecoder;
import org.mockserver.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jamesdbloom
 */
public class ParameterStringMatcher extends BodyMatcher<String> implements Matcher<String> {
    private static Logger logger = LoggerFactory.getLogger(ParameterStringMatcher.class);
    private final MapMatcher matcher;

    public ParameterStringMatcher(List<Parameter> parameters) {
        this.matcher = new MapMatcher(KeyToMultiValue.toMultiMap(parameters));
    }

    public boolean matches(String matched) {
        boolean result = false;

        if (matcher.matches(parseString(matched))) {
            result = true;
        }

        if (!result) {
            logger.trace("Failed to match [{}] with [{}]", matched, this.matcher);
        }

        return result;
    }

    private List<KeyToMultiValue> parseString(String matched) {
        Map<String, Parameter> mappedParameters = new HashMap<String, Parameter>();
        Map<String, List<String>> parameters = new QueryStringDecoder("?" + matched).parameters();
        for (String name : parameters.keySet()) {
            for (String value : parameters.get(name)) {
                if (mappedParameters.containsKey(name)) {
                    mappedParameters.get(name).addValue(value);
                } else {
                    mappedParameters.put(name, new Parameter(name, value));
                }
            }
        }
        return new ArrayList<KeyToMultiValue>(mappedParameters.values());
    }

    @Override
    public String[] fieldsExcludedFromEqualsAndHashCode() {
        return new String[]{"logger"};
    }
}
