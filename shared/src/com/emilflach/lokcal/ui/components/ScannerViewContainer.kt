package com.emilflach.lokcal.ui.components

import androidx.compose.runtime.Composable
import com.emilflach.lokcal.theme.LocalRecipesColors
import org.ncgroup.kscan.BarcodeFormat
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
            BarcodeFormat.FORMAT_EAN_8,
            BarcodeFormat.FORMAT_UPC_A,
            BarcodeFormat.FORMAT_EAN_13,
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
                onScan(result.barcode.data.trim())
                onClose()
            }
            else -> {
                onClose()
            }
        }
    }
}
