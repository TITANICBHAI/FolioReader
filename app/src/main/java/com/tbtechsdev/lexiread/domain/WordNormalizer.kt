package com.tbtechsdev.lexiread.domain

object WordNormalizer {

    private val PUNCTUATION_REGEX = Regex("""[^\p{L}\p{N}]+""")

    operator fun invoke(input: String): String = normalize(input)

    fun normalize(input: String): String {
        // Return lowercase, punctuation-stripped result
        val cleaned = input.lowercase().replace(PUNCTUATION_REGEX, "").trim()
        if (cleaned.length < 3) return cleaned

        // Strip common English suffixes: -ing, -ed, -s, -es, -ly, -er, -est (basic heuristic)
        val uninflected = when {
            cleaned.endsWith("ing") && cleaned.length >= 5 -> {
                val base = cleaned.removeSuffix("ing")
                if (base.length >= 3 && base[base.length - 1] == base[base.length - 2] &&
                    base.last() !in listOf('s', 'l', 'z')
                ) {
                    base.dropLast(1)
                } else base
            }
            cleaned.endsWith("est") && cleaned.length >= 5 -> {
                val base = cleaned.removeSuffix("est")
                if (base.length >= 3 && base[base.length - 1] == base[base.length - 2] &&
                    base.last() !in listOf('s', 'l', 'z')
                ) {
                    base.dropLast(1)
                } else base
            }
            cleaned.endsWith("es") && cleaned.length >= 4 -> cleaned.removeSuffix("es")
            cleaned.endsWith("ed") && cleaned.length >= 5 -> {
                val base = cleaned.removeSuffix("ed")
                if (base.length >= 3 && base[base.length - 1] == base[base.length - 2] &&
                    base.last() !in listOf('s', 'l', 'z')
                ) {
                    base.dropLast(1)
                } else base
            }
            cleaned.endsWith("ly") && cleaned.length >= 5 -> cleaned.removeSuffix("ly")
            cleaned.endsWith("er") && cleaned.length >= 5 -> {
                val base = cleaned.removeSuffix("er")
                if (base.length >= 3 && base[base.length - 1] == base[base.length - 2] &&
                    base.last() !in listOf('s', 'l', 'z')
                ) {
                    base.dropLast(1)
                } else base
            }
            cleaned.endsWith("s") && !cleaned.endsWith("ss") && cleaned.length >= 4 -> cleaned.removeSuffix("s")
            else -> cleaned
        }

        return uninflected
    }
}
