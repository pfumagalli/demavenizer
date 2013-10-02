package com.github.pfumagalli.demavenizer.parser;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.github.pfumagalli.demavenizer.Log;

public abstract class Expression {

    private Expression() {
        super();
    }

    public static Expression parse(String expression, URI uri) {
        final List<Expression> expressions = new ArrayList<>();

        int position = 0;
        while (true) {
            /* Check for start of expression */
            final int start = expression.indexOf("${", position);
            if (start < 0) {
                final String string = expression.substring(position);
                if (!string.isEmpty()) expressions.add(new StringExpression(string));
                break;
            }

            /* Check for end of expression */
            final int end = expression.indexOf("}", start + 2);
            if (end < 0) {
                throw new IllegalArgumentException("Unterminated expression in " + expression);
            }

            /* Create our expression and move on to the next token */
            final String string = expression.substring(position, start);
            if (!string.isEmpty()) expressions.add(new StringExpression(string));
            expressions.add(new ContextExpression(uri, expression.substring(start + 2, end)));
            position = end + 1;
        }

        return expressions.size() == 0 ? new EmptyExpression() :
               expressions.size() == 1 ? expressions.get(0) :
               new AggregateExpression(expressions);
    }

    public String evaluate(Map<?, ?> context) {
        return this.evaluate(context, false);
    }

    public abstract String evaluate(Map<?, ?> context, boolean ignoreMissing);

    /* ====================================================================== */

    private static class EmptyExpression extends Expression {

        private EmptyExpression() {
            super();
        }

        @Override
        public String evaluate(Map<?, ?> context, boolean ignoreMissing) {
            return "";
        }

    }

    /* ====================================================================== */
    private static class AggregateExpression extends Expression {

        private final List<Expression> expressions;

        private AggregateExpression(List<Expression> expressions) {
            assert(expressions != null): "Null expressions list";
            this.expressions = expressions;
        }

        @Override
        public String evaluate(Map<?, ?> context, boolean ignoreMissing) {
            final StringBuilder builder = new StringBuilder();
            for (final Expression expression: expressions)
                builder.append(expression.evaluate(context, ignoreMissing));
            return builder.toString();
        }
    }

    /* ====================================================================== */

    private static class StringExpression extends Expression {

        private final String token;

        private StringExpression(String token) {
            assert(token != null): "Null token";
            this.token = token;
        }

        @Override
        public String evaluate(Map<?, ?> context, boolean ignoreMissing) {
            return token;
        }
    }

    /* ====================================================================== */

    private static class ContextExpression extends Expression {

        private final List<String> tokens = new ArrayList<>();
        private final String expression;
        private final URI uri;

        private ContextExpression(URI uri, String expression) {
            this.uri = uri;
            assert(expression != null): "Null expression";

            final StringBuilder builder = new StringBuilder();
            final StringTokenizer tokenizer = new StringTokenizer(expression, ".");

            while (tokenizer.hasMoreTokens()) {
                final String token = tokenizer.nextToken().trim();
                builder.append('.').append(token);
                tokens.add(token);
            }

            try {
                this.expression = builder.substring(1);
            } catch (final IndexOutOfBoundsException exception) {
                throw new IllegalArgumentException("Invalid expression ${" + expression + "}");
            }
        }

        @Override
        public String evaluate(Map<?, ?> context, boolean ignoreMissing) {

            /* No context? Bye! */
            if (context == null) return null;

            /* Check the key in the map */
            Object object = context.get(expression);
            if (object != null) return object.toString();

            /* Try to resolve the value */
            object = context;
            for (final String token: tokens) object = evaluate(token, object);
            if (object != null) return object.toString();

            /* Try to get as a system property */
            object = System.getProperty(expression);
            if (object != null) return object.toString();

            /* Ignore missing or warn */
            if (ignoreMissing) return "${" + expression + "}";
            Log.error("Unable to resolve ${" + expression + "} found in " + uri);
            return null;
        }

        private Object evaluate(String token, Object context) {
            /* Ignore nulls */
            if (context == null) return null;

            /* Try getting the value out of a Map */
            if (context instanceof Map) return ((Map<?, ?>) context).get(token);

            /* Try calling a method */
            final String methodName = "get" +
                                      token.substring(0, 1).toUpperCase() +
                                      token.substring(1);
            try {
                final Method method = context.getClass().getMethod(methodName);
                return method.invoke(context);
            } catch (final NoSuchMethodException
                         | IllegalAccessException
                         | InvocationTargetException exception) {
                /* Swallow method exceptions */
            }

            /* Try accessing a field */
            try {
                final Field field = context.getClass().getField(token);
                return field.get(context);
            } catch(final NoSuchFieldException
                        | IllegalAccessException error) {
                /* Swallow field exceptions */
            }

            /* Nada, return null */
            return null;
        }
    }
}
