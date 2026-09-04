package src.parser;

import java.io.File;
import java.util.*;
import java.util.zip.*;

public class FileSearcher{
    public static List<File> findFiles(String zipName){
        File root = new File(System.getProperty("java.home")).getParentFile();
        List<File> found = findFilesRecursively(root, zipName);

        return new ArrayList<>(new LinkedHashSet<>(found));
    }

    private static List<File> findFilesRecursively(File dir, String targetName){
        List<File> results = new ArrayList<>();
        if(dir == null || !dir.exists() || !dir.isDirectory()) return results;

        File[] files = dir.listFiles();
        if(files == null) return results;

        for(File f : files){
            if(f.isDirectory()){
                results.addAll(findFilesRecursively(f, targetName)); // 再帰
            }else if(f.getName().equalsIgnoreCase(targetName)){
                results.add(f);
            }
        }

        return results;
    }

    public static List<String> getJavaClassesList(File file, String type){
        List<String> list = new ArrayList<>();
        String filePath = file.getAbsolutePath();
        try{
            ZipFile zip = new ZipFile(filePath);

            zip.stream()
               .filter(e -> e.getName().endsWith("." + type))
               .forEach(e -> {
                   String name = e.getName()
                                  .replace("/", ".")
                                  .replace("." + type, "");
                   if(!isInternalApi(name) && classExists(name)) list.add(name);
               });

            zip.close();
            return new ArrayList<>(new LinkedHashSet<>(list));
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return null;
    }
    private static boolean classExists(String className){
        try{
            Class.forName(className, false, ClassLoader.getSystemClassLoader());
            return true;
        }catch(ClassNotFoundException e){
            return false;
        }catch(SecurityException e){
            System.err.println("Security restriction: " + className);
            return false;
        }
    }
    private static boolean isInternalApi(String className){
        return className.startsWith("sun.")
            || className.startsWith("com.sun.")
            || className.startsWith("jdk.internal")
            || className.startsWith("javafx.")
            || className.startsWith("org.jcp.xml.dsig.internal.")
            || className.contains(".internal.")
            || className.matches("java.lang.[A-Z]\\w*");
    }
}