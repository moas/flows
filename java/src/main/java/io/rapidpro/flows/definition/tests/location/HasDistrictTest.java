package io.rapidpro.flows.definition.tests.location;

import com.google.gson.JsonObject;
import io.rapidpro.expressions.EvaluatedTemplate;
import io.rapidpro.expressions.EvaluationContext;
import io.rapidpro.flows.definition.Flow;
import io.rapidpro.flows.definition.FlowParseException;
import io.rapidpro.flows.definition.tests.Test;
import io.rapidpro.flows.runner.Location;
import io.rapidpro.flows.runner.RunState;
import io.rapidpro.flows.runner.Runner;
import io.rapidpro.flows.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Test that returns whether the text contains the valid state
 */
public class HasDistrictTest extends Test {

    public static final String TYPE = "district";

    protected String m_state;

    public HasDistrictTest(String state) {
        m_state = state;
    }

    /**
     * @see Test#fromJson(JsonObject, Flow.DeserializationContext)
     */
    public static HasDistrictTest fromJson(JsonObject obj, Flow.DeserializationContext context) throws FlowParseException {
        return new HasDistrictTest(JsonUtils.getAsString(obj, "test"));
    }

    /**
     * @see Test#evaluate(Runner, RunState, EvaluationContext, String)
     */
    @Override
    public Result evaluate(Runner runner, RunState run, EvaluationContext context, String text) {
        String country = run.getOrg().getCountry();
        if (StringUtils.isNotEmpty(country)) {
            // state might be an expression
            EvaluatedTemplate state = runner.substituteVariables(m_state, context);

            if (!state.hasErrors()) {
                Location location = runner.getLocationResolver().resolve(text, country, Location.Level.DISTRICT, state.getOutput());
                if (location != null) {
                    return Result.match(location.getName());
                }
            }
        }

        return Result.NO_MATCH;
    }

    public String getState() {
        return m_state;
    }
}
