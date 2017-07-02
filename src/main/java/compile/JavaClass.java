package compile;

public class JavaClass {
    private final String name;
    private final String source;

    public JavaClass(final String name, final String source) {
        this.name = name;
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public String getSource() {
        return source;
    }
}
