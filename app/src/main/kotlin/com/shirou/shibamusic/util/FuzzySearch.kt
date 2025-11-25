package com.shirou.shibamusic.util

import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object FuzzySearch {

    /**
     * Calculates the similarity between two words using a modified Levenshtein distance.
     * Returns a value between 0.0 (no match) and 1.0 (perfect match).
     */
    private fun calculateWordSimilarity(query: String, target: String): Double {
        if (query.isBlank() || target.isBlank()) return 0.0
        
        val q = query
        val t = target

        if (q == t) return 1.0
        
        // If query is a substring of target (e.g. "floyd" in "floydian"), give high score
        // but penalize slightly for length difference to prefer exact matches
        if (t.contains(q)) {
            return 0.9 + (0.1 * (q.length.toDouble() / t.length))
        }

        val distance = levenshteinDistance(q, t)
        val maxLength = max(q.length, t.length)
        
        if (maxLength == 0) return 1.0
        
        return max(0.0, 1.0 - (distance.toDouble() / maxLength))
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val len0 = lhs.length + 1
        val len1 = rhs.length + 1
        var cost = IntArray(len0)
        var newCost = IntArray(len0)

        for (i in 0 until len0) cost[i] = i

        for (j in 1 until len1) {
            newCost[0] = j
            for (i in 1 until len0) {
                val match = if (lhs[i - 1] == rhs[j - 1]) 0 else 1
                val costReplace = cost[i - 1] + match
                val costInsert = cost[i] + 1
                val costDelete = newCost[i - 1] + 1
                newCost[i] = min(min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[len0 - 1]
    }

    /**
     * Matches a single query token against a full text field.
     * It splits the text field into words to allow fuzzy matching against individual words.
     */
    private fun matchTokenToText(token: String, text: String): Double {
        val t = text.lowercase(Locale.getDefault()).trim()
        val q = token
        
        // 1. Exact phrase/substring match on the full text (fastest and strongest)
        if (t.contains(q)) return 1.0

        // 2. Fuzzy match against individual words in the text
        // This handles cases like "drak" matching "Dark" inside "The Dark Side"
        val words = t.split(Regex("[\\s\\p{Punct}]+")) // Split by whitespace and punctuation
        var maxWordScore = 0.0
        
        for (word in words) {
            val score = calculateWordSimilarity(q, word)
            if (score > maxWordScore) {
                maxWordScore = score
                if (maxWordScore == 1.0) break 
            }
        }
        
        return maxWordScore
    }

    /**
     * Filters and sorts a list of items based on a query string.
     * Supports multi-word queries matching across multiple fields.
     *
     * @param items List of items to search
     * @param query Search query
     * @param textSelector Function to extract the text fields to match from an item
     * @param threshold Minimum average similarity score (0.0 to 1.0) to include in results
     */
    fun <T> search(
        items: List<T>,
        query: String,
        textSelector: (T) -> List<String?>,
        threshold: Double = 0.4
    ): List<T> {
        if (query.isBlank()) return emptyList()

        // Split query into tokens (e.g. "pink floyd" -> ["pink", "floyd"])
        val queryTokens = query.lowercase(Locale.getDefault())
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (queryTokens.isEmpty()) return emptyList()

        return items
            .asSequence()
            .map { item ->
                val fields = textSelector(item).filterNotNull()
                
                // Calculate score: Average of the best match for each token
                var totalScore = 0.0
                
                for (token in queryTokens) {
                    var bestTokenScore = 0.0
                    // Find the best match for this token across ALL fields of the item
                    for (field in fields) {
                        val score = matchTokenToText(token, field)
                        if (score > bestTokenScore) {
                            bestTokenScore = score
                            if (bestTokenScore == 1.0) break
                        }
                    }
                    totalScore += bestTokenScore
                }
                
                val avgScore = totalScore / queryTokens.size
                item to avgScore
            }
            .filter { it.second >= threshold }
            .sortedByDescending { it.second }
            .map { it.first }
            .toList()
    }
}