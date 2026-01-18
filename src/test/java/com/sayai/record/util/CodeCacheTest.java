package com.sayai.record.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CodeCacheTest {

    @Test
    void testCleanStringCorrectness() {
        assertNull(CodeCache.cleanString(null));
        assertEquals("abc", CodeCache.cleanString("abc"));
        assertEquals("abc", CodeCache.cleanString(" abc "));
        assertEquals("abc", CodeCache.cleanString("a b c"));
        assertEquals("abc", CodeCache.cleanString("a [removed] b c"));
        assertEquals("abc", CodeCache.cleanString("a[removed]b c"));
        assertEquals("abc", CodeCache.cleanString(" [ ] a [test] b c "));
        assertEquals("", CodeCache.cleanString("[all]"));
        assertEquals("", CodeCache.cleanString("   "));
        assertEquals("def", CodeCache.cleanString("d e f [ignored]"));
    }
}
