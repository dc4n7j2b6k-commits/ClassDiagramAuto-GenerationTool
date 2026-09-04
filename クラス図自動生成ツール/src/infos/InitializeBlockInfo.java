package src.infos;

import java.util.*;

public class InitializeBlockInfo{
    public List<TypeInfo> localTypes;
    public List<AssignmentInfo> ai;
    public String body;
    public boolean isStatic;

    public InitializeBlockInfo(List<TypeInfo> localTypes, List<AssignmentInfo> ai, String body, boolean isStatic){
        this.localTypes = localTypes;
        this.ai = ai;
        this.body = body;
        this.isStatic = isStatic;
    }
}