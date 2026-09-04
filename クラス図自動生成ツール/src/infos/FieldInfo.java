package src.infos;

import java.util.*;

import src.draw.StyledLine;

public class FieldInfo{
    private final String kind;
    public final Set<String> modifiers;
    public final TypeInfo type, initializer;
    public final String name;
    public StyledLine line;

    public FieldInfo(String kind, Set<String> modifiers, TypeInfo type, String name, TypeInfo initializer){
        this.kind = kind;
        this.modifiers = modifiers;
        this.type = type;
        this.name = name;
        this.initializer = initializer;
        toStyledLine();
    }
    public void toStyledLine(){
        String text;

        text = accsessModifiers(modifiers) + " " + name + " : " + type.getRawName();
        if(initializer != null){
            text += " = " + decodeUnicode(initializer.getRawName());
        }
        line = new StyledLine(text, modifiers.contains("static"), modifiers.contains("final"), modifiers.contains("abstract"));
    }
    public TypeInfo getType(){
        return type;
    }
    private String accsessModifiers(Set<String> allMod){
        String[] access = {"public", "private", "protected", "+", "-", "#"};
        for(int i = 0; i < access.length / 2; i++){
            if(allMod.contains(access[i])) return access[i + 3];
        }
        return "~";
    }
    private String decodeUnicode(String init){
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while(i < init.length()){
            char c = init.charAt(i);
            if(c == '\\' && i + 5 < init.length() && init.charAt(i + 1) == 'u'){
                String hex = init.substring(i + 2, i + 6);
                try{
                    int code = Integer.parseInt(hex, 16);
                    sb.append((char) code);
                    i += 6;
                    continue;
                }catch(Exception e){}
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }
}