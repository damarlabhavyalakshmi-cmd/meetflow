package com.example.ai

import com.example.data.DecisionEntity
import com.example.data.MeetingMemoryEntity
import com.example.data.TaskEntity
import kotlinx.coroutines.delay

data class TranscriptLineDraft(
    val speaker: String,
    val timestamp: String,
    val text: String
)

data class QuoteDraft(
    val speaker: String,
    val timestamp: String,
    val quote: String
)

data class ExtractedDecisionDraft(
    val topicKey: String,
    val title: String,
    val description: String,
    val proposer: String,
    val approver: String,
    val status: String // PROPOSED or APPROVED
)

data class ExtractedTaskDraft(
    val title: String,
    val description: String,
    val assignee: String,
    val deadline: String,
    val priority: String,
    val dependencies: String
)

data class AnalyzedMeetingOutput(
    val segments: List<TranscriptLineDraft>,
    val summaryTopics: String,
    val summaryDiscussions: String,
    val summaryConclusions: String,
    val unresolvedIssues: List<String>,
    val importantQuotes: List<QuoteDraft>,
    val decisions: List<ExtractedDecisionDraft>,
    val tasks: List<ExtractedTaskDraft>,
    val isSampleOutput: Boolean = true
)

data class ConflictEvaluationDraft(
    val previousDecisionId: String,
    val previousDecisionText: String,
    val newProposalText: String,
    val reason: String,
    val aiRecommendation: String
)

interface AiRepository {
    val isLiveGeminiConnected: Boolean
    val aiNoticeMessage: String

    suspend fun transcribeMediaPlaceholder(
        fileName: String,
        sourceType: String,
        participantsHint: String
    ): List<TranscriptLineDraft>

    suspend fun analyzeTranscript(
        title: String,
        project: String,
        participants: List<String>,
        rawTranscript: String
    ): AnalyzedMeetingOutput

    suspend fun detectConflicts(
        newDecisions: List<ExtractedDecisionDraft>,
        historicalDecisions: List<DecisionEntity>,
        memories: List<MeetingMemoryEntity>
    ): List<ConflictEvaluationDraft>

    suspend fun generateWeeklyDigestSummary(
        memberName: String,
        tasks: List<TaskEntity>,
        decisions: List<DecisionEntity>,
        unresolvedConflictCount: Int
    ): String
}

/**
 * Sample / offline-safe AiRepository used when Firebase AI Logic is not enabled.
 * Parses real user-supplied transcript text into speaker segments and produces
 * clearly labeled sample AI extractions without calling external REST endpoints.
 */
class SampleAiRepository : AiRepository {
    override val isLiveGeminiConnected: Boolean = false
    override val aiNoticeMessage: String =
        "Sample AI Mode: Live Gemini inference requires enabling Firebase AI Logic."

    override suspend fun transcribeMediaPlaceholder(
        fileName: String,
        sourceType: String,
        participantsHint: String
    ): List<TranscriptLineDraft> {
        delay(350)
        val speakers = participantsHint.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("Speaker 1", "Speaker 2") }
        val primary = speakers.first()
        val secondary = speakers.getOrElse(1) { primary }

