package src.infos;

public class AssignmentInfo{
    public String fieldName;
    public String exprName;
    public String sourceType;
    public AssignmentInfo(String fieldName, String exprName, String sourceType){
        this.fieldName = fieldName;
        this.exprName = exprName;
        this.sourceType = sourceType;
    }
    public boolean isNewInstance(){
        return "NEW".equals(sourceType);
    }
    public boolean isFromParameter(){
        return "PARAM".equals(sourceType);
    }
    public boolean isByReference(){
        return "REFERENCE".equals(sourceType);
    }
    @Override
    public String toString(){
        return String.format("AssignmentInfo[fieldName=%s exprName=%s sourceType=%s]", fieldName, exprName, sourceType);
    }
}