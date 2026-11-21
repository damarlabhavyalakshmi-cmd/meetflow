package com.example.data

class DemoDataSeeder(private val dao: MeetFlowDao) {

    suspend fun seedInitialWorkspaceAndUser() {
        val adminProfile = UserProfileEntity(
            id = "usr_admin_1",
            name = "Alex Rivera",
            email = "alex@meetflow.ai",
            passwordHash = "demo1234".hashCode().toString(),
            role = UserRole.ADMIN.name,
            avatarInitials = "AR"
        )
        val rahulProfile = UserProfileEntity(
            id = "usr_rahul_2",
            name = "Rahul Verma",
            email = "rahul@meetflow.ai",
            passwordHash = "demo1234".hashCode().toString(),
            role = UserRole.TEAM_MEMBER.name,
            avatarInitials = "RV"
        )
        val priyaProfile = UserProfileEntity(
            id = "usr_priya_3",
            name = "Priya Nair",
            email = "priya@meetflow.ai",
            passwordHash = "demo1234".hashCode().toString(),
            role = UserRole.MANAGER.name,
            avatarInitials = "PN"
        )
        dao.upsertProfile(adminProfile)
        dao.upsertProfile(rahulProfile)
        dao.upsertProfile(priyaProfile)

        val coreWorkspace = WorkspaceEntity(
            id = "ws_core_product",
            name = "Core Product Launch",
            description = "Website Launch & Q4 Platform Delivery (Hackathon Demo Workspace)",
            ownerId = adminProfile.id
        )
        val growthWorkspace = WorkspaceEntity(
            id = "ws_eng_ops",
            name = "Platform Engineering",
            description = "Infrastructure, Security & API Reliability",
            ownerId = adminProfile.id
        )
        dao.upsertWorkspace(coreWorkspace)
        dao.upsertWorkspace(growthWorkspace)

        dao.upsertWorkspaceMembers(
            listOf(
                WorkspaceMemberEntity("wm_1", coreWorkspace.id, adminProfile.id, adminProfile.name, adminProfile.email, UserRole.ADMIN.name),
                WorkspaceMemberEntity("wm_2", coreWorkspace.id, rahulProfile.id, rahulProfile.name, rahulProfile.email, UserRole.TEAM_MEMBER.name),
                WorkspaceMemberEntity("wm_3", coreWorkspace.id, priyaProfile.id, priyaProfile.name, priyaProfile.email, UserRole.MANAGER.name),
                WorkspaceMemberEntity("wm_4", growthWorkspace.id, adminProfile.id, adminProfile.name, adminProfile.email, UserRole.ADMIN.name)
            )
        )

        seedIntegrations(coreWorkspace.id)
        seedIntegrations(growthWorkspace.id)
        resetDemoData(coreWorkspace.id)
    }

    suspend fun seedIntegrations(workspaceId: String) {
        val providers = listOf(
            IntegrationEntity("int_${workspaceId}_gcal", workspaceId, "Google Calendar", "Scheduling", "Sync approved meeting deadlines and release milestones to team calendars.", "DISCONNECTED", "", "Requires Google OAuth 2.0 Calendar scope"),
            IntegrationEntity("int_${workspaceId}_gdrive", workspaceId, "Google Drive", "Storage", "Import meeting recordings and archive exported executive summaries.", "DISCONNECTED", "", "Requires Google Drive OAuth 2.0 scope"),
            IntegrationEntity("int_${workspaceId}_trello", workspaceId, "Trello", "Task Tracker", "Push approved action items as cards onto project boards.", "DISCONNECTED", "", "Requires Trello API Key & Token"),
            IntegrationEntity("int_${workspaceId}_asana", workspaceId, "Asana", "Task Tracker", "Sync action items, owners, and due dates to Asana projects.", "DISCONNECTED", "", "Requires Asana Personal Access Token / OAuth"),
            IntegrationEntity("int_${workspaceId}_clickup", workspaceId, "ClickUp", "Task Tracker", "Mirror task statuses and priorities with ClickUp lists.", "DISCONNECTED", "", "Requires ClickUp OAuth / API Token"),
            IntegrationEntity("int_${workspaceId}_slack", workspaceId, "Slack", "Messaging", "Post conflict alerts and approval requests to Slack channels.", "DISCONNECTED", "", "Requires Slack Bot OAuth Token / Webhook"),
            IntegrationEntity("int_${workspaceId}_email", workspaceId, "Email", "Digest Delivery", "Send personalized Weekly Digests and overdue task reminders via SMTP/API.", "DISCONNECTED", "", "Requires verified Email provider / SMTP")
        )
        dao.insertIntegrations(providers)
    }

