package org.example.domain.usecase

import org.example.data.persistence.HistoryStore

/**
 * Use case that returns accumulated total_tokens for a given session.
 *
 * If there is no usage data or all total_tokens are null, returns null.
 */
class GetSessionUsageUseCase(
    private val historyStore: HistoryStore
) {
    suspend operator fun invoke(sessionId: String): Int? {
        val records = historyStore.getUsageForSession(sessionId)
        if (records.isEmpty()) return null

        val totals = records.mapNotNull { it.usage.total_tokens }
        if (totals.isEmpty()) return null

        return totals.sum()
    }
}
