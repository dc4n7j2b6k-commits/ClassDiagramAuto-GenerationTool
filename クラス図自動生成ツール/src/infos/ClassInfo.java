package src.infos;

import java.util.*;
import src.draw.*;

public class ClassInfo{
    public String name, packageName, qualifiedName, kind, superClass;
    public List<String> importNames;
    public boolean superclassIsJavaAPI = false, isJavaAPI = false;
    public Set<String> modifiers = new HashSet<>();
    public List<InitializeBlockInfo> initBlocks = new ArrayList<>();
    public List<String> enumConstants = new ArrayList<>();
    public List<FieldInfo> fields = new ArrayList<>();
    public List<MethodInfo> methods = new ArrayList<>();
    public List<ConstructorInfo> constructors = new ArrayList<>();
    public List<ClassInfo> nestedClasses = new ArrayList<>();
    public List<String> interfaces = new ArrayList<>();
    public List<Boolean> interfaceIsJavaAPI = new ArrayList<>();
    public StyledLine line;

    public AssignmentInfo getAssignmentToField(String fieldName){
        for(ConstructorInfo ci : constructors){
            for(AssignmentInfo ai : ci.ai){
                if(ai.exprName.equals(fieldName)) return ai;
            }
        }
        for(InitializeBlockInfo ibi : initBlocks){
            for(AssignmentInfo ai : ibi.ai){
                if(ai.exprName.equals(fieldName) && ai.isNewInstance()) return ai;
            }
        }
        return null;
    }
    public void toStyledLine(){
        line = new StyledLine(name, modifiers.contains("static"), modifiers.contains("final"), modifiers.contains("abstract"));
    }
    public String toString(){
        return String.format("ClassInfo[%s]", qualifiedName);
    }
}