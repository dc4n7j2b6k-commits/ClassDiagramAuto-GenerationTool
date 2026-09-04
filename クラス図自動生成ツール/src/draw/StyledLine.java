package src.draw;

import java.awt.*;

public class StyledLine{
    public String text;
    public boolean isStatic, isFinal, isAbstract;
    public StyledLine(String text, boolean isStatic, boolean isFinal, boolean isAbstract){
        this.text = text;
        this.isStatic = isStatic;
        this.isFinal = isFinal;
        this.isAbstract = isAbstract;
    }
}