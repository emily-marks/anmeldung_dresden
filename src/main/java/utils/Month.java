package utils;

public enum Month {
    Januar (1),
    Juni(6),
    Juli(7),
    August(8),
    September(9),
    Oktober(10),
    November(11),
    Dezember(12);

    private final int id;

    Month(int id) {
        this.id = id;
    }

    public static Month getMonthByName (String name) {
        return Month.valueOf(name);
    }

    public static int getMonthId (String name) {
        return getMonthByName(name).id;
    }

    public boolean isJune() {
        return this.equals(Juni);
    }

    public boolean isAugust() {
        return this.equals(August);
    }
}
