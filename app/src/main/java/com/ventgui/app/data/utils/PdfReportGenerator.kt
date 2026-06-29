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
import android.graphics.RectF
import com.ventgui.app.ui.screens.team.checkEmdStatus
import java.time.format.DateTimeFormatter

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

    fun generateAndShareEmergencyPdf(context: Context, athlete: Athlete) {
        val pdfDocument = PdfDocument()
        // Página A4 padrão (595 x 842 pontos a 72 DPI)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        // Pincéis de Desenho
        val bgPaint = Paint().apply {
            color = 0xFFF5F7FA.toInt() // Cinza claro de fundo médico
            style = Paint.Style.FILL
        }
        val headerPaint = Paint().apply {
            color = 0xFF001E35.toInt() // Midnight Blue
            style = Paint.Style.FILL
        }
        val accentPaint = Paint().apply {
            color = 0xFFFF5252.toInt() // Vermelho Coral de Emergência Médica
            style = Paint.Style.FILL
        }
        val whitePaint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.FILL
        }
        val textPaint = Paint().apply {
            color = 0xFF1A202C.toInt() // Cinza escuro para texto principal
            textSize = 12f
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = 0xFFE2E8F0.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // Desenhar fundo da folha
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        // 1. Cabeçalho Principal (Midnight Blue)
        canvas.drawRect(0f, 0f, 595f, 130f, headerPaint)

        // Faixa de destaque de emergência (Vermelho Coral à esquerda)
        canvas.drawRect(0f, 0f, 15f, 130f, accentPaint)

        // Títulos do Cabeçalho
        textPaint.apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 20f
            isFakeBoldText = true
        }
        canvas.drawText("FICHA DE EMERGÊNCIA MÉDICA", 40f, 55f, textPaint)

        textPaint.apply {
            color = 0xFF00E5FF.toInt() // Cyber Cyan
            textSize = 12f
            isFakeBoldText = true
        }
        canvas.drawText("CANTANHEDE CYCLING HUB", 40f, 80f, textPaint)

        textPaint.apply {
            color = 0xFFA0AEC0.toInt()
            textSize = 10f
            isFakeBoldText = false
        }
        canvas.drawText("Escola de Ciclismo / Clube Técnico UVP-FPC", 40f, 98f, textPaint)

        // 2. Foto do Atleta ou Placeholder de Ciclista
        // Caixa da Foto
        val photoRect = RectF(445f, 150f, 555f, 260f)
        canvas.drawRoundRect(photoRect, 16f, 16f, whitePaint)
        canvas.drawRoundRect(photoRect, 16f, 16f, borderPaint)

        // Desenhar ícone de utilizador genérico no interior
        val iconPaint = Paint().apply {
            color = 0xFFCBD5E0.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        // Cabeça
        canvas.drawCircle(500f, 190f, 18f, iconPaint)
        // Corpo (arco de elipse)
        val bodyRect = RectF(475f, 215f, 525f, 260f)
        canvas.drawArc(bodyRect, 180f, 180f, true, iconPaint)

        // Texto do Placeholder
        textPaint.apply {
            color = 0xFF718096.toInt()
            textSize = 8f
            isFakeBoldText = true
        }
        canvas.drawText("FOTO PERFIL", 472f, 250f, textPaint)

        // 3. Dados Principais do Atleta (Esquerda)
        var y = 180f
        textPaint.apply {
            color = 0xFF1A202C.toInt()
            textSize = 18f
            isFakeBoldText = true
        }
        canvas.drawText(athlete.name.uppercase(), 40f, y, textPaint)

        y += 25f
        textPaint.apply {
            color = 0xFF718096.toInt()
            textSize = 12f
            isFakeBoldText = false
        }
        val escalao = athlete.category.uppercase()
        val licenca = if (athlete.license_number.isNullOrBlank()) "NÃO FEDERADO" else "L.: ${athlete.license_number}"
        canvas.drawText("$escalao  •  $licenca", 40f, y, textPaint)

        y += 20f
        val birthDate = athlete.birth_date ?: "-"
        val tlf = athlete.phone ?: "Não Registado"
        canvas.drawText("Data Nasc.: $birthDate  •  Tlm Atleta: $tlf", 40f, y, textPaint)

        // 4. SECÇÃO MÉDICA E REGULAMENTAR (Sinalização a Vermelho)
        y = 290f
        // Separador Visual
        accentPaint.color = 0xFFFF5252.toInt()
        canvas.drawRoundRect(RectF(40f, y, 70f, y + 4f), 2f, 2f, accentPaint)
        
        textPaint.apply {
            color = 0xFF001E35.toInt()
            textSize = 14f
            isFakeBoldText = true
        }
        canvas.drawText("INFORMAÇÕES MÉDICAS CRÍTICAS", 80f, y + 8f, textPaint)

        y += 25f
        // Caixa Médica
        val medCardRect = RectF(40f, y, 555f, y + 120f)
        canvas.drawRoundRect(medCardRect, 16f, 16f, whitePaint)
        canvas.drawRoundRect(medCardRect, 16f, 16f, borderPaint)

        // Linha interna de alerta vertical (Vermelho à esquerda)
        val alertLinePaint = Paint().apply {
            color = 0xFFFF5252.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(40f, y, 46f, y + 120f), 16f, 16f, alertLinePaint)
        canvas.drawRect(43f, y, 46f, y + 120f, alertLinePaint) // Quadrado sobreposto para cantos retos do lado de dentro

        // Conteúdo da Caixa Médica
        textPaint.textSize = 11f
        var internalY = y + 25f
        
        // EMD
        textPaint.isFakeBoldText = true
        canvas.drawText("Exame Médico Desportivo (EMD):", 60f, internalY, textPaint)
        textPaint.isFakeBoldText = false
        val emdState = checkEmdStatus(athlete.emd_validade)
        val emdLabel = when (emdState) {
            "VALIDO" -> "VÁLIDO (Até ${athlete.emd_validade})"
            "AVISO" -> "VAI EXPIRAR BREVEMENTE (Até ${athlete.emd_validade}) ⚠️"
            else -> "EXPIRADO OU EM FALTA 🔴"
        }
        val emdColor = when (emdState) {
            "VALIDO" -> 0xFF4CAF50.toInt()
            "AVISO" -> 0xFFFF9800.toInt()
            else -> 0xFFF44336.toInt()
        }
        val emdPaint = Paint(textPaint).apply { color = emdColor; isFakeBoldText = true }
        canvas.drawText(emdLabel, 240f, internalY, emdPaint)

        internalY += 22f
        // Grupo Sanguíneo
        textPaint.isFakeBoldText = true
        canvas.drawText("Grupo Sanguíneo:", 60f, internalY, textPaint)
        textPaint.isFakeBoldText = false
        val gSanguineo = "Não Indicado / Pendente"
        canvas.drawText(gSanguineo, 240f, internalY, textPaint)

        internalY += 22f
        // Termo de Responsabilidade
        textPaint.isFakeBoldText = true
        canvas.drawText("Termo de Responsabilidade:", 60f, internalY, textPaint)
        textPaint.isFakeBoldText = false
        val termoEntregue = athlete.termo_responsabilidade_assinado == true
        val termoLabel = if (termoEntregue) "ENTREGUE E ASSINADO (Válido)" else "NÃO ENTREGUE / PENDENTE 🔴"
        val termoPaint = Paint(textPaint).apply {
            color = if (termoEntregue) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
            isFakeBoldText = true
        }
        canvas.drawText(termoLabel, 240f, internalY, termoPaint)

        internalY += 22f
        // Contacto Principal de Emergência
        textPaint.isFakeBoldText = true
        canvas.drawText("Contacto de Emergência:", 60f, internalY, textPaint)
        textPaint.isFakeBoldText = false
        val emergencyContact = athlete.encarregado_educacao_contacto ?: athlete.phone ?: "Não Indicado"
        val emergencyName = if (!athlete.encarregado_educacao_nome.isNullOrBlank()) " (${athlete.encarregado_educacao_nome})" else ""
        val contactPaint = Paint(textPaint).apply { isFakeBoldText = true; color = 0xFF001E35.toInt() }
        canvas.drawText("$emergencyContact$emergencyName", 240f, internalY, contactPaint)

        // 5. CONTACTOS FAMILIARES (Encarregados de Educação)
        y = 440f
        accentPaint.color = 0xFF00E5FF.toInt() // Cyber Cyan
        canvas.drawRoundRect(RectF(40f, y, 70f, y + 4f), 2f, 2f, accentPaint)
        
        textPaint.apply {
            color = 0xFF001E35.toInt()
            textSize = 14f
            isFakeBoldText = true
        }
        canvas.drawText("ENCARREGADO DE EDUCAÇÃO E FAMÍLIA", 80f, y + 8f, textPaint)

        y += 25f
        val familyCardRect = RectF(40f, y, 555f, y + 75f)
        canvas.drawRoundRect(familyCardRect, 16f, 16f, whitePaint)
        canvas.drawRoundRect(familyCardRect, 16f, 16f, borderPaint)

        // Conteúdo Familiar
        textPaint.textSize = 11f
        internalY = y + 25f
        
        textPaint.isFakeBoldText = true
        canvas.drawText("Nome do Encarregado:", 60f, internalY, textPaint)
        textPaint.isFakeBoldText = false
        canvas.drawText(athlete.encarregado_educacao_nome ?: "Não Indicado", 220f, internalY, textPaint)

        internalY += 22f
        textPaint.isFakeBoldText = true
        canvas.drawText("Telemóvel Encarregado:", 60f, internalY, textPaint)
        textPaint.isFakeBoldText = false
        canvas.drawText(athlete.encarregado_educacao_contacto ?: "Não Indicado", 220f, internalY, textPaint)

        // 6. DECLARAÇÃO DE RESPONSABILIDADE & ASSINATURAS
        y = 570f
        accentPaint.color = 0xFF718096.toInt()
        canvas.drawRoundRect(RectF(40f, y, 70f, y + 4f), 2f, 2f, accentPaint)
        
        textPaint.apply {
            color = 0xFF001E35.toInt()
            textSize = 14f
            isFakeBoldText = true
        }
        canvas.drawText("VALIDAÇÃO REGULAMENTAR DA ASSOCIAÇÃO/CLUBE", 80f, y + 8f, textPaint)

        y += 25f
        val signatureCardRect = RectF(40f, y, 555f, y + 150f)
        canvas.drawRoundRect(signatureCardRect, 16f, 16f, whitePaint)
        canvas.drawRoundRect(signatureCardRect, 16f, 16f, borderPaint)

        textPaint.apply {
            color = 0xFF718096.toInt()
            textSize = 10f
            isFakeBoldText = false
        }
        val declarationText1 = "Declara-se que o atleta acima mencionado se encontra devidamente registado"
        val declarationText2 = "na nossa Escola de Ciclismo. Em caso de acidente ou emergência médica no decorrer"
        val declarationText3 = "das provas FPC, os contactos telefónicos acima descritos devem ser utilizados imediatamente."
        canvas.drawText(declarationText1, 60f, y + 25f, textPaint)
        canvas.drawText(declarationText2, 60f, y + 38f, textPaint)
        canvas.drawText(declarationText3, 60f, y + 51f, textPaint)

        // Linhas de assinatura
        val sigLinePaint = Paint().apply {
            color = 0xFFCBD5E0.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawLine(70f, y + 115f, 250f, y + 115f, sigLinePaint)
        canvas.drawLine(340f, y + 115f, 520f, y + 115f, sigLinePaint)

        textPaint.apply {
            textSize = 8f
            color = 0xFF718096.toInt()
            isFakeBoldText = true
        }
        canvas.drawText("ASSINATURA DO DIRETOR DESPORTIVO", 88f, y + 130f, textPaint)
        canvas.drawText("ASSINATURA DO ENCARREGADO", 368f, y + 130f, textPaint)

        // 7. RODAPÉ DE SEGURANÇA E DATA
        val footerDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        textPaint.apply {
            textSize = 8f
            color = 0xFF718096.toInt()
            isFakeBoldText = false
        }
        canvas.drawText("Documento gerado automaticamente pelo CantanhedeHub em $footerDate.", 40f, 800f, textPaint)
        canvas.drawText("Confidencialidade Médica Regulamentada pelo RGPD.", 40f, 812f, textPaint)

        pdfDocument.finishPage(page)

        // Gravação em cache interna
        try {
            val cachePath = File(context.cacheDir, "fichas_emergencia")
            if (!cachePath.exists()) cachePath.mkdirs()
            val file = File(cachePath, "Ficha_Emergencia_${athlete.name.replace(" ", "_")}.pdf")
            
            val fileOutputStream = FileOutputStream(file)
            pdfDocument.writeTo(fileOutputStream)
            fileOutputStream.close()
            pdfDocument.close()

            // Partilhar o arquivo PDF via Sharesheet
            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            if (contentUri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Enviar Ficha de Emergência"))
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao exportar PDF: ${e.message}", Toast.LENGTH_LONG).show()
            pdfDocument.close()
        }
    }
}
