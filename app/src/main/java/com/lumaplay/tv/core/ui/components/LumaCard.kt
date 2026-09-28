package com.lumaplay.tv.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.LocalContentColor
import com.lumaplay.tv.core.theme.LumaTheme

/**
 * LumaPlay TV Media Card Foundation.
 *
 * Implements Google Stitch TV design specifications for interactive media tiles:
 * - 12dp rounded corners ([LumaTheme.shapes.large], Stitch rounded-xl) by default.
 * - Level 2 surface resting container ([LumaTheme.colors.surfaceCard]).
 * - Focused 1.04x scale enlargement for 10-foot television clarity.
 * - Solid 3dp Electric Cyan ([LumaTheme.colors.cyan]) focused border.
 * - Luminous 20dp Electric Cyan glow halo ([LumaTheme.colors.cyanGlow]) on focus.
 * - Smooth elevation and color transition to Level 3 surface on focus.
 *
 * **Disabled Behavior & Focus Exclusion**:
 * In `androidx.tv.material3.Card`, the native API does not expose an `enabled` parameter
 * and requires an `onClick` lambda, which would retain D-pad focus and accessibility actions.
 * When [enabled] is `false`, [LumaCard] renders a completely non-interactive container ([Box])
 * that:
 * 1. Has no focusable or clickable modifiers, entirely bypassing TV D-pad focus traversal.
 * 2. Emits no interactive semantics, preventing TalkBack from announcing phantom actions.
 * 3. Applies muted styling (dimmed background, outline variant border, muted text color).
 * 4. Exposes the exact same [ColumnScope] content slot for seamless caller parity.
 *
 * **Important Caller Constraint**:
 * Callers must not place independently focusable children (such as buttons or interactive controls)
 * inside a disabled card, as the non-interactive container is designed to be completely bypassed
 * during D-pad navigation.
 *
 * @param onClick Invoked when the card is clicked via remote D-pad center / Enter button.
 * @param modifier Composable modifier for sizing, layout, or placement.
 * @param onLongClick Optional callback for long-press actions (context menu, favorites).
 * @param enabled Whether the card is interactive and focusable via D-pad navigation.
 * @param shape Corner geometry, defaults to [LumaTheme.shapes.large] (16dp).
 * @param interactionSource Optional [MutableInteractionSource] to monitor focus/press state.
 * @param content Vertical slot content placed inside the card's [ColumnScope].
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LumaCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = LumaTheme.shapes.large,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (enabled) {
        val fallbackInteractionSource = remember {
            MutableInteractionSource()
        }

        Card(
            onClick = onClick,
            modifier = modifier,
            onLongClick = onLongClick,
            shape = CardDefaults.shape(shape = shape),
            colors = CardDefaults.colors(
                containerColor = LumaTheme.colors.surfaceCard,
                contentColor = LumaTheme.colors.textPrimary,
                focusedContainerColor = LumaTheme.colors.surfaceContainerHigh,
                focusedContentColor = LumaTheme.colors.textPrimary,
                pressedContainerColor = LumaTheme.colors.surfaceContainerHigh,
                pressedContentColor = LumaTheme.colors.textPrimary
            ),
            scale = CardDefaults.scale(
                scale = 1.0f,
                focusedScale = 1.04f,
                pressedScale = 1.02f
            ),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(width = 3.dp, color = LumaTheme.colors.cyan),
                    shape = shape
                )
            ),
            glow = CardDefaults.glow(
                focusedGlow = Glow(
                    elevationColor = LumaTheme.colors.cyanGlow,
                    elevation = 20.dp
                )
            ),
            interactionSource = interactionSource ?: fallbackInteractionSource,
            content = content
        )
    } else {
        Box(
            modifier = modifier
                .alpha(0.5f)
                .clip(shape)
                .background(LumaTheme.colors.surfaceCard)
                .border(
                    width = 1.dp,
                    color = LumaTheme.colors.outlineVariant.copy(alpha = 0.4f),
                    shape = shape
                )
        ) {
            CompositionLocalProvider(
                LocalContentColor provides LumaTheme.colors.textMuted
            ) {
                Column(content = content)
            }
        }
    }
}
