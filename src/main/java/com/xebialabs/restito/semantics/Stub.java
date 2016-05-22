package com.xebialabs.restito.semantics;

import org.glassfish.grizzly.http.server.Response;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.UnaryOperator;

import static com.xebialabs.restito.semantics.Action.composite;
import static com.xebialabs.restito.semantics.Action.noop;

/**
 * <p>Stub is not responsible for decision whether to execute action or not (e.g. when request matches XXX => do YYY)</p>
 * <p>Just a wrapper for the {@link Action} which will should be performed when {@link Condition} is true.</p>
 *
 * @see com.xebialabs.restito.semantics.Action
 * @see com.xebialabs.restito.semantics.Condition
 */
public class Stub {

    private Condition when = Condition.custom(Predicates.<Call>alwaysTrue());

    private Applicable action = noop();

    private List<Applicable> actionSequence = new CopyOnWriteArrayList<>();

    private int appliedTimes = 0;

    private int expectedTimes = 0;

    /**
     * Creates a stub with action and condition
     */
    public Stub(Condition when, Applicable action) {
        this.when = when;
        this.action = action;
    }

    public Stub(Condition when) {
        this.when = when;
    }

    /**
     * Creates a stub with action and condition
     */
    public Stub(Condition when, ActionSequence actionSequence) {
        this.when = when;
        this.actionSequence.addAll(actionSequence.getActions());
    }

    /**
     * Appends an extra condition to the stub.
     */
    public Stub alsoWhen(final Condition extraCondition) {
        this.when = Condition.composite(this.when, extraCondition);
        return this;
    }

    /**
     * Appends an extra action to the stub
     */
    public Stub withExtraAction(final Applicable extraAction) {
       action = composite(action, extraAction);
        return this;
    }

    public Stub withSequenceItem(final Applicable nextWhat) {
        actionSequence.add(nextWhat);
        return this;
    }

    /**
     * Checks whether the call satisfies condition of this stub
     */
    public boolean isApplicable(Call call) {
        return when.getPredicate().apply(call) && (actionSequence.size() == 0 || actionSequence.size() > appliedTimes);
    }

    /**
     * Executes all actions against the response.
     */
    public Response apply(Response response) {
        if (when instanceof ConditionWithApplicables) {
            for (Applicable applicable : ((ConditionWithApplicables) when).getApplicables()) {
                response = applicable.apply(response);
            }
        }

        Applicable chosenAction;

        if (actionSequence.isEmpty()) {
            chosenAction = action;
        } else if (actionSequence.size() > appliedTimes) {
            chosenAction = composite(action, actionSequence.get(appliedTimes));
        } else {
            chosenAction = action;
        }

        response = chosenAction.apply(response);
        appliedTimes++;
        return response;
    }

    public Stub withAction(Applicable action) {
        this.action = action;
        return this;
    }

    public void withActionSequence(List<Applicable> actionSequence) {
        this.actionSequence = new CopyOnWriteArrayList<>(actionSequence);
    }

    /**
     * How many times stub has been called
     */
    public int getAppliedTimes() {
        return appliedTimes;
    }

    /**
     * Set how many times stub expected to be called
     */
    public void setExpectedTimes(int expectedTimes) {
        this.expectedTimes = expectedTimes;
    }

    /**
     * Get how many times stub expected to be called
     */
    public int getExpectedTimes() {
        return expectedTimes;
    }
}
