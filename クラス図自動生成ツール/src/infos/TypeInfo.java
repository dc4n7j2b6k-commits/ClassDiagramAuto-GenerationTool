package src.infos;

import javax.lang.model.type.*;
import java.util.*;

public class TypeInfo{
    private String rawName;
    private String erasedName;
    private String qualifiedName;
    private List<TypeInfo> generics;
    private boolean isArray;
    private TypeInfo componentType;
    private TypeMirror mirror;

    public TypeInfo(String rawName){
        this.rawName = rawName;
        this.generics = new ArrayList<>();
    }
    public void setRawName(String rawName){
        this.rawName = rawName;
    }
    public String getRawName(){
        return rawName;
    }
    public void setErasedName(String erasedName){
        this.erasedName = erasedName;
    }
    public void setQualifiedName(String qualifiedName){
        this.qualifiedName = qualifiedName;
    }
    public String getQualifiedName(){
        return qualifiedName;
    }
    public void addGenerics(TypeInfo typeInfo){
        generics.add(typeInfo);
    }
    public void setArrayType(boolean isArray){
        this.isArray = isArray;
    }
    public boolean isArrayType(){
        return isArray;
    }
    public void setComponentType(TypeInfo componentType){
        this.componentType = componentType;
    }
    public void setTypeMirror(TypeMirror mirror){
        this.mirror = mirror;
    }
    public TypeMirror getTypeMirror(){
        return mirror;
    }
}