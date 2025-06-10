package TTTConsoleOO;

public enum Seed {
    CROSS("X"), NOUGHT("O"), NO_SEED(" ");

    // Private variable
    private String icon;
    // Constructor (must be private)
    private Seed(String icon) {
        this.icon = icon;
    }
    // Public Getter
    public String getIcon() {
        return icon;
    }
}
