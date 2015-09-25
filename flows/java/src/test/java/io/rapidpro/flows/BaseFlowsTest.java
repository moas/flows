package io.rapidpro.flows;

import io.rapidpro.expressions.dates.DateStyle;
import io.rapidpro.flows.definition.GroupRef;
import io.rapidpro.flows.definition.TranslatableText;
import io.rapidpro.flows.definition.actions.Action;
import io.rapidpro.flows.definition.actions.group.AddToGroupsAction;
import io.rapidpro.flows.definition.actions.message.ReplyAction;
import io.rapidpro.flows.runner.Contact;
import io.rapidpro.flows.runner.ContactUrn;
import io.rapidpro.flows.runner.Location;
import io.rapidpro.flows.runner.Org;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.threeten.bp.ZoneId;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Base class for project tests
 */
@Ignore
public abstract class BaseFlowsTest {

    protected Org m_org;

    protected Contact m_contact;

    @Before
    public void initBaseData() throws Exception {
        m_org = new Org("RW", "eng", ZoneId.of("Africa/Kigali"), DateStyle.DAY_FIRST, false);

        Map<String, String> contactFields = new HashMap<>();
        contactFields.put("gender", "M");
        contactFields.put("age", "34");

        m_contact = new Contact(
                "1234-1234",
                "Joe Flow",
                new ArrayList<>(Arrays.asList(
                        ContactUrn.fromString("tel:+260964153686"),
                        ContactUrn.fromString("twitter:realJoeFlow")
                )),
                new LinkedHashSet<>(Arrays.asList("Testers", "Developers")),
                contactFields,
                "eng"
        );
    }

    public String readResource(String resource) throws IOException {
        return IOUtils.toString(getClass().getClassLoader().getResourceAsStream(resource));
    }

    /**
     * Location parser for testing which has one state (Kigali) and one district (Gasabo)
     */
    public static class TestLocationResolver implements Location.Resolver {

        @Override
        public Location resolve(String input, String country, Location.Level level, String parent) {
            if (level == Location.Level.STATE && input.trim().equalsIgnoreCase("Kigali")) {
                return new Location("S0001", "Kigali", Location.Level.STATE);
            } else if (level == Location.Level.DISTRICT && input.trim().equalsIgnoreCase("Gasabo") && parent.trim().equalsIgnoreCase("Kigali")) {
                return new Location("D0001", "Gasabo", Location.Level.DISTRICT);
            } else {
                return null;
            }
        }
    }

    protected void assertReply(Action action, String msg) {
        assertThat(action, instanceOf(ReplyAction.class));
        assertThat(((ReplyAction) action).getMsg(), is(new TranslatableText(msg)));
    }

    protected void assertAddToGroup(Action action, String... groupNames) {
        assertThat(action, instanceOf(AddToGroupsAction.class));

        List<String> names = new ArrayList<>();
        for (GroupRef group : ((AddToGroupsAction) action).getGroups()) {
            names.add(group.getName());
        }
        assertThat(names, is(Arrays.asList(groupNames)));
    }

    public Org getOrg() {
        return m_org;
    }

    public Contact getContact() {
        return m_contact;
    }
}
