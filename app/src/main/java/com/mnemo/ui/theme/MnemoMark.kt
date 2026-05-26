package com.mnemo.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val M_PATH =
    "M8.174 9.125c.186-1.116 1.395-2.387 3.039-2.387c1.55 0 2.76 1.116 3.101 2.232" +
    "l3.659 12.278h.062L21.692 8.97c.341-1.116 1.55-2.232 3.101-2.232c1.642 0 2.852 1.271" +
    " 3.039 2.387l2.883 17.302c.031.186.031.372.031.526c0 1.365-.992 2.232-2.232 2.232" +
    "c-1.582 0-2.201-.713-2.418-2.17l-1.83-12.619h-.062l-3.721 12.991c-.217.744-.805 1.798" +
    "-2.48 1.798c-1.674 0-2.263-1.054-2.48-1.798l-3.721-12.991h-.062l-1.83 12.62" +
    "c-.217 1.457-.837 2.17-2.418 2.17c-1.24 0-2.232-.867-2.232-2.232c0-.154 0-.341.031-.526" +
    "L8.174 9.125z"

private val mPath by lazy { PathParser().parsePathString(M_PATH).toPath() }

@Composable
fun MnemoMark(
    color: Color = TextPrimary,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.requiredSize(size)) {
        val s = this.size.width / 36f
        scale(scaleX = s, scaleY = s, pivot = Offset.Zero) {
            drawPath(mPath, color = color)
        }
    }
}
