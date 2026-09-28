package dev.brahmkshatriya.echo.ui.common

import androidx.paging.PagingSource
import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PagedSource.load contract after the IO move.
 * Pure JVM: fakes construct PagedData directly, no views, no paging runtime.
 *
 * SCOPE NOTE: dispatcher placement itself is read in code (withContext around
 * the whole body). What is pinned here is what the move could have broken:
 * the page/error routing, including the dropped non-local return that no
 * longer compiles inside the IO lambda.
 */
class PagedSourceLoadTest {

    private fun params(key: String? = null) =
        PagingSource.LoadParams.Refresh<String>(
            key = key,
            loadSize = 10,
            placeholdersEnabled = false
        )

    @Test
    fun `loaded page returns its data with its continuation`() = runBlocking {
        val source = PagedSource(
            loaded = Result.success(
                PagedData.Continuous { Page(listOf("a", "b"), "next") }
            )
        )
        val result = source.load(params()) as PagingSource.LoadResult.Page<String, String>
        assertEquals(listOf("a", "b"), result.data)
        assertEquals("next", result.nextKey)
    }

    @Test
    fun `failed load falls back to the cached page`() = runBlocking {
        val source = PagedSource(
            loaded = Result.failure(RuntimeException("boom")),
            cached = Result.success(PagedData.Single { listOf("c") })
        )
        val result = source.load(params()) as PagingSource.LoadResult.Page<String, String>
        assertEquals(listOf("c"), result.data)
    }

    @Test
    fun `failed load without cache is an error`() = runBlocking {
        val source = PagedSource<String>(
            loaded = Result.failure(RuntimeException("boom"))
        )
        val result = source.load(params())
        assertTrue(result is PagingSource.LoadResult.Error<String, String>)
    }

    @Test
    fun `load runs off the calling thread`() = runBlocking {
        val caller = Thread.currentThread().name
        var worker = ""
        val source = PagedSource(
            loaded = Result.success(
                PagedData.Continuous {
                    worker = Thread.currentThread().name
                    Page(listOf("a"), null)
                }
            )
        )
        source.load(params())
        assertTrue(worker.isNotEmpty() && worker != caller)
    }
}
