package io.rapidpro.flows.runner;

import com.google.gson.annotations.SerializedName;
import io.rapidpro.expressions.EvaluatedTemplate;
import io.rapidpro.expressions.EvaluationContext;
import io.rapidpro.expressions.Expressions;
import io.rapidpro.flows.definition.Action;
import io.rapidpro.flows.definition.Flow;
import io.rapidpro.flows.definition.TranslatableText;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class RunState {

    protected static Expressions.TemplateEvaluator s_evaluator = Expressions.getTemplateEvaluator();

    @SerializedName("org")
    protected Org m_org;

    @SerializedName("contact")
    protected Contact m_contact;

    @SerializedName("flow")
    protected Flow m_flow;

    @SerializedName("step")
    protected StepState m_stepState;

    @SerializedName("actions")
    protected List<Action.Result> m_actions;

    public RunState(Org org, Contact contact, Flow flow) {
        m_org = org;
        m_contact = contact;
        m_flow = flow;
        m_stepState = new StepState();
        m_actions = new ArrayList<>();
    }

    public Org getOrg() {
        return m_org;
    }

    public Contact getContact() {
        return m_contact;
    }

    public Flow getFlow() {
        return m_flow;
    }

    public StepState getStepState() {
        return m_stepState;
    }

    public List<Action.Result> getActions() {
        return m_actions;
    }

    public EvaluatedTemplate substituteVariables(String text, EvaluationContext context) {
        // TODO update context

        return s_evaluator.evaluateTemplate(text, context);
    }

    public EvaluationContext buildContext() {
        Map<String, Object> variables = new HashMap<>();

        variables.put("contact", m_contact.buildContext(m_org));

        // TODO
        // variables.put("flow", m_stepState.buildContext(m_org));

        return new EvaluationContext(variables, m_org.getTimezone(), m_org.isDayFirst());
    }
}