package io.rapidpro.expressions.evaluator;

import io.rapidpro.expressions.*;
import io.rapidpro.expressions.functions.FunctionManager;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * The template evaluator
 */
public class TemplateEvaluator {

    protected static Logger logger = LoggerFactory.getLogger(TemplateEvaluator.class);

    private FunctionManager m_functionManager = new FunctionManager();

    private char m_expressionPrefix;

    public TemplateEvaluator(char expressionPrefix, List<Class<?>> functionLibraries) {
        m_expressionPrefix = expressionPrefix;

        for (Class<?> functionLibrary : functionLibraries) {
            m_functionManager.addLibrary(functionLibrary);
        }
    }

    /**
     * Templates support 2 forms of embedded expression:
     *  1. Single variable, e.g. @contact, @contact.name (delimited by character type or end of input)
     #  2. Contained expression, e.g. @(SUM(1, 2) + 2) (delimited by balanced parentheses)
     */
    private enum State {
        BODY,               // not in a expression
        PREFIX,             // '@' prefix that denotes the start of an expression
        IDENTIFIER,         // the identifier part, e.g. 'SUM' in '@SUM(1, 2)' or 'contact.age' in '@contact.age'
        BALANCED,           // the balanced parentheses delimited part, e.g. '(1 + 2)' in '@(1 + 2)'
        STRING_LITERAL,     // a string literal which could contain )
        ESCAPED_PREFIX      // a '@' prefix preceded by another '@'
    }

    /**
     * Determines whether the given character is a word character, i.e. \w in a regex
     */
    private static boolean isWordChar(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '_';
    }

    /**
     * Evaluates a template string, e.g. "Hello @contact.name you have @(contact.reports * 2) reports"
     * @param template the template string
     * @param context the evaluation context
     * @return a tuple of the evaluated template and a list of evaluation errors
     */
    public EvaluatedTemplate evaluateTemplate(String template, EvaluationContext context) {
        return evaluateTemplate(template, context, false);
    }

    /**
     * Evaluates a template string, e.g. "Hello @contact.name you have @(contact.reports * 2) reports"
     * @param template the template string
     * @param context the evaluation context
     * @param urlEncode whether or not values should be URL encoded
     * @return a tuple of the evaluated template and a list of evaluation errors
     */
    public EvaluatedTemplate evaluateTemplate(String template, EvaluationContext context, boolean urlEncode) {
        char[] inputChars = template.toCharArray();
        StringBuilder output = new StringBuilder();
        List<String> errors = new ArrayList<>();
        State state = State.BODY;
        StringBuilder currentExpression = new StringBuilder();
        boolean currentExpressionTerminated = false;
        int parenthesesLevel = 0;

        for (int pos = 0; pos < inputChars.length; pos++) {
            char ch = inputChars[pos];

            // in order to determine if the b in a.b terminates an identifier, we have to peek two characters ahead as
            // it could be a.b. (b terminates) or a.b.c (b doesn't terminate)
            char nextCh = (pos < (inputChars.length - 1)) ? inputChars[pos + 1] : 0;
            char nextNextCh = (pos < (inputChars.length - 2)) ? inputChars[pos + 2] : 0;

            if (state == State.BODY) {
                if (ch == m_expressionPrefix && (isWordChar(nextCh) || nextCh == '(')) {
                    state = State.PREFIX;
                    currentExpression = new StringBuilder("" + ch);
                } else if (ch == m_expressionPrefix && nextCh == m_expressionPrefix) {
                    state = State.ESCAPED_PREFIX;
                } else {
                    output.append(ch);
                }
            }
            else if (state == State.PREFIX) {
                if (isWordChar(ch)) {
                    state = State.IDENTIFIER; // we're parsing an expression like =XXX
                } else if (ch == '(') {
                    // we're parsing an expression like =(1 + 2)
                    state = State.BALANCED;
                    parenthesesLevel += 1;
                }

                currentExpression.append(ch);
            }
            else if (state == State.IDENTIFIER) {
                currentExpression.append(ch);
            }
            else if (state == State.BALANCED) {
                if (ch == '(') {
                    parenthesesLevel += 1;
                } else if (ch == ')') {
                    parenthesesLevel -= 1;
                } else if (ch == '"') {
                    state = State.STRING_LITERAL;
                }

                currentExpression.append(ch);

                // expression terminates if parentheses balance
                if (parenthesesLevel == 0) {
                    currentExpressionTerminated = true;
                }
            }
            else if (state == State.STRING_LITERAL) {
                if (ch == '"') {
                    state = State.BALANCED;
                }
                currentExpression.append(ch);
            }
            else if (state == State.ESCAPED_PREFIX) {
                state = State.BODY;
                output.append(ch);
            }

            // identifier can terminate expression in 3 ways:
            //  1. next char is null (i.e. end of the input)
            //  2. next char is not a word character or period
            //  3. next char is a period, but it's not followed by a word character
            if (state == State.IDENTIFIER) {
                if (nextCh == 0  || (!isWordChar(nextCh) && nextCh != '.') || (nextCh == '.' && ! isWordChar(nextNextCh))) {
                    currentExpressionTerminated = true;
                }
            }

            if (currentExpressionTerminated) {
                output.append(resolveExpression(currentExpression.toString(), context, urlEncode, errors));
                currentExpression = null;
                currentExpressionTerminated = false;
                state = State.BODY;
            }
        }

        // if last expression didn't terminate - add to output as is
        if (!currentExpressionTerminated && StringUtils.isNotEmpty(currentExpression)) {
            output.append(currentExpression.toString());
        }

        return new EvaluatedTemplate(output.toString(), errors);
    }

    /**
     * Resolves an expression found in the template. If an evaluation error occurs, expression is returned as is.
     */
    private String resolveExpression(String expression, EvaluationContext context, boolean urlEncode, List<String> errors) {
        try {
            String cleaned = expression.substring(1); // strip @ prefix
            Object evaluated = evaluateExpression(cleaned, context);

            String rendered = Conversions.toString(evaluated, context); // render result as string
            return urlEncode ? URLEncoder.encode(rendered, "UTF-8") : rendered;
        }
        catch (EvaluationError ex) {
            logger.debug("Unable to evaluate expression", ex);
            errors.add(ex.getMessage());

            return expression; // if we can't evaluate expression, include it as is in the output
        }
        catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Evaluates a single expression, e.g. "contact.reports * 2"
     * @param expression the expression string
     * @param context the evaluation context
     * @return the evaluated expression value
     * @throws EvaluationError if an error occurs during evaluation
     */
    public Object evaluateExpression(String expression, EvaluationContext context) throws EvaluationError {
        ExcellentLexer lexer = new ExcellentLexer(new ANTLRInputStream(expression));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        ExcellentParser parser = new ExcellentParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        ParseTree tree;

        try {
            tree = parser.input();

            if (logger.isDebugEnabled()) {
                logger.info("Expression '{}' parsed as {}", expression, tree.toStringTree());
            }
        }
        catch (ParseCancellationException ex) {
            throw new EvaluationError("Expression is invalid", ex);
        }

        ExcellentVisitor visitor = new ExpressionVisitorImpl(m_functionManager, context);
        return visitor.visit(tree);
    }
}