package com.sabin.expensetracker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Local copies of the two Material icons we use, so we don't need the material-icons artifact. */
object AppIcons {
    val Add: ImageVector by lazy { icon("Add", "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z") }

    val Clear: ImageVector by lazy {
        icon(
            "Clear",
            "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 " +
                "19,17.59 13.41,12z"
        )
    }

    val ChevronLeft: ImageVector by lazy { icon("ChevronLeft", "M15.41,7.41L14,6l-6,6 6,6 1.41,-1.41L10.83,12z") }

    val ChevronRight: ImageVector by lazy { icon("ChevronRight", "M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z") }

    private fun icon(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(PathParser().parsePathString(pathData).toNodes(), fill = SolidColor(Color.Black))
            .build()
}
