package src.parser;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.*;

import javax.tools.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;

import src.infos.ClassInfo;

public class JavaClassParser{
    public File[] javaFiles;
    public Set<String> API;
    public JavaClassParser(File[] javaFiles, Set<String> API){
        this.javaFiles = javaFiles;
        this.API = API;
    }
    public List<ClassInfo> parse() throws IOException{
        List<ClassInfo> infos = new ArrayList<>();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
        List<JavaFileObject> files = new ArrayList<>();
        for(File f : javaFiles){
            files.add(fileManager.getJavaFileObjects(f).iterator().next());
        }

        JavacTask task = (JavacTask) compiler.getTask(null, fileManager, null, null, null, files);
        Iterable<? extends CompilationUnitTree> trees = task.parse();
        task.analyze();

        Trees treesUtil = Trees.instance(task);
        Types typesUtil = task.getTypes();

        ClassInfoExtractor extractor = new ClassInfoExtractor(typesUtil, treesUtil, API);
        for(CompilationUnitTree tree : trees) extractor.scan(tree, null);
        return extractor.classes;
    }
}