        return listOf(
            TranscriptLineDraft(
                speaker = primary,
                timestamp = "00:00:14",
                text = "[Sample Transcription for $sourceType '$fileName'] Let's review our current delivery milestones, blockers, and ownership updates."
            ),
            TranscriptLineDraft(
                speaker = secondary,
                timestamp = "00:01:08",
                text = "[Sample Transcription] API readiness is tracking well, though QA suggested buffering the release timeline if integration tests slip."
            ),
            TranscriptLineDraft(
                speaker = primary,
                timestamp = "00:02:22",
                text = "[Sample Transcription] Noted. Let's log the proposal for approval and assign follow-up tasks before finalizing."
            )
        )
    }

    override suspend fun analyzeTranscript(
        title: String,
        project: String,
        participants: List<String>,
        rawTranscript: String
    ): AnalyzedMeetingOutput {
        delay(450)
        val parsedSegments = parseTranscriptLines(rawTranscript, participants)
        val firstSpeaker = parsedSegments.firstOrNull()?.speaker
            ?: participants.firstOrNull()
            ?: "Unassigned"

        val lines = parsedSegments.map { it.text }
        val extractedDecisions = mutableListOf<ExtractedDecisionDraft>()
        val extractedTasks = mutableListOf<ExtractedTaskDraft>()
        val unresolved = mutableListOf<String>()

        // Deterministically inspect transcript text for real keywords without fabricating speakers
        parsedSegments.forEach { seg ->
            val lower = seg.text.lowercase()
            if (lower.contains("launch") || lower.contains("release") || lower.contains("decision") || lower.contains("propose") || lower.contains("approve")) {
                val dateMatch = Regex("""(january|february|march|april|may|june|july|august|september|october|november|december)\s+\d{1,2}""", RegexOption.IGNORE_CASE)
                    .find(seg.text)?.value
                val topicKey = if (lower.contains("launch") || lower.contains("release")) "website_launch_date" else "project_scope_${project.lowercase().replace(" ", "_")}"
                val titleText = if (dateMatch != null && (lower.contains("launch") || lower.contains("release"))) {
                    "Website Launch Target: $dateMatch"
                } else if (dateMatch != null) {
                    "Milestone Proposal: $dateMatch"
                } else {
                    "Proposal from ${seg.speaker}"
                }
                extractedDecisions.add(
                    ExtractedDecisionDraft(
                        topicKey = topicKey,
                        title = titleText,
                        description = "[Sample AI Extraction] ${seg.text.take(180)}",
                        proposer = seg.speaker,
                        approver = "Pending Approval",
                        status = "PROPOSED"
                    )
                )
            }
            if (lower.contains("complete") || lower.contains("action") || lower.contains("todo") || lower.contains("task") || lower.contains("will ") || lower.contains("deadline") || lower.contains("finish")) {
                val assignee = participants.firstOrNull { seg.text.contains(it, ignoreCase = true) } ?: seg.speaker
                val deadlineMatch = Regex("""(december|january|november|friday|monday|next week|\d{4}-\d{2}-\d{2})\s*\d{0,2}""", RegexOption.IGNORE_CASE)
                    .find(seg.text)?.value?.trim()?.replaceFirstChar { it.uppercase() } ?: "TBD"
                extractedTasks.add(
                    ExtractedTaskDraft(
                        title = seg.text.take(64).trimEnd('.', ' '),
                        description = "[Sample AI Extraction] Identified from ${seg.speaker} at ${seg.timestamp}: ${seg.text}",
                        assignee = assignee,
                        deadline = deadlineMatch,
                        priority = if (lower.contains("urgent") || lower.contains("critical") || lower.contains("blocker")) "HIGH" else "MEDIUM",
                        dependencies = "None specified"
                    )
                )
            }
            if (seg.text.contains("?") || lower.contains("risk") || lower.contains("blocker") || lower.contains("clarify") || lower.contains("pending")) {
                unresolved.add("${seg.speaker}: ${seg.text.take(140)}")
            }
        }

        if (extractedDecisions.isEmpty()) {
            extractedDecisions.add(
                ExtractedDecisionDraft(
                    topicKey = "${project.lowercase().replace(" ", "_")}_alignment",
                    title = "$title — Scope & Execution Alignment",
                    description = "[Sample AI Extraction] Key discussion points captured for $project review; awaiting formal team confirmation.",
                    proposer = firstSpeaker,
                    approver = "Pending Approval",
                    status = "PROPOSED"
                )
            )
        }

        if (extractedTasks.isEmpty()) {
            extractedTasks.add(
                ExtractedTaskDraft(
                    title = "Follow up on $title outcomes",
                    description = "[Sample AI Extraction] Review meeting summary and confirm next steps for $project.",
                    assignee = firstSpeaker,
                    deadline = "TBD",
                    priority = "MEDIUM",
                    dependencies = "None specified"
                )
            )
        }

        if (unresolved.isEmpty()) {
            unresolved.add("Confirm final sign-off owners for proposed milestones.")
        }

        val quotes = parsedSegments.take(3).map {
            QuoteDraft(speaker = it.speaker, timestamp = it.timestamp, quote = it.text.take(160))
        }

        val discussionSummary = if (lines.isNotEmpty()) {
            "[Sample AI Summary] Discussed ${lines.size} transcript segments covering $project execution, timeline checkpoints, and team ownership."
        } else {
            "[Sample AI Summary] Analyzed $title for $project."
        }

        return AnalyzedMeetingOutput(
            segments = parsedSegments,
            summaryTopics = "$project Roadmap, Milestone Checkpoints, Ownership & Risks",
            summaryDiscussions = discussionSummary,
            summaryConclusions = "[Sample AI Conclusion] Extracted ${extractedDecisions.size} decision proposal(s) and ${extractedTasks.size} action item(s) for human review.",
            unresolvedIssues = unresolved.take(4),
            importantQuotes = quotes,
            decisions = extractedDecisions,
            tasks = extractedTasks,
            isSampleOutput = true
        )
    }

    override suspend fun detectConflicts(
        newDecisions: List<ExtractedDecisionDraft>,
        historicalDecisions: List<DecisionEntity>,
        memories: List<MeetingMemoryEntity>
    ): List<ConflictEvaluationDraft> {
        delay(300)
        val detected = mutableListOf<ConflictEvaluationDraft>()
        val activeHistory = historicalDecisions.filter {
            it.status == "APPROVED" || it.status == "PROPOSED"
        }

        for (candidate in newDecisions) {
            val matching = activeHistory.firstOrNull { prev ->
                prev.topicKey.equals(candidate.topicKey, ignoreCase = true) ||
                    (prev.project.equals("Website Launch", ignoreCase = true) && candidate.title.contains("Launch", ignoreCase = true)) ||
                    (prev.title.contains("Launch", ignoreCase = true) && candidate.title.contains("Launch", ignoreCase = true))
            }
            if (matching != null && !matching.title.equals(candidate.title, ignoreCase = true)) {
                detected.add(
                    ConflictEvaluationDraft(
                        previousDecisionId = matching.id,
                        previousDecisionText = "${matching.title} (${matching.status} in ${matching.meetingTitle})",
                        newProposalText = "${candidate.title} (Proposed by ${candidate.proposer})",
                        reason = "Contradicts prior commitment '${matching.title}' stored in Meeting Memory.",
                        aiRecommendation = "[Sample AI] Confirm whether the original target has officially shifted, or keep '${matching.title}' as canonical."
                    )
                )
            }
        }
        return detected
    }

    override suspend fun generateWeeklyDigestSummary(
        memberName: String,
        tasks: List<TaskEntity>,
        decisions: List<DecisionEntity>,
        unresolvedConflictCount: Int
    ): String {
        delay(300)
        val completed = tasks.count { it.status == "COMPLETED" }
        val pending = tasks.count { it.status == "TODO" || it.status == "IN_PROGRESS" }
        val overdue = tasks.count { it.status == "OVERDUE" }
        return "[Sample AI Digest for $memberName] Completed $completed task(s), with $pending active and $overdue overdue. " +
            "Tracked ${decisions.size} workspace decision(s) and $unresolvedConflictCount unresolved conflict(s) requiring review."
    }

    private fun parseTranscriptLines(
        rawTranscript: String,
        participants: List<String>
    ): List<TranscriptLineDraft> {
        val cleanedLines = rawTranscript.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("WEBVTT", ignoreCase = true) && !it.all { ch -> ch.isDigit() } }

        val result = mutableListOf<TranscriptLineDraft>()
        var currentTime = "00:00:00"
        var index = 0

        for (line in cleanedLines) {
            if (line.contains("-->")) {
                currentTime = line.substringBefore("-->").trim().take(8)
                continue
            }
            val colonIdx = line.indexOf(':')
            if (colonIdx in 2..28 && !line.startsWith("http", ignoreCase = true)) {
                val speaker = line.substring(0, colonIdx).trim()
                val text = line.substring(colonIdx + 1).trim()
                if (text.isNotBlank()) {
                    val ts = if (currentTime != "00:00:00") currentTime else formatSec(index * 35)
                    result.add(TranscriptLineDraft(speaker = speaker, timestamp = ts, text = text))
                    index++
                    continue
                }
            }
            val fallbackSpeaker = participants.getOrNull(index % participants.size.coerceAtLeast(1)) ?: "Speaker"
            result.add(
                TranscriptLineDraft(
                    speaker = fallbackSpeaker,
                    timestamp = formatSec(index * 35),
                    text = line
                )
            )
            index++
        }
        return result
    }

    private fun formatSec(totalSec: Int): String {
        val mins = totalSec / 60
        val secs = totalSec % 60
        return "00:%02d:%02d".format(mins, secs)
    }
}
