package src.draw;

import java.awt.*;
import java.util.*;
import java.util.List;

import src.infos.*;

public class PackageFolder{
    public String packageName;
    public List<ClassInfo> classes = new ArrayList<>();
    public List<ClassBox> boxes = new ArrayList<>();
    private int textX = 0, textY = 0;
    private int padding = 10;
    private Rectangle bounds;

    public PackageFolder(String packageName){
        this.packageName = packageName;
        bounds = new Rectangle(0, 0, 0, 0);
    }
    public void update(){
        if(boxes.isEmpty()) return;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

        for(ClassBox box : boxes){
            minX = Math.min(minX, box.x);
            minY = Math.min(minY, box.y);
            maxX = Math.max(maxX, box.x + box.width);
            maxY = Math.max(maxY, box.y + box.height);
        }
        bounds.setBounds(
            minX - padding, minY - padding, (maxX - minX) + 2 * padding, (maxY - minY) + 2 * padding
        );
    }
    public void draw(Graphics2D g){
        if(boxes.isEmpty()) return;

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(5f));
        g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);

        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(packageName);
        textX = bounds.x + padding;
        textY = bounds.y + fm.getAscent() - 65;
        g.drawPolygon(getTagName());
        g.setFont(new Font("Serif", Font.BOLD, 50));
        g.drawString(packageName, textX, textY);
    }
    public Polygon getTagName(){
        return new Polygon(
            new int[]{bounds.x, bounds.x, bounds.x + bounds.width * 2 / 5, textX + bounds.width / 2},
            new int[]{bounds.y, textY - 60, textY - 60, bounds.y}, 4
        );
    }
}