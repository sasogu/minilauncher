package es.sasogu.minilauncher

import android.app.Notification
import android.os.Build
import android.os.Environment
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PaymentNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in PAYMENT_PACKAGES) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE).orEmpty()
        val text = extras.getString(Notification.EXTRA_TEXT).orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val fullText = "$title $text $bigText"

        val amount = parseAmount(fullText) ?: return
        val appLabel = PAYMENT_PACKAGES[sbn.packageName] ?: sbn.packageName
        val merchant = parseMerchant(sbn.packageName, fullText)
        val concepte = (if (text.isNotBlank()) text else bigText).take(120)

        writePaymentCsv(amount, merchant, appLabel, concepte)
    }

    private fun writePaymentCsv(amount: Double, merchant: String, appLabel: String, concepte: String) {
        val dir = financesDir()
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "Cannot create finances dir: $dir")
            return
        }

        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val file = File(dir, "moviments_$today.csv")
        val isNew = !file.exists()

        try {
            file.appendText(
                buildString {
                    if (isNew) appendLine("data,comerç,import,targeta_app,concepte,categoria")
                    appendLine("$today,${csv(merchant)},${String.format("%.2f", amount)},$appLabel,${csv(concepte)},")
                },
                Charsets.UTF_8,
            )
            Log.i(TAG, "Payment: ${amount}€ at $merchant via $appLabel → $file")
        } catch (e: Exception) {
            Log.e(TAG, "CSV write failed", e)
        }
    }

    private fun financesDir(): File {
        // Android 11+: needs MANAGE_EXTERNAL_STORAGE for Documents access.
        // Without it we fall back to the app-specific dir; user can copy files manually.
        val documentsDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "minilauncher-finances",
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Log.w(TAG, "MANAGE_EXTERNAL_STORAGE not granted; writing to app-specific dir instead")
            return File(getExternalFilesDir(null), "finances")
        }
        return documentsDir
    }

    companion object {
        private const val TAG = "PaymentNotifService"

        private fun csv(value: String) = "\"${value.replace("\"", "\"\"")}\""

        // Map of payment/banking app package names to friendly labels
        val PAYMENT_PACKAGES: Map<String, String> = mapOf(
            "com.bbva.bbvacontigo" to "BBVA",
            "es.lacaixa.mobile.android.newwapicon" to "CaixaBank",
            "es.caixabank.caixabanknow" to "CaixaBank",
            "com.santander.app" to "Santander",
            "es.openbank.mobile" to "Openbank",
            "com.ing.banking" to "ING",
            "es.evobanco.bancamovil" to "EVO Banco",
            "es.pibank.customers" to "Pibank",
            "es.cajamar.app" to "Cajamar",
            "es.unicaja.banco.app" to "Unicaja",
            "es.ibercaja.app" to "Ibercaja",
            "es.kutxabank.app" to "Kutxabank",
            "app.fiare" to "Fiare",
            "com.fiare.android" to "Fiare",
            "com.bizum.app" to "Bizum",
            "com.paypal.android.p2pmobile" to "PayPal",
            "com.google.android.apps.walletnfcrel" to "Google Pay",
            "com.revolut.revolut" to "Revolut",
            "com.transferwise.android" to "Wise",
            "com.n26.android" to "N26",
        )

        // Matches amounts like: 12,50€  -1.234,00 €  +29.99€
        private val AMOUNT_REGEX = Regex(
            """([+-]?\s*\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{1,2})?)\s*€""",
        )

        fun parseAmount(text: String): Double? {
            val raw = AMOUNT_REGEX.find(text)?.groupValues?.get(1)?.replace(" ", "") ?: return null
            val normalized = when {
                raw.contains(',') && raw.contains('.') ->
                    // European format "1.234,56" vs US format "1,234.56"
                    if (raw.lastIndexOf(',') > raw.lastIndexOf('.'))
                        raw.replace(".", "").replace(",", ".")
                    else
                        raw.replace(",", "")
                raw.contains(',') -> raw.replace(",", ".")
                else -> raw
            }
            return normalized.toDoubleOrNull()
        }

        fun parseMerchant(packageName: String, text: String): String {
            // Common Spanish notification patterns: "pagado en X", "pago en X", "recibido de X"
            val patterns = listOf(
                Regex("""(?:en|at)\s+([A-ZÁÉÍÓÚÜÑ][^.,\n]{2,35})""", RegexOption.IGNORE_CASE),
                Regex("""(?:de|from)\s+([A-ZÁÉÍÓÚÜÑ][^.,\n€]{2,35})\s*[€\d]""", RegexOption.IGNORE_CASE),
                Regex("""(?:a|to)\s+([A-ZÁÉÍÓÚÜÑ][^.,\n€]{2,35})\s*[€\d]""", RegexOption.IGNORE_CASE),
            )
            for (pattern in patterns) {
                val candidate = pattern.find(text)?.groupValues?.get(1)?.trim() ?: continue
                if (candidate.length >= 2) return candidate
            }
            return PAYMENT_PACKAGES[packageName] ?: packageName
        }
    }
}
