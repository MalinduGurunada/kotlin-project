package com.example.personalfinanceapp.feature.dashboard

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.personalfinanceapp.feature.history.ui.UnifiedTransactionItem
import java.io.File

object TransactionCsvExporter {

    fun exportAndShareCsv(context: Context, transactions: List<UnifiedTransactionItem>) {
        val csvHeader = "Date,Title,Category/Source,Amount,Type,Channel\n"
        val csvRows = transactions.joinToString("\n") { item ->
            "\"${item.occurredDate}\",\"${item.title}\",\"${item.categoryOrSource}\",\"${item.amountDecimalText}\",\"${if (item.isExpense) "Expense" else "Income"}\",\"${item.paymentChannelOrKind}\""
        }
        val csvContent = csvHeader + csvRows

        val exportDir = File(context.cacheDir, "csv_exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val csvFile = File(exportDir, "finclarity_transactions_report.csv")
        csvFile.writeText(csvContent)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            csvFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "FinClarity Transaction Export Report")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Export Transactions CSV Report"))
    }
}
