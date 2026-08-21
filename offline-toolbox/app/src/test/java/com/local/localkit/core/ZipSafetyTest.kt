package com.local.localkit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ZipSafetyTest {
    @Test fun safeNestedPathIsPreserved() {
        assertEquals("folder/file.txt", FileOperations.validateZipPath("folder/file.txt"))
    }

    @Test fun zipSlipIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { FileOperations.validateZipPath("../../private.txt") }
        assertThrows(IllegalArgumentException::class.java) { FileOperations.validateZipPath("C:\\private.txt") }
        assertThrows(IllegalArgumentException::class.java) { FileOperations.validateZipPath("/private.txt") }
    }
}

