package dynamiccompilation;
import java.io.InputStream;

import dynamiccompilation.DynamicInMemoryCompileHelper;

public class DynaCompTest {
    public static void main(String[] args) throws Exception {
        // Full name of the class that will be compiled.
        // If class should be in some package,
        // fullName should contain it too
        // (ex. "testpackage.DynaClass")
        String fullName = "DynaClass";

        // Here we specify the source code of the class to be compiled
        StringBuilder src = new StringBuilder();
        src.append("public class DynaClass {\n");
        src.append("    public String toString() {\n");
        src.append("        return \"Hello, I am \" + ");
        src.append("this.getClass().getSimpleName();\n");
        src.append("    }\n");
        src.append("}\n");

        System.out.println(src);

        // We get an instance of JavaCompiler. Then
        // we create a file manager
        // (our custom implementation of it)
       
        
      //  Object instance = DynamicInMemoryCompileHelper.compileAndNewInstance(fullName, src);
        
        final InputStream javacodeSrc = DynamicInMemoryCompileHelper.class.getResourceAsStream("/winstone/SslKeyFactoryForIbmJdkImpl.javasrc");
        Object instance = DynamicInMemoryCompileHelper.compileAndNewInstance("winstone.SslKeyFactoryForIbmJdkImpl", javacodeSrc);
        
        
        System.out.println(instance);
        
        System.out.println(System.getProperties());
    }

  
}