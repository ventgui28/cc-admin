package com.ventgui.app.data.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.ventgui.app.data.model.Athlete
import com.ventgui.app.data.model.Race
import com.ventgui.app.data.model.RaceResult
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import android.content.ContentValues

object PdfReportGenerator {
    fun generateAndShareReport(
        context: Context,
        startDate: String,
        endDate: String,
        location: String,
        races: List<Race>,
        results: List<Pair<RaceResult, Athlete>>,
        totalKm: Double,
        avgPosition: Double,
        totalPodiums: Int,
        showOnlyTop5: Boolean = false
    ) {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create() // A4: 595 x 842 pt
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint()
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        // --- Helper: Draw Header on Page 1 ---
        fun drawPage1Header() {
            paint.color = Color.rgb(0, 18, 32) // Midnight Blue
            canvas.drawRect(0f, 0f, 595f, 95f, paint)


            textPaint.color = Color.WHITE
            textPaint.textSize = 18f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("CANTANHEDE CYCLING", 24f, 40f, textPaint)

            textPaint.textSize = 10f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Relatório de Conquistas e Resultados Oficiais", 24f, 58f, textPaint)

            val subtitle = buildString {
                append("Período: $startDate a $endDate")
                if (location.isNotBlank()) {
                    append("  |  Filtro Local: $location")
                }
                if (showOnlyTop5) {
                    append("  |  Apenas Top 5")
                }
            }
            canvas.drawText(subtitle, 24f, 74f, textPaint)
        }


        // --- Helper: Draw Header on subsequent pages ---
        fun drawSubsequentPageHeader() {
            paint.color = Color.rgb(0, 18, 32) // Midnight Blue
            canvas.drawRect(0f, 0f, 595f, 50f, paint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 12f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("CANTANHEDE CYCLING  -  Resultados (continuação)", 24f, 30f, textPaint)
        }

        // Draw initial header
        drawPage1Header()

        // --- Draw Summary Stats Card on Page 1 ---
        var y = 120f
        paint.color = Color.rgb(240, 248, 255) // Cyber light cyan
        canvas.drawRect(24f, y, 571f, y + 56f, paint)

        textPaint.color = Color.rgb(0, 18, 32)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 9f
        val label = if (showOnlyTop5) "TOTAL TOP 5 (1º-5º)" else "TOTAL PÓDIOS (1º-3º)"
        canvas.drawText(label, 40f, y + 20f, textPaint)

        textPaint.textSize = 16f
        canvas.drawText("$totalPodiums", 40f, y + 42f, textPaint)


        y += 85f

        // Group results by race
        val resultsByRace = results.groupBy { it.first.race_id }

        // --- Helper: Check page bounds and break page if necessary ---
        fun checkAndPageBreak(requiredSpace: Float, drawSubsequentHeader: Boolean = true) {
            if (y + requiredSpace > 790f) {
                // Draw footer on current page
                textPaint.color = Color.GRAY
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("Cantanhede Cycling  |  Gerado em ${LocalDate.now()}  |  Página $pageNumber", 24f, 810f, textPaint)


                pdfDocument.finishPage(page)

                // Create new page
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                if (drawSubsequentHeader) {
                    drawSubsequentPageHeader()
                    y = 75f
                } else {
                    y = 40f
                }
            }
        }

        // --- Render Grouped Race Sections ---
        for ((raceId, raceResults) in resultsByRace) {
            val race = races.firstOrNull { it.id == raceId } ?: continue

            // Calculate space needed for this race section: title + table headers + separator (approx 55f) + rows (18f per row)
            val sectionHeaderSpace = 55f
            val resultsSpace = raceResults.size * 18f
            val totalNeededSpace = sectionHeaderSpace + resultsSpace

            if (totalNeededSpace <= 700f) {
                // The section can fit completely on a single page. Prevent it from being split:
                if (y + totalNeededSpace > 790f) {
                    checkAndPageBreak(850f) // Force page break
                }
            } else {
                // The section is too big to fit on one page anyway (e.g. >35 results).
                // Ensure there is at least space on this page for the header + first 2 rows.
                if (y + sectionHeaderSpace + 36f > 790f) {
                    checkAndPageBreak(850f)
                }
            }


            // Draw Race Section Title
            val dateStr = if (race.date.contains("T")) race.date.substringBefore("T") else race.date
            val raceLocation = if (!race.location.isNullOrBlank()) " (${race.location})" else ""
            val fullTitle = "$dateStr - ${race.title}$raceLocation"

            textPaint.color = Color.rgb(0, 18, 32)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 11f
            canvas.drawText(fullTitle, 24f, y, textPaint)

            // Draw Section Table Headers
            y += 18f
            textPaint.color = Color.GRAY
            textPaint.textSize = 9f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Atleta", 36f, y, textPaint)
            canvas.drawText("Posição", 380f, y, textPaint)
            canvas.drawText("Tempo", 480f, y, textPaint)

            // Separator line
            paint.color = Color.rgb(200, 200, 200)
            paint.strokeWidth = 0.5f
            canvas.drawLine(24f, y + 4f, 571f, y + 4f, paint)

            y += 18f

            // Draw results for this race
            for (item in raceResults) {
                val res = item.first
                val athlete = item.second

                checkAndPageBreak(25f)

                textPaint.color = Color.BLACK
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textPaint.textSize = 10f

                // Athlete Name
                val athleteName = if (athlete.name.length > 40) athlete.name.take(37) + "..." else athlete.name
                canvas.drawText(athleteName, 36f, y, textPaint)

                // Position
                val isHighlight = if (showOnlyTop5) res.position in 1..5 else res.position in 1..3
                if (isHighlight) {
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textPaint.color = Color.rgb(220, 100, 0) // Orange/Bronze for podium/top-5
                }
                val posText = when {
                    res.position != null && res.position > 0 -> "${res.position}º"
                    else -> "--"
                }
                canvas.drawText(posText, 380f, y, textPaint)

                // Time
                textPaint.color = Color.BLACK
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(res.time ?: "--:--", 480f, y, textPaint)

                y += 18f
            }

            // Extra space between race sections
            y += 15f
        }

        // --- Render Team Classifications Section (if any exist) ---
        val teamRaces = races.filter { it.team_classification != null }
        if (teamRaces.isNotEmpty()) {
            checkAndPageBreak(80f)

            // Draw Section Header
            textPaint.color = Color.rgb(0, 18, 32)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 11f
            canvas.drawText("CLASSIFICAÇÃO COLETIVA (EQUIPA)", 24f, y, textPaint)

            y += 18f
            textPaint.color = Color.GRAY
            textPaint.textSize = 9f
            canvas.drawText("Prova", 36f, y, textPaint)
            canvas.drawText("Posição Coletiva", 380f, y, textPaint)

            paint.color = Color.rgb(180, 180, 180)
            paint.strokeWidth = 0.5f
            canvas.drawLine(24f, y + 4f, 571f, y + 4f, paint)

            y += 18f

            for (race in teamRaces) {
                checkAndPageBreak(25f)

                textPaint.color = Color.BLACK
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textPaint.textSize = 10f

                val raceTitle = if (race.title.length > 50) race.title.take(47) + "..." else race.title
                canvas.drawText(raceTitle, 36f, y, textPaint)

                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("${race.team_classification}º", 380f, y, textPaint)

                y += 18f
            }
        }

        // Draw final page footer
        textPaint.color = Color.GRAY
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Cantanhede Cycling  |  Gerado em ${LocalDate.now()}  |  Página $pageNumber", 24f, 810f, textPaint)


        pdfDocument.finishPage(page)

        // --- Save and Share PDF File ---
        val fileName = "Relatorio_Cantanhede_${System.currentTimeMillis()}.pdf"
        var pdfUri: Uri? = null
        var isPublic = false

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    pdfUri = uri
                    isPublic = true
                }
            } else {
                val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!publicDir.exists()) {
                    publicDir.mkdirs()
                }
                val file = File(publicDir, fileName)
                try {
                    FileOutputStream(file).use { fos ->
                        pdfDocument.writeTo(fos)
                    }
                    pdfUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    isPublic = true
                } catch (e: Exception) {
                    val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    val fallbackFile = File(fallbackDir, fileName)
                    FileOutputStream(fallbackFile).use { fos ->
                        pdfDocument.writeTo(fos)
                    }
                    pdfUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        fallbackFile
                    )
                    isPublic = false
                }
            }

            pdfDocument.close()

            if (pdfUri != null) {
                val message = if (isPublic) {
                    "PDF guardado na pasta Downloads!"
                } else {
                    "PDF guardado em documentos da aplicação."
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Partilhar Relatório"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao guardar PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
