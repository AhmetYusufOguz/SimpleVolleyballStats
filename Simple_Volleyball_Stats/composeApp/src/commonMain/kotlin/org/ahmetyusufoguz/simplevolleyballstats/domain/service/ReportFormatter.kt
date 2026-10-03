package org.ahmetyusufoguz.simplevolleyballstats.domain.service

object ReportFormatter {

    fun formatToShareableText(report: MatchReport): String {
        return buildString {
            appendLine("🏐 SVS MAÇ RAPORU 🏐")
            appendLine("---------------------------------------")
            appendLine("${report.homeTeamName}  ${report.homeSetsWon} - ${report.awaySetsWon}  ${report.awayTeamName}")
            appendLine("---------------------------------------")
            
            appendLine("📋 Set Skorları:")
            report.setScores.forEachIndexed { index, (home, away) ->
                appendLine("  Set ${index + 1}: $home - $away")
            }
            appendLine()
            
            appendLine("📊 TAKIM İSTATİSTİKLERİ")
            appendLine("---------------------------------------")
            appendLine("Toplam Sayı: ${report.teamStats.totalPoints} | Toplam Hata: ${report.teamStats.totalErrors}")
            appendLine()
            
            appendActionRow("Servis", report.teamStats.serve)
            appendActionRow("Karşılama", report.teamStats.reception)
            appendActionRow("Smaç", report.teamStats.attack)
            appendActionRow("Blok", report.teamStats.block)
            appendActionRow("Savunma", report.teamStats.dig)
            appendActionRow("Pas", report.teamStats.set)
            appendLine()
            
            appendLine("👤 OYUNCU İSTATİSTİKLERİ")
            appendLine("---------------------------------------")
            report.playerStats.forEach { stats ->
                appendLine("▪ #${stats.player.number} ${stats.player.name}")
                appendLine("   Puan: ${stats.totalPoints} | Hata: ${stats.totalErrors}")
                
                if (stats.serve.total > 0) {
                    appendLine("   SRV: ${stats.serve.total} (Ace:${stats.serve.perfect} Err:${stats.serve.error})")
                }
                if (stats.reception.total > 0) {
                    appendLine("   RCV: ${stats.reception.total} (#:${stats.reception.perfect} Err:${stats.reception.error})")
                }
                if (stats.attack.total > 0) {
                    appendLine("   ATK: ${stats.attack.total} (Kill:${stats.attack.perfect} Err:${stats.attack.error}) %${stats.attack.efficiency.toInt()}")
                }
                if (stats.block.total > 0) {
                    appendLine("   BLK: ${stats.block.total} (Kill:${stats.block.perfect} Err:${stats.block.error})")
                }
                if (stats.dig.total > 0) {
                    appendLine("   DIG: ${stats.dig.total} (#:${stats.dig.perfect} Err:${stats.dig.error})")
                }
                if (stats.set.total > 0) {
                    appendLine("   SET: ${stats.set.total} (#:${stats.set.perfect} Err:${stats.set.error})")
                }
                appendLine()
            }
            
            appendLine("---------------------------------------")
            appendLine("📱 Simple Volleyball Stats ile oluşturuldu")
        }
    }

    private fun StringBuilder.appendActionRow(label: String, stats: ActionStats) {
        if (stats.total == 0) return
        appendLine("🔹 $label: ${stats.total}")
        appendLine("   (#:${stats.perfect} | +:${stats.positive} | =:${stats.error}) -> Verim: %${stats.efficiency.toInt()}")
    }
}
