package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.SampleAiRepository
import com.example.data.DecisionEntity
import com.example.data.DecisionStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MeetFlow AI", appName)
    }

    @Test
    fun `sample ai parses transcript and detects launch date conflict`() = runTest {
        val ai = SampleAiRepository()
        val output = ai.analyzeTranscript(
            title = "Dev Sync",
            project = "Website Launch",
            participants = listOf("Priya Nair", "Rahul Verma"),
            rawTranscript = """
                Priya Nair: I propose moving website launch to December 20.
                Rahul Verma: I will complete frontend fixes by December 5.
            """.trimIndent()
        )
        assertEquals(2, output.segments.size)
        assertTrue(output.decisions.isNotEmpty())
        assertTrue(output.tasks.isNotEmpty())

        val historical = listOf(
            DecisionEntity(
                id = "dec_1",
                workspaceId = "ws_core_product",
                meetingId = "mtg_1",
                meetingTitle = "Meeting 1 – Project Planning",
                project = "Website Launch",
                topicKey = "website_launch_date",
                title = "Website launch December 10",
                description = "Approved Dec 10 launch",
                proposer = "Alex Rivera",
                approver = "Alex Rivera",
                decisionDate = "2026-11-10",
                status = DecisionStatus.APPROVED.name
            )
        )
        val conflicts = ai.detectConflicts(output.decisions, historical, emptyList())
        assertEquals(1, conflicts.size)
        assertTrue(conflicts.first().reason.contains("Contradicts"))
    }
}
