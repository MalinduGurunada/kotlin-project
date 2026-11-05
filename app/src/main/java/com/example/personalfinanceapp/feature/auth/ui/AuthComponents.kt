package com.example.personalfinanceapp.feature.auth.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.R

internal object AuthColors {
    val EmeraldTop = Color(0xFF073E32)
    val EmeraldBottom = Color(0xFF012A23)
    val EmeraldCard = Color(0xFF0B4B3D)
    val EmeraldDeep = Color(0xFF052E27)
    val Lime = Color(0xFFB7F34A)
    val LimeSoft = Color(0xFFD9FF92)
    val White = Color(0xFFFFFFFF)
    val TextMuted = Color(0xFFB9CEC7)
    val InputText = Color(0xFF14332B)
    val InputHint = Color(0xFF73877F)
    val Error = Color(0xFFFFA8A8)
    val Success = Color(0xFFAEEA62)
}

internal object AuthDimens {
    val ScreenPadding = 24.dp
    val FieldRadius = 18.dp
    val ButtonRadius = 18.dp
    val FieldHeight = 62.dp
    val ButtonHeight = 58.dp
}

@Composable
internal fun AuthBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AuthColors.EmeraldTop, Color(0xFF064236), AuthColors.EmeraldBottom)
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = AuthColors.Lime.copy(alpha = 0.045f),
                radius = size.minDimension * 0.52f,
                center = Offset(size.width * 1.03f, size.height * 0.10f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.025f),
                radius = size.minDimension * 0.46f,
                center = Offset(-size.width * 0.16f, size.height * 0.76f)
            )
        }
        content()
    }
}

@Composable
internal fun AuthBrand(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(35.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(AuthColors.Lime),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(20.dp)) {
                val stroke = 2.4.dp.toPx()
                drawLine(AuthColors.EmeraldDeep, Offset(size.width * .18f, size.height * .72f), Offset(size.width * .45f, size.height * .45f), stroke, StrokeCap.Round)
                drawLine(AuthColors.EmeraldDeep, Offset(size.width * .45f, size.height * .45f), Offset(size.width * .62f, size.height * .59f), stroke, StrokeCap.Round)
                drawLine(AuthColors.EmeraldDeep, Offset(size.width * .62f, size.height * .59f), Offset(size.width * .84f, size.height * .28f), stroke, StrokeCap.Round)
            }
        }
        Text(
            text = "FinClarity",
            color = AuthColors.White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.35).sp
        )
    }
}

@Composable
internal fun AuthTopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.09f), CircleShape)
                .semantics { contentDescription = "Back" }
        ) {
            Canvas(Modifier.size(20.dp)) {
                val stroke = 2.dp.toPx()
                drawLine(Color.White, Offset(size.width * .72f, size.height * .16f), Offset(size.width * .28f, size.height * .5f), stroke, StrokeCap.Round)
                drawLine(Color.White, Offset(size.width * .28f, size.height * .5f), Offset(size.width * .72f, size.height * .84f), stroke, StrokeCap.Round)
            }
        }
        AuthBrand(Modifier.align(Alignment.Center))
    }
}

internal enum class AuthFieldIcon { Person, Email, Lock }

@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    leadingIcon: AuthFieldIcon,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = AuthColors.White.copy(alpha = .92f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        TextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            placeholder = { Text(placeholder, color = AuthColors.InputHint, fontSize = 15.sp) },
            singleLine = true,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            leadingIcon = { AuthInputIcon(leadingIcon) },
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(AuthDimens.FieldRadius),
            colors = TextFieldDefaults.colors(
                focusedTextColor = AuthColors.InputText,
                unfocusedTextColor = AuthColors.InputText,
                focusedContainerColor = AuthColors.White,
                unfocusedContainerColor = AuthColors.White.copy(alpha = .97f),
                disabledContainerColor = AuthColors.White.copy(alpha = .75f),
                cursorColor = AuthColors.EmeraldCard,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AuthDimens.FieldHeight)
                .semantics { contentDescription = label }
        )
    }
}

