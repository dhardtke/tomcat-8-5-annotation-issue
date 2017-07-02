package web;

import compile.CompilerFeedback;
import compile.InMemoryCompiler;
import compile.JavaClass;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Path("/")
public class CompileWebService {
    /**
     * You might need to adjust this to point to the directory where you put the hamcrest-core-1.3.jar and junit-4.12.jar.
     */
    private final static String LIBRARY_PATH = "C:\\tools\\codex-libraries";

    /**
     * When invoking this method by executing it on the shell, everything works fine.
     */
    public static void main(String[] a) throws Exception {
        System.err.println(new CompileWebService().compile());
    }

    /**
     * When calling this method via HTTP, only the annotation defined directly in the source code is detected.
     */
    @GET
    @Path("compile")
    public String compile() throws Exception {
        final String source = "package minimal_example;" +
                "import org.junit.Test;" +
                "import static org.junit.Assert.*;" +
                "import java.lang.annotation.*;" +
                "@Documented\n" +
                "@Retention(value=RetentionPolicy.RUNTIME)\n" +
                "@Target(value=ElementType.METHOD)\n" +
                "@interface MinimalAnnotation { }\n" +
                "public class MinimalExample {" +
                "    @Test\n" +
                "    @MinimalAnnotation\n" +
                "    public void testTrivial() {" +
                "        assertTrue(true);" +
                "    }" +
                "}";
        List<JavaClass> sourceCodeList = Collections.singletonList(new JavaClass("minimal_example.MinimalExample", source));

        InMemoryCompiler compiler = InMemoryCompiler.compile(sourceCodeList, LIBRARY_PATH);
        CompilerFeedback compile = compiler.getCompilerFeedback();

        if (!compile.isSuccess()) {
            throw new RuntimeException("Compilation failed: " + compile.getMessages());
        }

        Class<?> compiledClass = compiler.getCompiledClass("minimal_example.MinimalExample");

        Method testTrivial = compiledClass.getMethod("testTrivial");
        Annotation[] annotations = testTrivial.getAnnotations();

        // Expected output:
        // ---
        // Detected amount of annotations: 2, Detail: [@org.junit.Test(timeout=0, expected=class org.junit.Test$None), @minimal_example.MinimalAnnotation()]
        // ---
        return "Detected amount of annotations: " + annotations.length + ", Detail: " + Arrays.toString(annotations);
    }
}
