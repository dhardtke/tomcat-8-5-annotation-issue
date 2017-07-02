import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Weird: If I execute this class directly in the Project that also contains the CompileWebService everything works fine,
 * but not if this class is being executed in a separate project.
 */
public class Standalone {
    private final static String JAR_PATH = "C:\\Users\\nick\\IntelliJ Projects\\tomcat-8-5-annotation-issue\\build\\libs\\tomcat85annotationissue-1.0-SNAPSHOT.jar";

    public static void main(String[] a) throws Exception {
        URL[] urls = new URL[]{
                new File(JAR_PATH).toURI().toURL()
        };

        ClassLoader classLoader = new URLClassLoader(urls);
        Class<?> aClass = classLoader.loadClass("web.CompileWebService");
        Object instance = aClass.newInstance();
        Method compile = instance.getClass().getMethod("compile");
        Object result = compile.invoke(instance);

        System.err.println(result);
    }
}