@Composable
private fun AuthInputIcon(icon: AuthFieldIcon) {
    Canvas(Modifier.size(21.dp).clearAndSetSemantics { }) {
        val color = AuthColors.EmeraldCard
        val stroke = 1.7.dp.toPx()
        when (icon) {
            AuthFieldIcon.Person -> {
                drawCircle(color, size.width * .18f, Offset(size.width * .5f, size.height * .3f), style = Stroke(stroke))
                val body = Path().apply {
                    moveTo(size.width * .2f, size.height * .84f)
                    cubicTo(size.width * .22f, size.height * .57f, size.width * .78f, size.height * .57f, size.width * .8f, size.height * .84f)
                }
                drawPath(body, color, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            AuthFieldIcon.Email -> {
                drawRoundRect(color, Offset(size.width * .1f, size.height * .2f), Size(size.width * .8f, size.height * .62f), CornerRadius(3.dp.toPx()), style = Stroke(stroke))
                drawLine(color, Offset(size.width * .14f, size.height * .27f), Offset(size.width * .5f, size.height * .55f), stroke)
                drawLine(color, Offset(size.width * .86f, size.height * .27f), Offset(size.width * .5f, size.height * .55f), stroke)
            }
            AuthFieldIcon.Lock -> {
                drawRoundRect(color, Offset(size.width * .16f, size.height * .42f), Size(size.width * .68f, size.height * .46f), CornerRadius(4.dp.toPx()), style = Stroke(stroke))
                val shackle = Path().apply {
                    moveTo(size.width * .31f, size.height * .43f)
                    cubicTo(size.width * .3f, size.height * .09f, size.width * .7f, size.height * .09f, size.width * .69f, size.height * .43f)
                }
                drawPath(shackle, color, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
internal fun PasswordVisibilityButton(visible: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = if (visible) "Hide password" else "Show password" }
    ) {
        Canvas(Modifier.size(22.dp)) {
            val color = AuthColors.InputHint
            val stroke = 1.7.dp.toPx()
            val eye = Path().apply {
                moveTo(size.width * .06f, size.height * .5f)
                cubicTo(size.width * .24f, size.height * .18f, size.width * .76f, size.height * .18f, size.width * .94f, size.height * .5f)
                cubicTo(size.width * .76f, size.height * .82f, size.width * .24f, size.height * .82f, size.width * .06f, size.height * .5f)
            }
            drawPath(eye, color, style = Stroke(stroke, cap = StrokeCap.Round))
            drawCircle(color, size.width * .12f, center, style = Stroke(stroke))
            if (!visible) drawLine(color, Offset(size.width * .17f, size.height * .15f), Offset(size.width * .83f, size.height * .85f), stroke, StrokeCap.Round)
        }
    }
}

@Composable
internal fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(AuthDimens.ButtonRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthColors.Lime,
            contentColor = AuthColors.EmeraldDeep,
            disabledContainerColor = AuthColors.Lime.copy(alpha = .62f),
            disabledContentColor = AuthColors.EmeraldDeep.copy(alpha = .68f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AuthDimens.ButtonHeight)
            .semantics { contentDescription = text }
            .shadow(12.dp, RoundedCornerShape(AuthDimens.ButtonRadius), ambientColor = AuthColors.Lime.copy(alpha = .2f), spotColor = AuthColors.Lime.copy(alpha = .2f))
    ) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.size(23.dp), color = AuthColors.EmeraldDeep, strokeWidth = 2.5.dp)
        } else {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun AuthGoogleButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .size(52.dp)
            .shadow(8.dp, CircleShape, ambientColor = Color.Black.copy(alpha = .2f))
            .clip(CircleShape)
            .background(AuthColors.White)
            .semantics { contentDescription = "Continue with Google" }
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                Modifier.size(22.dp),
                color = AuthColors.EmeraldDeep,
                strokeWidth = 2.25.dp
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ic_google_g),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
internal fun AuthInlineLink(prompt: String, link: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(prompt, color = AuthColors.TextMuted, fontSize = 14.sp)
        Text(link, color = AuthColors.Lime, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun AuthMessage(message: String, isError: Boolean, modifier: Modifier = Modifier) {
    val color = if (isError) AuthColors.Error else AuthColors.Success
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = .1f))
            .border(1.dp, color.copy(alpha = .3f), RoundedCornerShape(12.dp))
            .semantics { liveRegion = LiveRegionMode.Assertive }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Text(message, color = color, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
internal fun PasswordRequirementChip(text: String, satisfied: Boolean, modifier: Modifier = Modifier) {
    val color by animateColorAsState(if (satisfied) AuthColors.Lime else AuthColors.TextMuted, label = "password requirement")
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = if (satisfied) .1f else .06f))
            .border(1.dp, color.copy(alpha = .28f), RoundedCornerShape(50))
            .semantics {
                stateDescription = if (satisfied) "Satisfied" else "Not satisfied"
            }
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Canvas(Modifier.size(12.dp)) {
            if (satisfied) {
                drawLine(color, Offset(size.width * .08f, size.height * .52f), Offset(size.width * .4f, size.height * .82f), 1.7.dp.toPx(), StrokeCap.Round)
                drawLine(color, Offset(size.width * .4f, size.height * .82f), Offset(size.width * .92f, size.height * .15f), 1.7.dp.toPx(), StrokeCap.Round)
            } else drawCircle(color, size.minDimension * .22f, center)
        }
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun PaymentCardArtwork(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(190.dp).clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(230.dp).background(
                Brush.radialGradient(listOf(AuthColors.Lime.copy(alpha = .1f), Color.Transparent)),
                CircleShape
            )
        )
        PaymentCard(
            dark = false,
            modifier = Modifier.size(238.dp, 143.dp).offset(x = 30.dp, y = (-2).dp).graphicsLayer { rotationZ = 8f }
        )
        PaymentCard(
            dark = true,
            modifier = Modifier.size(238.dp, 143.dp).offset(x = (-24).dp, y = 13.dp).graphicsLayer { rotationZ = -7f }
        )
    }
}

@Composable
private fun PaymentCard(dark: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    val background = if (dark) Brush.linearGradient(listOf(Color(0xFF0D5B49), Color(0xFF06382F))) else Brush.linearGradient(listOf(AuthColors.LimeSoft, Color(0xFF9FE234)))
    val foreground = if (dark) Color.White else AuthColors.EmeraldDeep
    Box(
        modifier
            .shadow(16.dp, shape, ambientColor = Color.Black.copy(alpha = .3f))
            .clip(shape)
            .background(background)
            .border(1.dp, Color.White.copy(alpha = .14f), shape)
            .padding(18.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(31.dp, 23.dp).clip(RoundedCornerShape(6.dp)).background(if (dark) AuthColors.Lime else Color.White.copy(alpha = .72f)))
                Text("FIN", color = foreground, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("••••  ••••  ••••  2486", color = foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = .9.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("YOUR MONEY", color = foreground.copy(alpha = .78f), fontSize = 9.sp)
                Text("12/29", color = foreground.copy(alpha = .78f), fontSize = 9.sp)
            }
        }
    }
}

@Composable
internal fun SupportCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = .075f))
            .border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(Modifier.size(42.dp).background(AuthColors.Lime.copy(alpha = .14f), CircleShape), contentAlignment = Alignment.Center) {
            Text("?", color = AuthColors.Lime, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Need more help?", color = AuthColors.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Our support team is here to help you.", color = AuthColors.TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}
