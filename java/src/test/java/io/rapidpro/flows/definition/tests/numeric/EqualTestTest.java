package io.rapidpro.flows.definition.tests.numeric;

import io.rapidpro.flows.definition.tests.BaseTestTest;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * Test for {@link EqualTest}
 */
public class EqualTestTest extends BaseTestTest {
    @Test
    public void evaluate() {
        EqualTest test = new EqualTest("32 ");
        assertTest(test, "3l", false, null);
        assertTest(test, "32", true, "32", new BigDecimal(32));
        assertTest(test, "33", false, null);

        // test can be an expression
        test = new EqualTest("@(contact.age - 2)");

        assertTest(test, "3l", false, null);
        assertTest(test, "32", true, "32", new BigDecimal(32));
        assertTest(test, "33", false, null);
    }
}
