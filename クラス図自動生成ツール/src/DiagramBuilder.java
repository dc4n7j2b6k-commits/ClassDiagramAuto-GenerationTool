package src;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.*;
import javax.lang.model.element.*;

import src.infos.*;
import src.draw.*;
import src.parser.*;

public class DiagramBuilder{
    private Map<RelationType, Integer> priority;
    private List<PackageFolder> folders;
    private Set<String> API = new HashSet<>();

    public DiagramBuilder(){
        List<File> fileListJar = new ArrayList<>(FileSearcher.findFiles("rt.jar"));
        for(File file : fileListJar){
            for(String element : FileSearcher.getJavaClassesList(file, "class")){
                if(element.contains("$")) continue;
                API.add(element);
            }
        }
        priority = new HashMap<>();
        priority.put(RelationType.EXTENDS, 6);
        priority.put(RelationType.IMPLEMENTS, 5);
        priority.put(RelationType.COMPOSITION, 4);
        priority.put(RelationType.AGGREGATION, 3);
        priority.put(RelationType.ASSOCIATION, 2);
        priority.put(RelationType.DEPENDENCY, 1);
    }
    public List<ClassInfo> parseDirectory(File dir){
        List<ClassInfo> infos = new ArrayList<>();
        File[] files = collectJavaFiles(dir);
        if(files != null){
            JavaClassParser parser = new JavaClassParser(files, API);
            try{
                infos.addAll(parser.parse());
            }catch(IOException e){
                e.printStackTrace();
            }
        }
        return infos;
    }
    public File[] collectJavaFiles(File root){
        List<File> result = new ArrayList<>();
        fileScan(root, result);
        return result.toArray(new File[0]);
    }
    private void fileScan(File file, List<File> result){
        if(file.isDirectory()){
            for(File f : file.listFiles()){
                fileScan(f, result);
            }
        }else if(file.isFile() && file.getName().endsWith(".java")){
            result.add(file);
        }
    }
    public List<PackageFolder> detectPackageFolder(List<ClassInfo> infos){
        Map<String, PackageFolder> folderMap = new HashMap<>();
        Map<String, ClassInfo> classMap = new HashMap<>();
        for(ClassInfo c : infos) flatten(c, classMap);

        for(ClassInfo clazz : classMap.values()){
            String pkg = clazz.packageName;
            if(pkg == null || pkg.isEmpty()) continue;
            PackageFolder folder = folderMap.computeIfAbsent(pkg, p -> new PackageFolder(p));
            folder.classes.add(clazz);
        }
        return new ArrayList<>(folderMap.values());
    }
    public List<ClassRelation> detectRelations(List<ClassInfo> infos){
        List<ClassRelation> result = new ArrayList<>();
        Map<String, ClassInfo> classMap = new HashMap<>();
        for(ClassInfo info : infos){
            flatten(info, classMap);
            for(ClassInfo nest : info.nestedClasses)
                flatten(nest, classMap); 
        }

        for(ClassInfo clazz : classMap.values()){
            String from = clazz.qualifiedName;
            // 継承
            if(clazz.superClass != null && classMap.containsKey(clazz.superClass)){
                String superClass = clazz.superClass;
                result.add(new ClassRelation(from, superClass, RelationType.EXTENDS));
            }
            // インターフェース
            for(String itf : clazz.interfaces){
                if(classMap.containsKey(itf)){
                    result.add(new ClassRelation(from, itf, RelationType.IMPLEMENTS));
                }
            }
            // ネストクラス
            for(ClassInfo nestedClazz : clazz.nestedClasses){
                String nest = nestedClazz.qualifiedName;
                if(classMap.containsKey(nest)){
                    for(String mod : nestedClazz.modifiers){
                        if(mod.equals("static")){
                            result.add(new ClassRelation(from, nest, RelationType.DEPENDENCY));
                        }else{
                            result.add(new ClassRelation(from, nest, RelationType.COMPOSITION));
                        }
                    }
                }
            }
            // フィールド解析
            for(FieldInfo f : clazz.fields){
                int i = 0;
                String multiplicity = detectMultiplicity(f.type);
                for(String typeName : extractTypeNames(f.type)){
                    if(from.equals(typeName.replaceAll("\\[\\]+", ""))) continue;
                    if(API.contains(typeName)) i++;
                    if(!classMap.containsKey(typeName.replaceAll("\\[\\]+", ""))) continue;
                    String newTypeName = typeName.replaceAll("\\[\\]+", "");

                    ClassRelation relation;

                    if(f.initializer != null){
                        String init = f.initializer.getRawName();
                        if(init.startsWith("new ")){ // 初期化がインスタンス
                            if(typeName.contains("[")){
                                relation = new ClassRelation(from, newTypeName, RelationType.AGGREGATION);
                                relation.toMultiplicity = multiplicity;
                                result.add(relation);
                            }else if(i == 0){
                                relation = new ClassRelation(from, typeName, RelationType.COMPOSITION);
                                relation.toMultiplicity = multiplicity;
                                result.add(relation);
                            }else{
                                relation = new ClassRelation(from, typeName, RelationType.AGGREGATION);
                                relation.toMultiplicity = multiplicity;
                                result.add(relation);
                            }
                        }else if(init.startsWith("{") && init.endsWith("}") && typeName.contains("[")){
                            relation = new ClassRelation(from, newTypeName, RelationType.COMPOSITION);
                            relation.toMultiplicity = multiplicity;
                            result.add(relation);
                        }
                    } 
                    AssignmentInfo assign = clazz.getAssignmentToField(f.name);
                    if(assign != null){
                        if(assign.isNewInstance()){
                            if(typeName.contains("[")){
                                String expr = assign.exprName.trim();
                                if(expr.contains("new " + typeName + "{") && expr.endsWith("}")){
                                    relation = new ClassRelation(from, newTypeName, RelationType.COMPOSITION);
                                    relation.toMultiplicity = multiplicity;
                                    result.add(relation);
                                }else{
                                    relation = new ClassRelation(from, newTypeName, RelationType.AGGREGATION);
                                    relation.toMultiplicity = multiplicity;
                                    result.add(relation);
                                }
                            }else if(i == 0){
                                relation = new ClassRelation(from, newTypeName, RelationType.COMPOSITION);
                                relation.toMultiplicity = multiplicity;
                                result.add(relation);
                            }else{
                                relation = new ClassRelation(from, newTypeName, RelationType.AGGREGATION);
                                relation.toMultiplicity = multiplicity;
                                result.add(relation);
                            }
                        }else if(assign.isFromParameter()){
                            relation = new ClassRelation(from, newTypeName, RelationType.AGGREGATION);
                            relation.toMultiplicity = multiplicity;
                            result.add(relation);
                        }
                    }
                    result.add(new ClassRelation(from, newTypeName, RelationType.ASSOCIATION));
                    i++;
                }
            }
            // 初期ブロック
            for(InitializeBlockInfo i : clazz.initBlocks){
                for(TypeInfo t : i.localTypes){
                    for(String typeName : extractTypeNames(t)){
                        typeName = typeName.replaceAll("\\[\\]+", "");
                        if(from.equals(typeName)) continue;
                        if(!classMap.containsKey(typeName)) continue;
                        result.add(new ClassRelation(from, typeName, RelationType.DEPENDENCY));
                    }
                }
            }
            // コンストラクタ解析
            for(ConstructorInfo c : clazz.constructors){
                for(TypeInfo t : c.args.values()){
                    for(String typeName : extractTypeNames(t)){
                        typeName = typeName.replaceAll("\\[\\]+", "");
                        if(from.equals(typeName)) continue;
                        if(!classMap.containsKey(typeName)) continue;
                        result.add(new ClassRelation(from, typeName, RelationType.DEPENDENCY));
                    }
                }
                for(TypeInfo t : c.localTypes){
                    for(String typeName : extractTypeNames(t)){
                        typeName = typeName.replaceAll("\\[\\]+", "");
                        if(from.equals(typeName)) continue;
                        if(!classMap.containsKey(typeName)) continue;
                        result.add(new ClassRelation(from, typeName, RelationType.DEPENDENCY));
                    }
                }
            }
            // メソッド解析
            for(MethodInfo m : clazz.methods){
                for(TypeInfo t : m.args.values()){
                    for(String typeName : extractTypeNames(t)){
                        typeName = typeName.replaceAll("\\[\\]+", "");
                        if(from.equals(typeName)) continue;
                        if(!classMap.containsKey(typeName)) continue;
                        result.add(new ClassRelation(from, typeName, RelationType.DEPENDENCY));
                    }
                }
                for(TypeInfo t : m.localTypes){
                    for(String typeName : extractTypeNames(t)){
                        typeName = typeName.replaceAll("\\[\\]+", "");
                        if(from.equals(typeName)) continue;
                        if(!classMap.containsKey(typeName)) continue;
                        result.add(new ClassRelation(from, typeName, RelationType.DEPENDENCY));
                    }
                }
                for(String typeName : extractTypeNames(m.returnType)){
                    typeName = typeName.replaceAll("\\[\\]+", "");
                    if(from.equals(typeName)) continue;
                    if(!classMap.containsKey(typeName)) continue;
                    result.add(new ClassRelation(from, typeName, RelationType.DEPENDENCY));
                }
            }
        }
        result = filterRelation(result);
        return result;
    }
    private void flatten(ClassInfo ci, Map<String, ClassInfo> classMap){
        classMap.put(ci.qualifiedName, ci);
        for(ClassInfo nc : ci.nestedClasses) flatten(nc, classMap);
    }
    public List<ClassBox> layout(List<ClassInfo> infos){ // クラスボックスの作成
        List<ClassBox> boxes = new ArrayList<>();
        folders = detectPackageFolder(infos);
        int x = 50, y = 50;
        for(ClassInfo info : infos){
            for(ClassInfo nestedInfo : info.nestedClasses){
                boxes.add(buildClassBox(nestedInfo, x, y));
            }
            boxes.add(buildClassBox(info, x, y));
            x += 500;
            if(x > 5000){
                x = 50;
                y += 1000;
            }
        }
        return boxes;
    }
    private ClassBox buildClassBox(ClassInfo info, int x, int y){
        ClassBox box = new ClassBox(info.line, info.qualifiedName, info.name, info.kind, info.isJavaAPI, x, y, 200, 100);
        for(String s : info.enumConstants) box.enumConstants.add(s);
        for(FieldInfo f : info.fields) box.fields.add(f);
        for(MethodInfo m : info.methods) box.methods.add(m);
        for(ConstructorInfo m : info.constructors) box.constructors.add(m);
        for(PackageFolder pkg : folders){
            if(!pkg.packageName.equals(info.packageName)) continue;
            pkg.boxes.add(box);
        }
        return box;
    }
    private List<String> extractTypeNames(TypeInfo type){
        List<String> names = new ArrayList<>();
        if(type == null) return names;
        String raw = type.getQualifiedName();
        if(!raw.contains("<")) names.add(raw);
        else{
            int idx = raw.indexOf("<");
            names.add(raw.substring(0, idx));
            String inner = raw.substring(idx + 1, raw.length() - 1);
            for(String part : inner.split(",")){
                names.addAll(extractTypeNamesFromString(part.trim()));
            }
        }
        return names;
    }
    private List<String> extractTypeNamesFromString(String s){
        if(!s.contains("<")) return List.of(s);
        int idx = s.indexOf("<");
        String outer = s.substring(0, idx);
        String inner = s.substring(idx + 1, s.length() - 1);
        List<String> list = new ArrayList<>();
        list.add(outer);
        for(String part : inner.split(",")){
            list.addAll(extractTypeNamesFromString(part.trim()));
        }
        return list;
    }
    private String detectMultiplicity(TypeInfo type){
        if(type == null) return " ";
    
        String raw = type.getQualifiedName();
        if(raw == null || raw.isEmpty()) return " ";

        raw = raw.trim();
        if(raw.endsWith("[]")) return "0..*";

        // 1. ジェネリクスや型名から実際の Class オブジェクトを取得する（※環境に合わせて要調整）
        Class<?> clazz = targetClass(raw); 
        if(clazz == null) return "1";

        // 2. 法則（親子関係）ベースで判定する
        if(Optional.class.isAssignableFrom(clazz)){
            return "0..1";
        }
        // Iterable（List, Set, Collection等の親）または Map の仲間ならすべて 0..*
        if(Iterable.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz)){
            return "0..*";
        }

