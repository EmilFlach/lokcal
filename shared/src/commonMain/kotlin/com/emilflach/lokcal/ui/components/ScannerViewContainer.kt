package com.emilflach.lokcal.ui.components

import androidx.compose.runtime.Composable
import com.emilflach.lokcal.theme.LocalRecipesColors
import org.ncgroup.kscan.BarcodeFormats
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerUiOptions
import org.ncgroup.kscan.ScannerView
import org.ncgroup.kscan.scannerColors

@Composable
fun ScannerViewContainer(
    onScan: (String) -> Unit,
    onClose: () -> Unit
) {
    val color = LocalRecipesColors.current
    ScannerView(
        codeTypes = listOf(
            BarcodeFormats.FORMAT_EAN_13,
        ),
        scannerUiOptions = ScannerUiOptions(
            headerTitle = "Scan barcode",
            showZoom = false,
        ),
        colors = scannerColors(
            headerContainerColor = color.backgroundPage,
            barcodeFrameColor = color.foregroundBrand,
        ),
    ) { result ->
        when (result) {
            is BarcodeResult.OnSuccess -> {
                val raw = result.barcode.data
                val digits = raw.filter { it.isDigit() }
                if (digits.length == 13) {
                    onScan(digits)
                } else {
                    onScan(raw)
                }
                onClose()
            }
            else -> {
                onClose()
            }
        }
    }
}
