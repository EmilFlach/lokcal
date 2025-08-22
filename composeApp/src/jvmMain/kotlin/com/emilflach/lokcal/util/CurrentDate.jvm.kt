package com.emilflach.lokcal.util

import java.time.LocalDate

actual fun currentDateIso(): String = LocalDate.now().toString()
