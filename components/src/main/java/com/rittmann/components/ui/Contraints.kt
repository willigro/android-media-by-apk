package com.rittmann.components.ui

import androidx.constraintlayout.compose.ConstrainScope
import androidx.constraintlayout.compose.ConstrainedLayoutReference

fun ConstrainScope.linkToSides(where: ConstrainedLayoutReference = parent) {
    start.linkTo(where.start)
    end.linkTo(where.end)
}