package dynamiccompilation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public final class DynamicInMemoryCompileHelper {

    public static Object compileAndNewInstance(String clsFullName, InputStream in) throws IOException,
            IllegalAccessException, InstantiationException, ClassNotFoundException {
        final String srcJavaCode = toString(in);
        return compileAndNewInstance(clsFullName, srcJavaCode);
    }

    protected static String toString(InputStream in) throws IOException, UnsupportedEncodingException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        final int BUFFER_SIZE = 8192;
        byte[] data = new byte[BUFFER_SIZE];
        int count = -1;
        while ((count = in.read(data, 0, BUFFER_SIZE)) != -1) {
            outStream.write(data, 0, count);
        }

        data = null;
        final String srcJavaCode = new String(outStream.toByteArray(), "UTF-8");
        return srcJavaCode;
    }

    public static Object compileAndNewInstance(String clsFullName, CharSequence srcJavaCode)
            throws  IOException,IllegalAccessException, InstantiationException, ClassNotFoundException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();       
    //     compiler = (JavaCompiler)Class.forName(compiler.getClass().getName()).newInstance();
        
        final StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(null, null, null);
        JavaFileManager fileManager = new ClassFileManager(
                standardFileManager);

        // Dynamic compiling requires specifying
        // a list of "files" to compile. In our case
        // this is a list containing one "file" which is in our case
        // our own implementation (see details below)
        List<JavaFileObject> jfiles = new ArrayList<JavaFileObject>();
        jfiles.add(new CharSequenceJavaFileObject(clsFullName, srcJavaCode));

        List<String> options = new ArrayList<String>();
        { //fixed for compile classpath issue 
        options.add("-classpath");
        StringBuilder sb = new StringBuilder();
        ClassLoader urlClassLoader = Thread.currentThread().getContextClassLoader();
        if(urlClassLoader instanceof  URLClassLoader ){
            for (URL url : ((URLClassLoader)urlClassLoader).getURLs()){
                sb.append(url.getFile()).append(File.pathSeparator);
            }            
        }
        
        final ClassLoader classLoader = DynamicInMemoryCompileHelper.class.getClassLoader();
       // System.out.println("DynamicInMemoryCompileHelper classLoader:"+classLoader );
        if(classLoader instanceof  URLClassLoader ){
            for (URL url : ((URLClassLoader)classLoader).getURLs()){
                sb.append(url.getFile()).append(File.pathSeparator);
            }            
        }
        options.add(sb.toString());
        System.out.println("options:"+options);
        
        }
        // We specify a task to the compiler. Compiler should use our file
        // manager and our list of "files".
        // Then we run the compilation with call()
        final CompilationTask task = compiler.getTask(null, fileManager, null, options, null, jfiles);
        task.call();

        standardFileManager.close();
     
        // Creating an instance of our compiled class and
        // running its toString() method
        Object instance = fileManager.getClassLoader(null).loadClass(clsFullName).newInstance();
        return instance;
    }
}
