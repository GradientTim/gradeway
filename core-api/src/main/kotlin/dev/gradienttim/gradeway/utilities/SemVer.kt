/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.utilities

data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val build: String? = null,
    val preRelease: String? = null
) : Comparable<SemVer> {
    init {
        require(major >= 0 && minor >= 0 && patch >= 0) { "Version components must be non-negative" }
        require(preRelease == null || PRE_RELEASE.matches(preRelease)) { "Invalid pre-release: '$preRelease'" }
        require(build == null || BUILD.matches(build)) { "Invalid build metadata: '$build'" }
    }

    override fun compareTo(other: SemVer): Int =
        compareValuesBy(this, other, SemVer::major, SemVer::minor, SemVer::patch)
            .takeIf { it != 0 }
            ?: comparePreRelease(preRelease, other.preRelease)

    override fun toString(): String = buildString {
        append("$major.$minor.$patch")
        preRelease?.let { append('-').append(it) }
        build?.let { append('+').append(it) }
    }

    companion object {
        private const val NUMERIC = """0|[1-9]\d*"""
        private const val PRE_RELEASE_ID = """(?:$NUMERIC|\d*[a-zA-Z-][0-9a-zA-Z-]*)"""
        private const val BUILD_ID = """[0-9a-zA-Z-]+"""
        private const val PRE_RELEASE_PATTERN = """$PRE_RELEASE_ID(?:\.$PRE_RELEASE_ID)*"""
        private const val BUILD_PATTERN = """$BUILD_ID(?:\.$BUILD_ID)*"""

        private val PRE_RELEASE = Regex(PRE_RELEASE_PATTERN)
        private val BUILD = Regex(BUILD_PATTERN)
        private val PATTERN = Regex(
            """^($NUMERIC)\.($NUMERIC)\.($NUMERIC)(?:-($PRE_RELEASE_PATTERN))?(?:\+($BUILD_PATTERN))?$"""
        )

        fun parse(input: String): SemVer {
            val match = requireNotNull(PATTERN.matchEntire(input.trim().removePrefix("v"))) {
                "Invalid semantic version: '$input'"
            }
            val groups = match.groupValues

            fun String.toVersionPart(): Int =
                requireNotNull(toIntOrNull()) { "Version component '$this' out of range in '$input'" }

            return SemVer(
                major = groups[1].toVersionPart(),
                minor = groups[2].toVersionPart(),
                patch = groups[3].toVersionPart(),
                preRelease = groups[4].ifEmpty { null },
                build = groups[5].ifEmpty { null }
            )
        }

        private fun comparePreRelease(left: String?, right: String?): Int = when {
            left == right -> 0
            left == null -> 1
            right == null -> -1
            else -> {
                val leftIds = left.split('.')
                val rightIds = right.split('.')
                leftIds.zip(rightIds)
                    .firstNotNullOfOrNull { (l, r) -> compareIdentifier(l, r).takeIf { it != 0 } }
                    ?: leftIds.size.compareTo(rightIds.size)
            }
        }

        // Numeric ids have no leading zeros, so length-then-lexical equals numeric order without overflow.
        private fun compareIdentifier(left: String, right: String): Int {
            val leftNumeric = left.all { it in '0'..'9' }
            val rightNumeric = right.all { it in '0'..'9' }
            return when {
                leftNumeric && rightNumeric -> compareValuesBy(left, right, String::length, { it })
                leftNumeric -> -1
                rightNumeric -> 1
                else -> left.compareTo(right)
            }
        }
    }
}
