package score7core;

/**
 * Every card in Flip 7 that this library cares about.
 * Number cards carry their face value, modifier cards carry their bonus,
 * and action cards are tracked only so the hand history is complete.
 */
public enum Cards {
    ZERO(0, Kind.NUMBER),
    ONE(1, Kind.NUMBER),
    TWO(2, Kind.NUMBER),
    THREE(3, Kind.NUMBER),
    FOUR(4, Kind.NUMBER),
    FIVE(5, Kind.NUMBER),
    SIX(6, Kind.NUMBER),
    SEVEN(7, Kind.NUMBER),
    EIGHT(8, Kind.NUMBER),
    NINE(9, Kind.NUMBER),
    TEN(10, Kind.NUMBER),
    ELEVEN(11, Kind.NUMBER),
    TWELVE(12, Kind.NUMBER),

    XTWO(2, Kind.MULTIPLIER),   // doubles the number-card total

    PTWO(2, Kind.BONUS),        // flat bonuses, added after doubling
    PFOUR(4, Kind.BONUS),
    PSIX(6, Kind.BONUS),
    PEIGHT(8, Kind.BONUS),
    PTEN(10, Kind.BONUS),

    FLIP_THREE(0, Kind.ACTION), // logged only
    FREEZE(0, Kind.ACTION),     // the holder is frozen: treated as an automatic STAY
    SECOND_CHANCE(0, Kind.ACTION); // absorbs one duplicate instead of busting

    public enum Kind { NUMBER, MULTIPLIER, BONUS, ACTION }

    private final int value;
    private final Kind kind;

    Cards(int value, Kind kind) {
        this.value = value;
        this.kind = kind;
    }

    /** Face value for number cards, bonus for P cards, multiplier for XTWO, 0 for actions. */
    public int getValue() {
        return value;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isNumber() {
        return kind == Kind.NUMBER;
    }
}
