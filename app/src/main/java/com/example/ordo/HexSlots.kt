package com.example.ordo

object HexSlots {
    val Active = RenderProps(320, 2160, 290, 1f)
    val ParentLeft = RenderProps(-27, 2160, 290, 1f)
    val StartButton = RenderProps(100, 2300, 172, 1f)

    fun getSiblingProps(index: Int): RenderProps {
        val xPos = if (index == 0) 150 else 493
        return RenderProps(xPos, 2442, 290, 1f)
    }
}