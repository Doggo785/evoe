package dev.brahmkshatriya.echo.common.helpers

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class PagedDataConcatTest {

    /**
     * Concat hands each source its own first page. The second source must be asked for it with a null
     * continuation token, which is what PagedData.Continuous documents for an initial load.
     */
    @Test
    fun `second source starts with a null continuation token`() = runBlocking {
        val tokensSeen = mutableListOf<String?>()
        val concat = PagedData.Concat(
            PagedData.Single { listOf("a") },
            PagedData.Continuous { continuation ->
                tokensSeen += continuation
                Page(listOf("b"), null)
            },
        )

        concat.loadPage(null)
        concat.loadPage("1_")

        assertEquals<List<String?>>(listOf(null), tokensSeen)
    }
}
