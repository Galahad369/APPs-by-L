package com.local.localkit.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TextOperationsTest {
    @Test fun titleCaseKeepsWhitespace() {
        assertEquals("Hello   Local Kit", TextOperations.titleCase("hELLO   LOCAL kIT"))
    }

    @Test fun dedupePreservesFirstAppearance() {
        assertEquals("b\na", TextOperations.dedupeLines("b\na\nb"))
    }
}

