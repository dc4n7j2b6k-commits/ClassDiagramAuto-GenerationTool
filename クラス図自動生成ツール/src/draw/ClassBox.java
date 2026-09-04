package src.draw;

import java.awt.*;
import java.awt.font.*;
import java.util.*;
import java.util.List;
import java.text.*;

import src.infos.*;

public class ClassBox{
    public final StyledLine line;
    public String className, simpleName, type;
    public int x, y, width, height;
    public int padding = 10;
    public int maxWidth, lineHeight, currentY;
    public boolean init = false, isJavaAPI;
    public List<String> enumConstants = new ArrayList<>();
    public List<FieldInfo> fields = new ArrayList<>();
    public List<ConstructorInfo> constructors = new ArrayList<>();
    public List<MethodInfo> methods = new ArrayList<>();
    public Font baseFont;

    public ClassBox(StyledLine line, String className, String simpleName, String type, boolean isJavaAPI, int x, int y, int w, int h){
        this.line = line;
        this.className = className;
        this.simpleName = simpleName;
        this.type = type;
        this.isJavaAPI = isJavaAPI;
        this.x = x; this.y = y; this.width = w; this.height = h;
    }
    public void draw(Graphics2D g, Font baseFont, boolean isVisible){
        this.baseFont = baseFont;
        g.setFont(baseFont);
        currentY = y + padding;
        g.setStroke(new BasicStroke(2f));
        g.setColor(Color.BLACK);
        maxWidth = Math.max(maxWidth, g.getFontMetrics().stringWidth(className)) * 3;
        lineHeight = g.getFontMetrics().getHeight() * 2 / 3;
        currentY += lineHeight;
        if(type.equals("interface")){
            drawKind(g, null, "<<Interface>>", baseFont);
            currentY += lineHeight / 2;
        }else if(type.equals("enum")){
            drawKind(g, null, "<<enumeration>>", baseFont);
            currentY += lineHeight / 2;
        }
        drawKind(g, line, simpleName, new Font("Monospaced", Font.BOLD, 30));
        if(!isJavaAPI){
            g.drawLine(x, currentY, x + width, currentY);
            currentY += lineHeight;
            if(!enumConstants.isEmpty()){
                for(String con : enumConstants){
                    g.drawString(con, x + padding, currentY);
                    currentY += lineHeight * 2;
                }
                g.drawLine(x, currentY - lineHeight / 2, x + width, currentY - lineHeight / 2);
                currentY += lineHeight;
            }
            for(FieldInfo fi : fields){
                drawStyledText(g, fi.line, baseFont, x + padding, currentY);
                currentY += lineHeight;
            }
            g.drawLine(x, currentY - lineHeight / 2, x + width, currentY - lineHeight / 2);
            currentY += lineHeight;
            if(!type.equals("enum")){
                for(ConstructorInfo ci : constructors){
                    if(ci.parentPathClass.isEmpty()) continue;
                    drawStyledText(g, ci.line, baseFont, x + padding, currentY);
                    currentY += lineHeight;
                }
                g.drawLine(x, currentY - lineHeight / 2, x + width, currentY - lineHeight / 2);
                currentY += lineHeight;
            }
            for(MethodInfo mi : methods){
                if(mi.parentPathClass.isEmpty()) continue;
                drawStyledText(g, mi.line, baseFont, x + padding, currentY);
                currentY += lineHeight;
            }
        }
        if(!init) width = maxWidth;
        if(!init) height = currentY + lineHeight - y;
        g.drawRect(x, y, width, height);
        if(isVisible) g.fillRect(x + width - 10, y + height - 10, 10, 10);
        init = true;
    }
    private void drawStyledText(Graphics2D g, StyledLine line, Font baseFont, int x, int y){
        int style = Font.PLAIN;
        String text = line.text;
        if(line.isFinal) text += " {final}";
        if(line.isAbstract) style |= Font.ITALIC;
        Font styledFont = baseFont.deriveFont(style);
        FontMetrics fm = g.getFontMetrics();
        if(!init) maxWidth = Math.max(maxWidth, fm.stringWidth(text));
        AttributedString as = new AttributedString(text);
        as.addAttribute(TextAttribute.FONT, styledFont);
        if(line.isStatic){
            as.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        }
        g.drawString(as.getIterator(), x, currentY);
        currentY += lineHeight;
    }
    public void drawKind(Graphics2D g, StyledLine line, String kind, Font font){
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int classWidth = 0;
        if(line == null){
            classWidth = fm.stringWidth(kind);
            g.drawString(kind, x + (width - classWidth) / 2, currentY);
        }else{
            int style = Font.PLAIN;
            String text = line.text;
            if(line.isFinal) text += " {final}";
            if(line.isAbstract) style |= Font.ITALIC;
            classWidth = fm.stringWidth(text);
            Font styledFont = font.deriveFont(style);
            if(!init) maxWidth = Math.max(maxWidth, fm.stringWidth(text));
            AttributedString as = new AttributedString(text);
            as.addAttribute(TextAttribute.FONT, styledFont);
            if(line.isStatic){
                as.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            }
            g.drawString(as.getIterator(), x + (width - classWidth) / 2, currentY);
        }
        g.setFont(baseFont);
        if(!init) width = Math.max(maxWidth, classWidth * 3);
        currentY += lineHeight;
    }
    public Point getCenter(){
        return new Point(x + width / 2, y + height / 2);
    }
    public Rectangle getBounds(){
        return new Rectangle(x, y, width, height);
    }
    public boolean contains(Point point){
        return new Rectangle(x, y, width, height).contains(point);
    }
    public Rectangle getResizeHandle(){
        return new Rectangle(x + width - 10, y + height - 10, 10, 10);
    }
    public String toString(){
        return className + ":" + type;
    }
}