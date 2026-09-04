package src.infos;

import java.util.*;

import src.draw.StyledLine;

public class ConstructorInfo{
    private String kind;
    public String name;
    public Set<String> modifiers;
    public Map<String, TypeInfo> args;
    public List<TypeInfo> localTypes;
    public List<AssignmentInfo> ai;
    public String body, parentPathClass;
    public StyledLine line;

    public ConstructorInfo(String kind, Set<String> modifiers, String name, Map<String, TypeInfo> args, List<TypeInfo> localTypes, List<AssignmentInfo> ai, String body, String parentPathClass){
        this.kind = kind;
        this.modifiers = modifiers;
        this.name = name;
        this.args = args;
        this.localTypes = localTypes;
        this.ai = ai;
        this.body = body;
        this.parentPathClass = parentPathClass;
        toStyledLine();
    }
    public void toStyledLine(){
        List<String> param = new ArrayList<>();
        for(Map.Entry<String, TypeInfo> entry : args.entrySet()) param.add(entry.getKey() + " : " + entry.getValue().getRawName());
        String parameter = "(" + String.join(", ", param) + ")";
        String text = accsessModifiers(modifiers) + " " + name + parameter;
        line = new StyledLine(text, modifiers.contains("static"), modifiers.contains("final"), modifiers.contains("abstract"));
    }
    private String accsessModifiers(Set<String> allMod){
        String[] access = {"public", "private", "protected", "+", "-", "#"};
        for(int i = 0; i < access.length / 2; i++){
            if(allMod.contains(access[i])) return access[i + 3];
        }
        return "~";
    }
}