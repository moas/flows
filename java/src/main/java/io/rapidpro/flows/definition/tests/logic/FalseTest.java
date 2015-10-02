package io.rapidpro.flows.definition.tests.logic;

import com.google.gson.JsonObject;
import io.rapidpro.expressions.EvaluationContext;
import io.rapidpro.flows.definition.Flow;
import io.rapidpro.flows.definition.tests.Test;
import io.rapidpro.flows.runner.RunState;
import io.rapidpro.flows.runner.Runner;

/**
 * Test that always returns false
 */
public class FalseTest extends Test {

    public static final String TYPE = "false";

    /**
     * @see Test#fromJson(JsonObject, Flow.DeserializationContext)
     */
    public static FalseTest fromJson(JsonObject obj, Flow.DeserializationContext context) {
        return new FalseTest();
    }

    /**
     * @see Test#evaluate(Runner, RunState, EvaluationContext, String)
     */
    @Override
    public Result evaluate(Runner runner, RunState run, EvaluationContext context, String text) {
        return new Result(false, text, text);
    }
}
