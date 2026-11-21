package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.UserProfileEntity
import com.example.data.UserRole
import com.example.ui.components.MeetFlowCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.Navy900
import com.example.ui.theme.RoyalBlue100

private enum class AuthTab { LOGIN, SIGNUP, FORGOT }

@Composable
fun AuthScreen(
    profiles: List<UserProfileEntity>,
    authError: String?,
    onClearError: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String, UserRole) -> Unit,
    onResetPassword: (String, String) -> Unit,
    onQuickDemoUser: (String) -> Unit
) {
    var activeTab by remember { mutableStateOf(AuthTab.LOGIN) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("alex@meetflow.ai") }
    var password by remember { mutableStateOf("demo1234") }
    var selectedRole by remember { mutableStateOf(UserRole.ADMIN) }
    var resetMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(MaterialTheme.shapes.large)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_banner),
                    contentDescription = "MeetFlow AI Hero",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x66091120), Color(0xCC091120))
                            )
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        StatusPill(
                            text = "AGENTIC MEETING INTELLIGENCE",
                            containerColor = RoyalBlue100.copy(alpha = 0.9f),
                            contentColor = Navy900
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "MeetFlow AI",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "From Meeting Conversations to Intelligent Execution",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            MeetFlowCard(modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = activeTab == AuthTab.LOGIN,
                        onClick = { activeTab = AuthTab.LOGIN; onClearError(); resetMessage = null },
                        shape = SegmentedButtonDefaults.itemShape(0, 3)
                    ) { Text("Sign In") }
                    SegmentedButton(
                        selected = activeTab == AuthTab.SIGNUP,
                        onClick = { activeTab = AuthTab.SIGNUP; onClearError(); resetMessage = null },
                        shape = SegmentedButtonDefaults.itemShape(1, 3)
                    ) { Text("Sign Up") }
                    SegmentedButton(
                        selected = activeTab == AuthTab.FORGOT,
                        onClick = { activeTab = AuthTab.FORGOT; onClearError(); resetMessage = null },
                        shape = SegmentedButtonDefaults.itemShape(2, 3)
                    ) { Text("Reset") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (authError != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = authError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                if (resetMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = resetMessage!!,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (activeTab == AuthTab.SIGNUP) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("signup_name_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Work Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("auth_email_input")
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (activeTab == AuthTab.FORGOT) "New Password" else "Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
                )

                if (activeTab == AuthTab.SIGNUP) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Workspace Role", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UserRole.entries.forEach { role ->
                            FilterChip(
                                selected = selectedRole == role,
                                onClick = { selectedRole = role },
                                label = { Text(role.displayName) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        when (activeTab) {
                            AuthTab.LOGIN -> onSignIn(email, password)
                            AuthTab.SIGNUP -> onSignUp(name, email, password, selectedRole)
                            AuthTab.FORGOT -> {
                                onResetPassword(email, password)
                                resetMessage = "Local password reset requested for $email."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("auth_submit_button")
                ) {
                    Text(
                        when (activeTab) {
                            AuthTab.LOGIN -> "Sign In to Workspace"
                            AuthTab.SIGNUP -> "Create Local Account"
                            AuthTab.FORGOT -> "Update Password"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Offline Local Workspace Auth is active (Firebase Cloud Auth was declined).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            MeetFlowCard(modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Quick Demo Access",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Instant Hackathon Demo Profiles",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap any persona to test role-based approvals, conflict resolution, and personalized digests.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                profiles.take(3).forEach { profile ->
                    val roleLabel = UserRole.entries.firstOrNull { it.name == profile.role }?.displayName ?: profile.role
                    OutlinedButton(
                        onClick = { onQuickDemoUser(profile.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("demo_login_${profile.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${profile.name} (${profile.email})", maxLines = 1)
                            StatusPill(
                                text = roleLabel,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
