package src.draw;

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class ClassRelation{
    public final String from, to;
    public final RelationType type;
    public String fromMultiplicity = " ";
    public String toMultiplicity = " ";
    public Point start, end;
    public List<Point> waypoints = new ArrayList<>();

    public ClassRelation(String from, String to, RelationType type){
        this.from = from;
        this.to = to;
        this.type = type;
    }
    public void addWaypoint(Point p){
        int insertIndex = 0;
        double minDist = Double.MAX_VALUE;
        Point prev = start;
        for(int i = 0; i <= waypoints.size(); i++){
            Point next = (i < waypoints.size()) ? waypoints.get(i) : end;
            double dist = distanceToSegment(p, prev, next);
            if(dist < minDist){
                minDist = dist;
                insertIndex = i;
            }
            prev = next;
        }
        if(minDist > 15) return;
        waypoints.add(insertIndex, p);
    }
    public double distanceToSegment(Point p, Point a, Point b){
        if(a == null || b == null) return 0;
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        if(dx == 0 && dy == 0) return p.distance(a);
        double t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        double projX = a.x + t * dx;
        double projY = a.y + t * dy;
        return p.distance(projX, projY);
    }
    public RelationType getType(){
        return type;
    }
    public List<Point> getWaypoints(){
        return waypoints;
    }
    public void drawWaypoints(Graphics2D g2d, boolean isVisible){
        if(!isVisible) return;
        for(Point wd : waypoints){
            g2d.fillOval(wd.x - 10, wd.y - 10, 20, 20);
        }
    }
    public void drawMultiplicity(Graphics2D g2d, Point from, Point to){
        double angle = Math.atan2(to.y - from.y, to.x - from.x);
        Font old = g2d.getFont();
        Font f = new Font("SansSerif", Font.BOLD, 30);
        FontMetrics font = g2d.getFontMetrics(f);
        float text_x = font.stringWidth(toMultiplicity);
        float text_y = font.getHeight();

        int x = to.x - (int)(text_x * 2 * Math.cos(angle - Math.PI / 4));
        int y = to.y - (int)(text_x * 2 * Math.sin(angle - Math.PI / 4));

        float fx = (float)x - text_x / 2;
        float fy = (float)y + font.getAscent() - text_y / 2;

        g2d.setFont(f);
        GlyphVector gv = f.createGlyphVector(g2d.getFontRenderContext(), toMultiplicity);
        g2d.fill(gv.getOutline(fx, fy));
        g2d.setFont(old);
    }
}