    suspend fun resetDemoData(workspaceId: String = "ws_core_product") {
        dao.clearMeetings(workspaceId)
        dao.clearTranscripts(workspaceId)
        dao.clearDecisions(workspaceId)
        dao.clearDecisionHistory(workspaceId)
        dao.clearMemories(workspaceId)
        dao.clearConflicts(workspaceId)
        dao.clearTasks(workspaceId)
        dao.clearApprovals(workspaceId)
        dao.clearReminders(workspaceId)
        dao.clearDigests(workspaceId)
        dao.clearAiLogs(workspaceId)

        val now = System.currentTimeMillis()

        // 3 Interactive Hackathon Demo Meetings
        val m1 = MeetingEntity(
            id = "demo_mtg_1",
            workspaceId = workspaceId,
            title = "Meeting 1 – Project Planning",
            meetingDate = "2026-11-10",
            project = "Website Launch",
            description = "[DEMO] Kickoff alignment for marketing site & frontend build milestones.",
            participantsCsv = "Alex Rivera, Rahul Verma, Priya Nair",
            sourceType = "TRANSCRIPT",
            sourceFileName = "demo_planning_kickoff.vtt",
            processingStatus = "COMPLETED",
            summaryTopics = "Website Launch Date, Frontend Architecture, QA Gate",
            summaryDiscussions = "[Demo Sample] Team aligned on shipping the new website by December 10, prioritizing responsive frontend completion by December 5.",
            summaryConclusions = "[Demo Sample] Approved December 10 launch target; assigned frontend implementation to Rahul Verma.",
            unresolvedIssuesSerialized = "Confirm CDN caching policy before final staging deploy.",
            importantQuotesSerialized = "Alex Rivera|00:01:12|Let's lock December 10 as our official website launch date.\nRahul Verma|00:02:05|I will complete the frontend build by December 5 so QA has buffer.",
            isDemo = true,
            createdAt = now - 300_000L
        )

        val m2 = MeetingEntity(
            id = "demo_mtg_2",
            workspaceId = workspaceId,
            title = "Meeting 2 – Development Review",
            meetingDate = "2026-11-16",
            project = "Website Launch",
            description = "[DEMO] Mid-sprint engineering sync reviewing API readiness and revised launch proposal.",
            participantsCsv = "Alex Rivera, Rahul Verma, Priya Nair",
            sourceType = "AUDIO",
            sourceFileName = "demo_dev_review.mp3",
            processingStatus = "COMPLETED",
            summaryTopics = "API Integration Schedule, Revised Launch Proposal, Load Testing",
            summaryDiscussions = "[Demo Sample] Priya reported API integration requires until December 15, prompting a new proposal to move website launch from December 10 to December 20.",
            summaryConclusions = "[Demo Sample] Conflict detected against Meeting 1 canonical launch date (Dec 10 vs Dec 20); awaiting human decision.",
            unresolvedIssuesSerialized = "Whether marketing campaign assets can wait until December 20.|Load test benchmark targets for checkout endpoint.",
            importantQuotesSerialized = "Priya Nair|00:01:40|I'll finish API integration by December 15, so I propose moving the website launch to December 20.\nAlex Rivera|00:02:18|Let's flag that conflict against our December 10 decision for review.",
            isDemo = true,
            createdAt = now - 200_000L
        )

        val m3 = MeetingEntity(
            id = "demo_mtg_3",
            workspaceId = workspaceId,
            title = "Meeting 3 – Final Review",
            meetingDate = "2026-11-20",
            project = "Website Launch",
            description = "[DEMO] Pre-release readiness review and testing sign-off checkpoint.",
            participantsCsv = "Alex Rivera, Rahul Verma, Priya Nair",
            sourceType = "TEXT",
            sourceFileName = "demo_final_review.txt",
            processingStatus = "COMPLETED",
            summaryTopics = "Testing Readiness, Launch Sign-off Policy, Stakeholder Comms",
            summaryDiscussions = "[Demo Sample] Reviewed staging test plan and agreed to confirm the revised launch date only after end-to-end testing completes.",
            summaryConclusions = "[Demo Sample] Final launch date confirmation gated on QA regression pass and Approval Center sign-off.",
            unresolvedIssuesSerialized = "Pending final QA regression suite sign-off.",
            importantQuotesSerialized = "Alex Rivera|00:00:54|We will confirm the revised launch date immediately after testing signs off.",
            isDemo = true,
            createdAt = now - 100_000L
        )

        dao.upsertMeeting(m1)
        dao.upsertMeeting(m2)
        dao.upsertMeeting(m3)

        dao.insertTranscriptSegments(
            listOf(
                TranscriptSegmentEntity("seg_d1_1", m1.id, workspaceId, "Alex Rivera", "00:00:18", "Welcome everyone to Project Planning. Let's lock our target launch date for the Website Launch.", 0),
                TranscriptSegmentEntity("seg_d1_2", m1.id, workspaceId, "Alex Rivera", "00:01:12", "Let's lock December 10 as our official website launch date.", 1),
                TranscriptSegmentEntity("seg_d1_3", m1.id, workspaceId, "Rahul Verma", "00:02:05", "I will complete the frontend build by December 5 so QA has enough buffer.", 2),
                TranscriptSegmentEntity("seg_d2_1", m2.id, workspaceId, "Rahul Verma", "00:00:42", "Frontend components are 85% complete and tracking on schedule for December 5.", 0),
                TranscriptSegmentEntity("seg_d2_2", m2.id, workspaceId, "Priya Nair", "00:01:40", "I will complete API integration by December 15, so I propose moving the website launch to December 20.", 1),
                TranscriptSegmentEntity("seg_d2_3", m2.id, workspaceId, "Alex Rivera", "00:02:18", "Let's flag that conflict against our December 10 decision and review it before updating stakeholders.", 2),
                TranscriptSegmentEntity("seg_d3_1", m3.id, workspaceId, "Priya Nair", "00:00:30", "API staging endpoints are ready for integration testing.", 0),
                TranscriptSegmentEntity("seg_d3_2", m3.id, workspaceId, "Alex Rivera", "00:00:54", "Decision: Confirm revised launch date after testing passes all end-to-end checks.", 1)
            )
        )

        // Decisions
        val d1 = DecisionEntity(
            id = "demo_dec_1",
            workspaceId = workspaceId,
            meetingId = m1.id,
            meetingTitle = m1.title,
            project = "Website Launch",
            topicKey = "website_launch_date",
            title = "Website launch December 10",
            description = "[DEMO] Official initial launch target locked during Project Planning.",
            proposer = "Alex Rivera",
            approver = "Alex Rivera",
            decisionDate = "2026-11-10",
            status = DecisionStatus.APPROVED.name,
            isDemo = true,
            createdAt = now - 300_000L
        )
        val d2 = DecisionEntity(
            id = "demo_dec_2",
            workspaceId = workspaceId,
            meetingId = m2.id,
            meetingTitle = m2.title,
            project = "Website Launch",
            topicKey = "website_launch_date",
            title = "Proposed launch December 20",
            description = "[DEMO] Proposal raised during Development Review to accommodate API integration completion on Dec 15.",
            proposer = "Priya Nair",
            approver = "Pending Approval",
            decisionDate = "2026-11-16",
            status = DecisionStatus.PROPOSED.name,
            isDemo = true,
            createdAt = now - 200_000L
        )
        val d3 = DecisionEntity(
            id = "demo_dec_3",
            workspaceId = workspaceId,
            meetingId = m3.id,
            meetingTitle = m3.title,
            project = "Website Launch",
            topicKey = "launch_qa_gate",
            title = "Confirm revised launch date after testing",
            description = "[DEMO] Launch sign-off requires verified end-to-end QA pass.",
            proposer = "Alex Rivera",
            approver = "Alex Rivera",
            decisionDate = "2026-11-20",
            status = DecisionStatus.APPROVED.name,
            isDemo = true,
            createdAt = now - 100_000L
        )
        dao.insertDecisions(listOf(d1, d2, d3))

        dao.insertDecisionHistory(
            DecisionHistoryEntity(
                id = "dh_demo_1",
                workspaceId = workspaceId,
                decisionId = d1.id,
                previousValue = "Unscheduled",
                newValue = "Website launch December 10 (Approved)",
                changedBy = "Alex Rivera",
                reason = "Initial Project Planning commitment",
                meetingId = m1.id,
                changedAt = now - 300_000L
            )
        )

        // Meeting Memories
        dao.insertMemories(
            listOf(
                MeetingMemoryEntity("demo_mem_1", workspaceId, m1.id, m1.title, "Website Launch", "DECISION", "Website launch December 10", "Canonical target approved by Alex Rivera with Rahul Verma & Priya Nair.", "Alex Rivera, Rahul Verma, Priya Nair", "launch,deadline,canonical", "2026-11-10", now - 300_000L),
                MeetingMemoryEntity("demo_mem_2", workspaceId, m1.id, m1.title, "Website Launch", "COMMITMENT", "Frontend completion by December 5", "Rahul Verma committed to finishing frontend implementation prior to QA buffer.", "Rahul Verma", "frontend,milestone", "2026-11-10", now - 290_000L),
                MeetingMemoryEntity("demo_mem_3", workspaceId, m2.id, m2.title, "Website Launch", "PROPOSAL", "Proposed launch shift to December 20", "Priya Nair proposed moving launch to Dec 20 due to Dec 15 API integration finish.", "Priya Nair", "proposal,conflict,api", "2026-11-16", now - 200_000L),
                MeetingMemoryEntity("demo_mem_4", workspaceId, m3.id, m3.title, "Website Launch", "DECISION", "QA Testing Gate before launch confirmation", "Team agreed to finalize revised date only after full staging test pass.", "Alex Rivera, Priya Nair", "qa,testing,gate", "2026-11-20", now - 100_000L)
            )
        )

        // Conflict Detection (Exact Meeting 1 vs Meeting 2 demo!)
        dao.insertConflicts(
            listOf(
                ConflictEntity(
                    id = "demo_conf_1",
                    workspaceId = workspaceId,
                    meetingId = m2.id,
                    meetingTitle = m2.title,
                    project = "Website Launch",
                    previousDecisionId = d1.id,
                    previousDecisionText = "December 10 (Approved in Meeting 1 – Project Planning)",
                    newProposalText = "December 20 (Proposed in Meeting 2 – Development Review)",
                    reason = "Different launch dates: Meeting 1 approved December 10, while Meeting 2 proposed December 20.",
                    aiRecommendation = "[Sample AI] Confirm whether the original December 10 deadline has officially changed to December 20, or keep December 10.",
                    status = ConflictStatus.UNRESOLVED.name,
                    createdAt = now - 190_000L
                )
            )
        )

        // Tasks
        dao.insertTasks(
            listOf(
                TaskEntity(
                    id = "demo_tsk_1",
                    workspaceId = workspaceId,
                    meetingId = m1.id,
                    project = "Website Launch",
                    title = "Complete frontend",
                    description = "[DEMO] Finish responsive UI pages and staging build for website launch.",
                    assignee = "Rahul",
                    deadline = "December 5",
                    priority = TaskPriority.HIGH.name,
                    dependencies = "Design system sign-off",
                    status = TaskStatus.IN_PROGRESS.name,
                    syncStatus = "Internal Tracker",
                    isDemo = true,
                    createdAt = now - 280_000L
                ),
                TaskEntity(
                    id = "demo_tsk_2",
                    workspaceId = workspaceId,
                    meetingId = m2.id,
                    project = "Website Launch",
                    title = "Complete API integration",
                    description = "[DEMO] Connect checkout, auth, and analytics endpoints to frontend.",
                    assignee = "Priya",
                    deadline = "December 15",
                    priority = TaskPriority.HIGH.name,
                    dependencies = "Complete frontend",
                    status = TaskStatus.TODO.name,
                    syncStatus = "Internal Tracker",
                    isDemo = true,
                    createdAt = now - 180_000L
                ),
                TaskEntity(
                    id = "demo_tsk_3",
                    workspaceId = workspaceId,
                    meetingId = m3.id,
                    project = "Website Launch",
                    title = "Run final staging QA regression",
                    description = "[DEMO] Execute end-to-end test suite prior to confirming final launch date.",
                    assignee = "Alex Rivera",
                    deadline = "December 17",
                    priority = TaskPriority.MEDIUM.name,
                    dependencies = "Complete API integration",
                    status = TaskStatus.TODO.name,
                    syncStatus = "Internal Tracker",
                    isDemo = true,
                    createdAt = now - 90_000L
                ),
                TaskEntity(
                    id = "demo_tsk_4",
                    workspaceId = workspaceId,
                    meetingId = m1.id,
                    project = "Website Launch",
                    title = "Publish domain & SSL checklist",
                    description = "[DEMO] Verify DNS records and SSL certificates for production.",
                    assignee = "Rahul",
                    deadline = "November 18",
                    priority = TaskPriority.LOW.name,
                    dependencies = "None",
                    status = TaskStatus.COMPLETED.name,
                    syncStatus = "Internal Tracker",
                    isDemo = true,
                    createdAt = now - 310_000L
                ),
                TaskEntity(
                    id = "demo_tsk_5",
                    workspaceId = workspaceId,
                    meetingId = m1.id,
                    project = "Website Launch",
                    title = "Submit accessibility audit report",
                    description = "[DEMO] Overdue compliance checklist for launch readiness.",
                    assignee = "Priya",
                    deadline = "November 19",
                    priority = TaskPriority.HIGH.name,
                    dependencies = "None",
                    status = TaskStatus.OVERDUE.name,
                    syncStatus = "Internal Tracker",
                    isDemo = true,
                    createdAt = now - 305_000L
                )
            )
        )

        // Human Approvals
        dao.insertApprovals(
            listOf(
                ApprovalEntity(
                    id = "demo_apr_1",
                    workspaceId = workspaceId,
                    meetingId = m2.id,
                    meetingTitle = m2.title,
                    proposedAction = "Update Canonical Launch Date to December 20",
                    description = "Supersede Meeting 1 approved date (December 10) with Meeting 2 proposal (December 20).",
                    responsiblePerson = "Priya Nair",
                    destination = "Meeting Memory & Calendar",
                    actionType = "UPDATE_DECISION",
                    payloadSummary = d2.id,
                    status = ApprovalStatus.PENDING.name,
                    createdAt = now - 175_000L
                ),
                ApprovalEntity(
                    id = "demo_apr_2",
                    workspaceId = workspaceId,
                    meetingId = m2.id,
                    meetingTitle = m2.title,
                    proposedAction = "Sync 'Complete API integration' to External Tracker",
                    description = "Create task for Priya (Due December 15, High Priority). Currently held internally until integration is connected and approved.",
                    responsiblePerson = "Priya",
                    destination = "Internal Tracker / ClickUp",
                    actionType = "CREATE_TASK",
                    payloadSummary = "demo_tsk_2",
                    status = ApprovalStatus.PENDING.name,
                    createdAt = now - 170_000L
                ),
                ApprovalEntity(
                    id = "demo_apr_3",
                    workspaceId = workspaceId,
                    meetingId = m1.id,
                    meetingTitle = m1.title,
                    proposedAction = "Approve Frontend Task Assignment",
                    description = "Assign 'Complete frontend' to Rahul with deadline December 5.",
                    responsiblePerson = "Rahul",
                    destination = "Internal Tracker",
                    actionType = "CREATE_TASK",
                    payloadSummary = "demo_tsk_1",
                    status = ApprovalStatus.APPROVED.name,
                    reviewedBy = "Alex Rivera",
                    executionResult = "Executed in Internal Task Tracker at Meeting 1 close.",
                    createdAt = now - 275_000L,
                    reviewedAt = now - 270_000L
                )
            )
        )

        // Reminders
        dao.insertReminders(
            listOf(
                ReminderEntity("demo_rem_1", workspaceId, "demo_conf_1", "Conflict Detected: Website Launch Date", "Meeting 2 proposed Dec 20 vs approved Dec 10.", "Immediate", "CONFLICT", isSent = false, isRead = false, createdAt = now - 185_000L),
                ReminderEntity("demo_rem_2", workspaceId, "demo_tsk_5", "Overdue Task: Accessibility Audit", "Priya's accessibility audit report passed Nov 19 deadline.", "Overdue", "OVERDUE", isSent = false, isRead = false, createdAt = now - 120_000L),
                ReminderEntity("demo_rem_3", workspaceId, "demo_tsk_1", "Upcoming Deadline: Complete frontend", "Rahul's frontend milestone is due December 5.", "Dec 5", "DEADLINE", isSent = false, isRead = false, createdAt = now - 80_000L)
            )
        )

        // Weekly Digest
        dao.upsertDigest(
            WeeklyDigestEntity(
                id = "demo_dig_1",
                workspaceId = workspaceId,
                memberName = "All Team Members",
                weekRange = "Nov 15 – Nov 21, 2026",
                assignedCount = 5,
                completedCount = 1,
                pendingCount = 3,
                overdueCount = 1,
                upcomingDeadlinesSummary = "• Complete frontend (Rahul) — Dec 5\n• Complete API integration (Priya) — Dec 15\n• Run final staging QA regression (Alex) — Dec 17",
                relevantDecisionsSummary = "• Website launch December 10 (Approved)\n• Proposed launch December 20 (Pending Conflict Review)\n• Confirm revised launch date after testing (Approved)",
                meetingUpdatesSummary = "Processed 3 demo meetings across Website Launch project. 1 timeline conflict flagged for human decision.",
                aiProgressSummary = "[Sample AI Digest] Frontend build is in progress with Rahul (due Dec 5). API integration is queued with Priya (due Dec 15), creating a date conflict between Dec 10 and Dec 20 that needs approval.",
                isSampleAi = true,
                createdAt = now - 60_000L
            )
        )

        // AI Activity Logs
        val demoLogs = listOf(
            AiActivityLogEntity("dlog_1", workspaceId, m1.id, "Transcription Agent", "Parse VTT Transcript", AgentStatus.COMPLETED.name, "3 speaker segments parsed", "", now - 300_000L),
            AiActivityLogEntity("dlog_2", workspaceId, m1.id, "Meeting Intelligence Agent", "Extract Decisions & Tasks", AgentStatus.COMPLETED.name, "1 decision, 2 tasks extracted", "", now - 295_000L),
            AiActivityLogEntity("dlog_3", workspaceId, m2.id, "Memory Agent", "Retrieve Launch Decisions", AgentStatus.COMPLETED.name, "Retrieved 'Website launch December 10'", "", now - 195_000L),
            AiActivityLogEntity("dlog_4", workspaceId, m2.id, "Conflict Detection Agent", "Compare Launch Dates", AgentStatus.COMPLETED.name, "CONFLICT: Dec 10 vs Dec 20", "", now - 190_000L),
            AiActivityLogEntity("dlog_5", workspaceId, m2.id, "Task Execution Agent", "Stage Human Approvals", AgentStatus.COMPLETED.name, "Queued 2 items in Approval Center", "", now - 180_000L),
            AiActivityLogEntity("dlog_6", workspaceId, m3.id, "Reminder Agent", "Evaluate Deadlines & Overdue", AgentStatus.COMPLETED.name, "Flagged 1 overdue + 1 conflict alert", "", now - 110_000L),
            AiActivityLogEntity("dlog_7", workspaceId, m3.id, "Weekly Digest Agent", "Compile Weekly Report", AgentStatus.COMPLETED.name, "Generated Nov 15–21 executive digest", "", now - 60_000L)
        )
        demoLogs.forEach { dao.insertAiLog(it) }
    }
}
