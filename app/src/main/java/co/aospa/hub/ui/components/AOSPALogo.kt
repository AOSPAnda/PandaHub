package co.aospa.hub.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AOSPALogo(
    modifier: Modifier = Modifier,
    size: Dp = 307.dp,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    rotation: Float = 0f,
) {
    Canvas(
        modifier = modifier.size(size)
    ) {
        val width = this.size.width
        val height = this.size.height
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = width * 0.45f

        // Hexagon
        rotate(
            degrees = rotation,
            pivot = Offset(centerX, centerY)
        ) {
            drawPath(
                path = createHexagonPath(centerX, centerY, radius),
                color = accentColor,
            )
        }

        // Circle
        drawCircle(
            color = Color.White,
            radius = width * 0.1918f,
            center = Offset(centerX, centerY)
        )

        // Crescent
        drawPath(
            path = createCrescentPath(width, height, centerX, centerY),
            color = Color.Black
        )
    }
}

private fun createHexagonPath(
    centerX: Float,
    centerY: Float,
    radius: Float
): Path {
    return Path().apply {
        for (i in 0..5) {
            val angle = PI.toFloat() / 3f * i - PI.toFloat() / 1
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

private fun createCrescentPath(
    width: Float,
    height: Float,
    centerX: Float,
    centerY: Float
): Path {
    return Path().apply {
        moveTo(0.6268f * width, 0.4690f * height)
        cubicTo(
            0.6332f * width, 0.4958f * height,
            0.6307f * width, 0.5240f * height,
            0.6195f * width, 0.5493f * height
        )
        cubicTo(
            0.6084f * width, 0.5747f * height,
            0.5895f * width, 0.5958f * height,
            0.5655f * width, 0.6095f * height
        )
        cubicTo(
            0.5416f * width, 0.6232f * height,
            0.5140f * width, 0.6289f * height,
            0.4865f * width, 0.6256f * height
        )
        cubicTo(
            0.4590f * width, 0.6223f * height,
            0.4335f * width, 0.6104f * height,
            0.4134f * width, 0.5914f * height
        )
        cubicTo(
            0.3934f * width, 0.5724f * height,
            0.3802f * width, 0.5474f * height,
            0.3753f * width, 0.5202f * height
        )
        cubicTo(
            0.3704f * width, 0.4929f * height,
            0.3744f * width, 0.4650f * height,
            0.3872f * width, 0.4403f * height
        )
        cubicTo(
            0.3999f * width, 0.4155f * height,
            0.4196f * width, 0.3955f * height,
            0.4443f * width, 0.3831f * height
        )
        cubicTo(
            0.4690f * width, 0.3708f * height,
            0.4970f * width, 0.3667f * height,
            0.5242f * width, 0.3716f * height
        )
        lineTo(centerX, centerY)
        lineTo(0.6268f * width, 0.4690f * height)
        close()
    }
}