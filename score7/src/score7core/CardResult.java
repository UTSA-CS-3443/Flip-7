package score7core;

/** What happened as a result of {@link Flip7Core#addCard}. */
public enum CardResult {
    /** Card was simply added to the hand. */
    ADDED,
    /** A duplicate was absorbed by the player's SECOND_CHANCE; neither card stays in the hand. */
    SECOND_CHANCE_USED,
    /** A duplicate number card with no SECOND_CHANCE: the player busted. */
    BUSTED,
    /** A FREEZE was added: the player is out of the round and banks their hand. */
    FROZEN,
    /** The player collected seven unique number cards. The round ended immediately. */
    FLIP_7
}
