package dev.brahmkshatriya.echo.utils

/**
 * Turns release-please markdown into plain blocks the update sheet styles
 * natively. Pure JVM — no Android imports — so it stays unit-testable.
 *
 * Only the shapes release-please actually emits are understood: the `##`
 * title line, `###` section headers, and `*` bullets with an optional
 * `**scope:**` lead. Links keep their text, commit hashes and issue refs in
 * parens are dropped. Anything else passes through as plain prose.
 */
object ReleaseNotes {

    sealed interface Block {
        data class SubHeader(val text: String) : Block
        data class Bullet(val scope: String?, val text: String) : Block
        data class Paragraph(val text: String) : Block
    }

    private val headerDate = Regex("""\((\d{4}-\d{2}-\d{2})\)\s*$""")
    private val scopeLead = Regex("""^\*\*([^*]+)\*\*\s*""")
    private val link = Regex("""\[([^\]]*)\]\([^)]*\)""")
    private val hashParen = Regex("""\(\s*(?:[0-9a-f]{7,40}|#[0-9]+)\s*\)""")
    private val emptyParen = Regex("""\(\s*\)""")
    private val bold = Regex("""\*\*""")
    private val spaces = Regex("""[ \t]+""")

    private const val CLEANUP_PASSES = 3

    /** The `(yyyy-mm-dd)` date of the `##` title line, or null without one. */
    fun date(body: String?): String? {
        val header = body?.lineSequence()?.map { it.trim() }
            ?.firstOrNull { it.startsWith("## ") } ?: return null
        return headerDate.find(header)?.groupValues?.getOrNull(1)
    }

    /** Content blocks of [body], title line excluded. */
    fun blocks(body: String?): List<Block> {
        if (body.isNullOrBlank()) return emptyList()
        return body.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val block: Block? = when {
                    line.startsWith("### ") -> Block.SubHeader(cleanInline(line.removePrefix("### ")))
                    line.startsWith("## ") -> null
                    line.startsWith("* ") || line.startsWith("- ") -> bullet(line)
                    else -> Block.Paragraph(cleanInline(line))
                }
                block
            }.toList()
    }

    private fun bullet(line: String): Block.Bullet {
        val text = line.removePrefix("* ").removePrefix("- ")
        val match = scopeLead.find(text)
        val scope = match?.groupValues?.getOrNull(1)?.trim()
        val rest = match?.let { text.substring(it.range.last + 1) } ?: text
        return Block.Bullet(scope?.takeIf { it.isNotEmpty() }, cleanInline(rest))
    }

    private fun cleanInline(raw: String): String {
        var text = link.replace(raw, "$1")
        repeat(CLEANUP_PASSES) {
            text = hashParen.replace(text, "")
            text = emptyParen.replace(text, "")
        }
        text = bold.replace(text, "")
        return spaces.replace(text, " ").trim()
    }
}
