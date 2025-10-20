package com.example.juego.model

class Deck {
    private val cards = mutableListOf<Card>()

    init {
        createDeck()
    }

    private fun createDeck() {
        cards.clear()
        for (suit in Suit.values()) {
            for (rank in Rank.values()) {
                cards.add(Card(rank, suit))
            }
        }
    }

    fun shuffle() {
        cards.shuffle()
    }

    fun drawCard(): Card {
        // Si la baraja se vacía, se crea y baraja una nueva
        if (cards.isEmpty()) {
            createDeck()
            shuffle()
        }
        return cards.removeAt(0)
    }

    // Un método conveniente para empezar un juego
    fun drawHand(): MutableList<Card> {
        return mutableListOf(drawCard(), drawCard())
    }
}