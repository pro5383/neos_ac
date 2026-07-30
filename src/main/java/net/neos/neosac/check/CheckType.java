package net.neos.neosac.check;

public enum CheckType {
    SIMULATION("Симуляция/Физика"),
    PACKET("Пакеты"),
    INTERACTION("Взаимодействие"),
    COMBAT("Бой"),
    WORLD("Мир");

    private final String displayName;

    CheckType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
