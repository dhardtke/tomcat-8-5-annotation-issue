package compile;

import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivilegedAction;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.security.AccessController.doPrivileged;

/**
 * @author Dominik Hardtke
 * @author Rekha Kumari
 * @see <a href="https://stackoverflow.com/questions/12173294/compile-code-fully-in-memory-with-javax-tools-javacompiler">StackOverflow.com</a>
 * @see <a href="http://javapracs.blogspot.de/2011/06/dynamic-in-memory-compilation-using.html">Rekha Kumari's Blog</a>
 * @since 10.06.2017
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class InMemoryCompiler {
    /**
     * An instance of the JavaFileManager used to load Java Classes.
     */
    private final JavaFileManager fileManager;

    /**
     * The CompilerFeedback.
     */
    private final CompilerFeedback compilerFeedback;

    /**
     * Constructs a new InMemoryCompiler.
     *
     * @param compilerFeedback the CompilerFeedback of the Compilation
     * @param fileManager      the JavaFileManager to be used to fetch already compiled Classes
     */
    private InMemoryCompiler(final CompilerFeedback compilerFeedback, final JavaFileManager fileManager) {
        this.compilerFeedback = compilerFeedback;
        this.fileManager = fileManager;
    }

    /**
     * Compiles the Classes passed to the method.
     *
     * @param javaClasses the List of JavaClasses to compile
     * @return the InMemoryCompiler instance
     */
    public static InMemoryCompiler compile(final List<JavaClass> javaClasses, final String libraryPath) {
        final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();

        /*
        final JavaCompiler javaCompiler;
        try {
            javaCompiler = (JavaCompiler)Class.forName("com.sun.tools.javac.api.JavacTool").newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        */

        if (javaCompiler == null) {
            throw new RuntimeException("ToolProvider.getSystemJavaCompiler() returned null! This program needs to be run on a system with an installed JDK.");
        }

        // prepare compilation
        JavaFileManager fileManager = new ForwardingJavaFileManager<JavaFileManager>(
                javaCompiler.getStandardFileManager(null, null, null)) {
            private final Map<String, ByteArrayOutputStream> byteStreams = new HashMap<>();

            @Override
            public ClassLoader getClassLoader(final Location location) {
                return doPrivileged(new PrivilegedAction<ClassLoader>() {
                    @Override
                    public ClassLoader run() {
                        return new SecureClassLoader() {
                            @Override
                            protected Class<?> findClass(final String className) throws ClassNotFoundException {
                                final ByteArrayOutputStream bos = byteStreams.get(className);

                                if (bos == null) {
                                    return null;
                                }

                                final byte[] b = bos.toByteArray();
                                return super.defineClass(className, b, 0, b.length);
                            }
                        };
                    }
                });
            }

            @Override
            public JavaFileObject getJavaFileForOutput(final Location location, final String className,
                                                       final JavaFileObject.Kind kind, final FileObject sibling) throws IOException {
                return new SimpleJavaFileObject(URI.create("string:///" + className.replace('.', '/') + kind.extension), kind) {
                    @Override
                    public OutputStream openOutputStream() throws IOException {
                        // retrieve the already stored ByteArrayOutputStream or add a new one to the Map
                        return byteStreams.computeIfAbsent(className, k -> new ByteArrayOutputStream());
                    }
                };
            }
        };

        // do the actual compiling
        final List<JavaFileObject> files = new ArrayList<>();

        for (JavaClass classSourceCode : javaClasses) {
            URI uri;

            try {
                uri = URI.create("string:///" + classSourceCode.getName().replace('.', '/') + JavaFileObject.Kind.SOURCE.extension);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            final SimpleJavaFileObject sjfo = new SimpleJavaFileObject(uri, JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
                    return classSourceCode.getSource();
                }
            };
            files.add(sjfo);
        }

        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        // The options passed to the JavaCompiler, see https://docs.oracle.com/javase/7/docs/technotes/tools/windows/javac.html
        List<String> options = new ArrayList<>();

        // set the class path to be able to use JUnit
        try {
            options.addAll(Arrays.asList("-cp", Files.list(Paths.get(libraryPath)).map(Path::toString).collect(Collectors.joining(File.pathSeparator))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final JavaCompiler.CompilationTask task = javaCompiler.getTask(null, fileManager, diagnostics, options, null, files);

        CompilerFeedback compilerFeedback = new CompilerFeedback(task.call(), diagnostics);

        return new InMemoryCompiler(compilerFeedback, fileManager);
    }

    /**
     * Gets a previously compiled Class.
     *
     * @param className the fully qualified Class of the Class to get
     * @return the Class instance
     * @throws ClassNotFoundException if the Class identified by the passed className cannot be found
     */
    public Class<?> getCompiledClass(final String className) throws ClassNotFoundException {
        final Class<?> ret = getClassLoader().loadClass(className);

        if (ret == null) {
            throw new ClassNotFoundException("Class returned by ClassLoader was null!");
        }

        return ret;
    }

    /**
     * @return the {@link JavaFileManager}'s ClassLoader that is used to load the compiled classes
     */
    public ClassLoader getClassLoader() {
        return fileManager.getClassLoader(null);
    }

    /**
     * @return the CompilerFeedback
     */
    public CompilerFeedback getCompilerFeedback() {
        return compilerFeedback;
    }
}
