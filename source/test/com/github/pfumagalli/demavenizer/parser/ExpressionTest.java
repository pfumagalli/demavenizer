package com.github.pfumagalli.demavenizer.parser;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ExpressionTest {

    private final Map<Object, Object> context = new HashMap<>();
    private final URI uri = URI.create("http://www.github.com/");

    @BeforeClass
    @SuppressWarnings("unused")
    public void initialize() {
        context.put("string", "theStringValue");
        context.put("bean", new Object() {
            public String getString() {
                return "theBeanString";
            }
            public Map<?, ?> getSubMap() {
                final Map<Object, Object> map = new HashMap<>();
                map.put("subkey", "theSubMapString");
                return map;
            }
            public Object getSubBean() {
                return new Object() {
                    public Number getValue() {
                        return 3.14;
                    }
                };
            }
        });
    }

    private void assertExpression(String expression, String expectedResult) {
        final String result = Expression.parse(expression, uri).evaluate(context);
        Assert.assertEquals(result, expectedResult, expression);
    }

    @Test
    public void testNoExpression() {
        assertExpression("thisIsNothing", "thisIsNothing");
    }

    @Test
    public void testSimpleExpression() {
        assertExpression("hello[${string}]world", "hello[theStringValue]world");
    }

    @Test
    public void testManyExpressions() {
        assertExpression("${string}${string}${string}", "theStringValuetheStringValuetheStringValue");
        assertExpression("[${string}]{${string}}", "[theStringValue]{theStringValue}");
    }


    @Test
    public void testOnlyExpression() {
        assertExpression("${string}", "theStringValue");
    }

    @Test
    public void testStstemProperty() {
        assertExpression("${user.dir}", System.getProperty("user.dir"));
    }

    @Test
    public void testBeanGetter() {
        assertExpression("${bean.string}", "theBeanString");
        assertExpression("${bean.subMap.subkey}", "theSubMapString");
        assertExpression("${bean.subBean.value}", "3.14");
    }

}
