package com.example.ordo

object HexGridData {
    // Твои замеры линейкой из файла ПОХИЦИИ.txt
    val slots = mapOf(
        1 to Pair(322, 2160), 2 to Pair(150, 1850), 3 to Pair(493, 1850),
        4 to Pair(673, 2160), 5 to Pair(836, 2442), 6 to Pair(493, 2442),
        7 to Pair(150, 2442), 8 to Pair(-27, 2160), 9 to Pair(-27, 1540),
        10 to Pair(323, 1540), 11 to Pair(673, 1540), 12 to Pair(836, 1850),
        13 to Pair(999, 2160), 14 to Pair(150, 1230), 15 to Pair(493, 1230),
        16 to Pair(836, 1230), 17 to Pair(999, 1540), 18 to Pair(-27, 920),
        19 to Pair(323, 920), 20 to Pair(673, 920), 21 to Pair(999, 920),
        22 to Pair(150, 610), 23 to Pair(493, 610), 24 to Pair(836, 610),
        25 to Pair(-27, 310), 26 to Pair(323, 310), 27 to Pair(673, 310),
        28 to Pair(999, 310)
    )

    fun getFlyoutTargets(plusSlotIndex: Int): List<Int> {
        return when (plusSlotIndex) {
            // Из центра (Слот 1) летим в стандартный треугольник сверху
            1 -> listOf(10, 15, 19)

            // Из Слота 3 (верх-право) летим еще правее и выше
            3 -> listOf(11, 16, 20)

            // Из Слота 4 (право) летим в крайние правые слоты
            4 -> listOf(12, 17, 21)

            // Из Слота 5 (низ-право) летим в нижний угол
            5 -> listOf(13, 17, 21)

            else -> listOf(10, 14, 19)
        }
    }
}