        return "1";
    }
    private Class<?> targetClass(String raw){
        // 1. ジェネリクス（<...>）が含まれている場合は、それ以降を削る
        int index = raw.indexOf('<');
        String className = (index >= 0) ? raw.substring(0, index) : raw;

        try{
            // 2. クラス名から実際の Class オブジェクトをロードして返す
            return Class.forName(className.trim());
        }catch(ClassNotFoundException e){
            // クラスが見つからない（外部ライブラリの型など）場合は null を返す
            return null; 
        }
    }
    private List<ClassRelation> filterRelation(List<ClassRelation> relations){
        Map<String, ClassRelation> relationMap = new HashMap<>();

        for(ClassRelation rel : relations){
            if(rel.from.equals(rel.to)) continue;
            String key = rel.from + "->" + rel.to;
            String multiplicity = rel.toMultiplicity;

            if(!relationMap.containsKey(key)){
                relationMap.put(key, rel);
            }else{
                ClassRelation existing = relationMap.get(key);
                String mergedMultiplicity = mergeMultiplicity(existing.toMultiplicity, rel.toMultiplicity);

                int existingPriority = priority.getOrDefault(existing.type, 0);
                int newPriority = priority.getOrDefault(rel.type, 0);
                if(newPriority > existingPriority){
                    rel.toMultiplicity = mergedMultiplicity;
                    relationMap.put(key, rel);
                }else{
                    existing.toMultiplicity = mergedMultiplicity;
                }
            }
        }
        return new ArrayList<>(relationMap.values());
    }
    private String mergeMultiplicity(String current, String incoming){
        if(current == null) return incoming;
        if(incoming == null) return current;
        if(current.equals(incoming)) return current;

        boolean currentIsMany = current.contains("*");
        boolean incomingIsMany = incoming.contains("*");
        if(currentIsMany || incomingIsMany){
            return currentIsMany ? current : incoming;
        }
        return current;
    }
    public List<PackageFolder> getPackageFolderList(){
        return folders;
    }
}