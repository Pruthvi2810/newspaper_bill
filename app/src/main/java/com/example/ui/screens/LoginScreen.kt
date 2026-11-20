package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.InkNavy
import com.example.ui.theme.InkText
import com.example.ui.theme.PaperOutline
import com.example.ui.theme.Terracotta

@Composable
fun LoginScreen(
    errorMessage: String?,
    onLogin: (String, String) -> Unit,
    onClearError: () -> Unit
) {
    var username by rememberSaveable { mutableStateOf("admin") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var rememberMe by rememberSaveable { mutableStateOf(true) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
    ) {
        // Curved Navy Blue + Orange Accent Top Header
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .align(Alignment.TopCenter)
        ) {
            val orangePath = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height * 0.78f)
                quadraticTo(size.width * 0.5f, size.height * 0.12f, 0f, size.height * 0.65f)
                close()
            }
            drawPath(orangePath, color = Terracotta)

            val navyPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height * 0.66f)
                quadraticTo(size.width * 0.5f, size.height * 0.04f, 0f, size.height * 0.58f)
                close()
            }
            drawPath(navyPath, color = InkNavy)
        }

        // Curved Navy Footer Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.BottomCenter)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val footerPath = Path().apply {
                    moveTo(0f, size.height * 0.35f)
                    quadraticTo(size.width * 0.5f, -size.height * 0.15f, size.width, size.height * 0.35f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(footerPath, color = InkNavy)
            }
            Text(
                text = "Trusted • Reliable • On Time",
                color = Color.White,
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pruthvi Logo Icon
            Image(
                painter = painterResource(id = R.drawable.ic_pruthvi_globe_news_1795101867268),
                contentDescription = "Pruthvi News Agency Logo",
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Pruthvi",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = InkNavy,
                lineHeight = 36.sp
            )
            Text(
                text = "News Agency",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = InkNavy
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = InkNavy.copy(alpha = 0.4f))
                Text(
                    text = "  News at Your Doorstep...  ",
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    color = InkNavy,
                    fontWeight = FontWeight.Medium
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = InkNavy.copy(alpha = 0.4f))
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Login Card with overlapping circular user badge
            Box(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 26.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, Color(0xFFE4E9F2))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 36.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Login",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF151922)
                        )
                        Text(
                            text = "Access your account",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280)
                        )

                        val loginFieldColors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = InkText,
                            unfocusedTextColor = InkText,
                            cursorColor = InkNavy,
                            focusedBorderColor = InkNavy,
                            unfocusedBorderColor = PaperOutline,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedPlaceholderColor = Color(0xFF6B7280),
                            unfocusedPlaceholderColor = Color(0xFF6B7280)
                        )

                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                onClearError()
                            },
                            placeholder = { Text("Username") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = InkNavy)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = loginFieldColors,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_username_input")
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                onClearError()
                            },
                            placeholder = { Text("Password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = InkNavy)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = Color(0xFF6B7280)
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = loginFieldColors,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { onLogin(username, password) }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = rememberMe,
                                    onCheckedChange = { rememberMe = it },
                                    colors = CheckboxDefaults.colors(checkedColor = InkNavy)
                                )
                                Text(
                                    text = "Remember Me",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF151922)
                                )
                            }
                            TextButton(onClick = { showForgotDialog = true }) {
                                Text(
                                    text = "Forgot Password?",
                                    color = Color(0xFF1D4ED8),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }

                        Button(
                            onClick = { onLogin(username, password) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_submit_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = InkNavy)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Login",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
                            Text(
                                text = "  OR  ",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF6B7280)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
                        }

                        OutlinedButton(
                            onClick = { showAboutDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFD0D7E5))
                        ) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                tint = InkNavy
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "About Our Agency",
                                color = InkNavy,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Overlapping circular user icon
                Surface(
                    shape = CircleShape,
                    color = InkNavy,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(54.dp)
                        .offset(y = 0.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Pruthvi News Agency", fontWeight = FontWeight.Bold, color = InkNavy) },
            text = {
                Text(
                    "Pro.: Rakeshsinh K. Rajawat\n" +
                        "A-4, Devalay Apartment, Keshavnagar,\n" +
                        "Subhashbridge, Ahmedabad-27\n" +
                        "Mobile: 9924545306\n\n" +
                        "News at Your Doorstep — Trusted • Reliable • On Time"
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Close") }
            }
        )
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = { Text("Local Login Credentials", fontWeight = FontWeight.Bold) },
            text = { Text("Default local username: admin\nDefault local password: admin123") },
            confirmButton = {
                TextButton(onClick = { showForgotDialog = false }) { Text("OK") }
            }
        )
    }
}
