package com.github.pfumagalli.demavenizer.maven;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class IdentifierTest {

    public void assertParse(String identifier, String groupId, String artifactId, String version) {
        final Identifier id = new Identifier(identifier);
        assertEquals(id.getGroupId(), groupId, "Wrong group ID parsed from \"" + identifier + "\"");
        assertEquals(id.getArtifactId(), artifactId, "Wrong artifact ID parsed from \"" + identifier + "\"");
        assertEquals(id.getVersion(), version, "Wrong version parsed from \"" + identifier + "\"");
    }

    @Test
    public void testParsing() {
        assertParse("group#artifact#version", "group", "artifact", "version");
        assertParse(" group#artifact#version ", "group", "artifact", "version");
        assertParse("group # artifact # version", "group", "artifact", "version");
        assertParse(" group # artifact # version ", "group", "artifact", "version");
        assertParse("group#artifact", "group", "artifact", null);
        assertParse(" group#artifact ", "group", "artifact", null);
        assertParse("group # artifact", "group", "artifact", null);
        assertParse(" group # artifact ", "group", "artifact", null);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testFail1() {
        assertParse("group#artifact#", "group", "artifact", null);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testFail2() {
        assertParse("group#", "group", "artifact", null);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testFail3() {
        assertParse("#", "group", "artifact", null);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testFail4() {
        assertParse("", "group", "artifact", null);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testFail5() {
        assertParse("random", "group", "artifact", null);
    }

}
