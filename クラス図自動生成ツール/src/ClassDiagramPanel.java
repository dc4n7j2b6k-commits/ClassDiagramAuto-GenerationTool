package src;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.font.TextAttribute;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.io.*;

import src.draw.*;

public class ClassDiagramPanel extends JPanel{
    private List<ClassBox> boxes;
    private List<ClassRelation> relations;
    private List<PackageFolder> folders;
    private Map<String, ClassBox> boxMap;
    private Point dragStart;
    private List<ClassBox> dragBoxs;
    private ClassRelation dragRel;
    private final int gridSize = 25;
    private int insertIndex = 0;
    private boolean ctrl = false;
    private boolean resizing = false;

    double scale = 1.0;
    double offsetX = 0, offsetY = 0;
    boolean isVisible = true;

    public ClassDiagramPanel(List<ClassBox> boxes, List<ClassRelation> relations, List<PackageFolder> folders){
        this.boxes = boxes;
        this.relations = relations;
        this.folders = folders;
        dragBoxs = new ArrayList<>();
        this.boxMap = boxes.stream().collect(Collectors.toMap(b -> b.className, b -> b));
        addMouseWheelListener(e -> {
            double delta = e.getPreciseWheelRotation();
            double factor = ctrl ? Math.pow(1.2, -delta) : Math.pow(1.1, -delta);
            scale *= factor;

            Point p = e.getPoint();
            offsetX = (offsetX - p.x) * factor + p.x;
            offsetY = (offsetY - p.y) * factor + p.y;
            repaint();
        });
        addMouseListener(new MouseAdapter(){
            public void mousePressed(MouseEvent e){
                Point p = toDiagramPoint(e.getPoint());
                relLabel:for(ClassRelation rel : relations){
                    for(int i = 0; i < rel.waypoints.size(); i++){
                        Point wp = rel.waypoints.get(i);
                        if(wp.distance(p) < 15){
                            dragRel = rel;
                            dragStart = p;
                            insertIndex = i;
                            break relLabel;
                        }
                    }
                }
                if(SwingUtilities.isRightMouseButton(e)){
                    if(dragStart == null){
                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem saveItem = new JMenuItem("PNGとして保存");
                        saveItem.addActionListener(ev -> {
                            JFileChooser chooser = new JFileChooser();
                            chooser.setSelectedFile(new File("クラス図.png"));
                            if(chooser.showSaveDialog(ClassDiagramPanel.this) == JFileChooser.APPROVE_OPTION){
                                try{
                                    BufferedImage img = exportAsImage();
                                    ImageIO.write(img, "png", chooser.getSelectedFile());
                                    JOptionPane.showMessageDialog(ClassDiagramPanel.this, "保存成功！");
                                }catch(IOException ex){
                                    JOptionPane.showMessageDialog(ClassDiagramPanel.this, "保存失敗：" + ex.getMessage());
                                }
                            }
                        });
                        menu.add(saveItem);
                        menu.show(ClassDiagramPanel.this, e.getX(), e.getY());
                    }else{
                        dragRel.waypoints.remove(insertIndex);
                    }
                }else{
                    dragStart = e.getPoint();
                    for(PackageFolder folder : folders){
                        if(folder.getTagName().contains(p)){
                            dragBoxs.addAll(folder.boxes);
                            dragStart = p;
                            resizing = false;
                            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                            break;
                        }
                    }
                    for(ClassBox b : boxes){
                        if(!isVisible) break;
                        if(b.getResizeHandle().contains(p)){
                            dragBoxs.add(b);
                            dragStart = p;
                            resizing = true;
                            setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                            break;
                        }else if(new Rectangle(b.x, b.y, b.width, b.height).contains(p)){
                            dragBoxs.add(b);
                            dragStart = p;
                            resizing = false;
                            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                            break;
                        }
                    }
                    ClassRelation closest = null;
                    double minDist = Double.MAX_VALUE;
                    for(ClassRelation rel : relations){
                        if(!isVisible) break;
                        Point prev = rel.start;
                        for(Point wp : rel.waypoints){
                            if(wp.distance(p) < 15) continue;
                            double dist = rel.distanceToSegment(p, prev, wp);
                            if(dist < minDist){
                                minDist = dist;
                                closest = rel;
                            }
                            prev = wp;
                        }
                        double dist = rel.distanceToSegment(p, prev, rel.end);
                        if(dist < minDist){
                            minDist = dist;
                            closest = rel;
                        }
                    }
                    if(closest != null && minDist < 20){
                        closest.addWaypoint(new Point(p));
                    }
                }
                repaint();
            }
            public void mouseReleased(MouseEvent e){
                dragBoxs.clear();
                dragRel = null;
                dragStart = null;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter(){
            public void mouseDragged(MouseEvent e){
                if(!isVisible || dragBoxs.isEmpty() && dragRel == null){
                    int dx = e.getX() - dragStart.x;
                    int dy = e.getY() - dragStart.y;
                    offsetX += dx;
                    offsetY += dy;
                    dragStart = e.getPoint();
                }else if(dragRel != null){
                    dragRel.waypoints.get(insertIndex).setLocation(toDiagramPoint(e.getPoint()));
                }else if(!dragBoxs.isEmpty()){
                    Point p = toDiagramPoint(e.getPoint());
                    if(resizing){
                        int dw = p.x - dragStart.x;
                        int dh = p.y - dragStart.y;
                        for(ClassBox box : dragBoxs){
                            box.width += dw;
                            box.height += dh;
                        }
                    }else{
                        int dx = p.x - dragStart.x;
                        int dy = p.y - dragStart.y;
                        for(ClassBox box : dragBoxs){
                            box.x += dx;
                            box.y += dy;
                        }
                    }
                    dragStart = p;
                }
                repaint();
            }
        });
    }

    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        Font baseFont = new Font("Monospaced", Font.PLAIN, 15);
        g2.translate(offsetX, offsetY);
        g2.scale(scale, scale);

        for(PackageFolder pkg : folders){
            pkg.update();
            pkg.draw(g2);
        }
        for(ClassBox box : boxes){
            box.draw(g2, baseFont, isVisible);
        }
        for(ClassRelation rel : relations){
            ClassBox fromBox = boxMap.get(rel.from);
            ClassBox toBox = boxMap.get(rel.to);
            if(fromBox == null || toBox == null) continue;
            rel.drawWaypoints(g2, isVisible);
            drawSmoothLine(g2, rel, fromBox, toBox);
        }
    }
    private void drawSmoothLine(Graphics2D g, ClassRelation rel, ClassBox from, ClassBox to){
        rel.start = getIntersection(from.getBounds(), to.getCenter(), from.getCenter());
        rel.end = getIntersection(to.getBounds(), from.getCenter(), to.getCenter());
        if(rel.waypoints.size() > 0){
            rel.start = getIntersection(from.getBounds(), rel.waypoints.get(0), from.getCenter());
            rel.end = getIntersection(to.getBounds(), rel.waypoints.get(rel.waypoints.size() - 1), to.getCenter());
        }
        switch(rel.getType()){
            case DEPENDENCY -> g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4f}, 0));
            case IMPLEMENTS -> g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6f}, 0));
            default -> g.setStroke(new BasicStroke(2f));
        }
        Point prev = rel.start;
        for(Point wp : rel.waypoints){
            g.drawLine(prev.x, prev.y, wp.x, wp.y);
            prev = wp;
        }
        g.drawLine(prev.x, prev.y, rel.end.x, rel.end.y);
        Point prevStart = rel.waypoints.isEmpty() ? rel.end : rel.waypoints.get(0);
        Point nextEnd = rel.waypoints.isEmpty() ? rel.start : rel.waypoints.get(rel.waypoints.size() - 1);
        switch(rel.type){
            case COMPOSITION -> drawDiamond(g, prevStart, rel.start, true);
            case AGGREGATION -> drawDiamond(g, prevStart, rel.start, false);
            case EXTENDS, IMPLEMENTS -> drawTriangle(g, nextEnd, rel.end, true);
            case DEPENDENCY -> drawArrow(g, nextEnd, rel.end);
        }
        rel.drawMultiplicity(g, nextEnd, rel.end);
    }
    public void drawArrow(Graphics2D g, Point from, Point to){
        double angle = Math.atan2(to.y - from.y, to.x - from.x);
        int len = 20;
        int x1 = to.x - (int)(len * Math.cos(angle - Math.PI / 6));
        int y1 = to.y - (int)(len * Math.sin(angle - Math.PI / 6));
        int x2 = to.x - (int)(len * Math.cos(angle + Math.PI / 6));
        int y2 = to.y - (int)(len * Math.sin(angle + Math.PI / 6));
        Polygon arrowHead = new Polygon();
        arrowHead.addPoint(to.x, to.y);
        arrowHead.addPoint(x1, y1);
        arrowHead.addPoint(x2, y2);
        g.setColor(Color.BLACK);
        g.fill(arrowHead);
    }
    public void drawDiamond(Graphics2D g, Point next, Point pos, boolean filled){
        double angle = Math.atan2(pos.y - next.y, pos.x - next.x);
        int size = 20;

        int x1 = pos.x, y1 = pos.y;
        int x2 = x1 - (int)(size * Math.cos(angle - Math.PI / 6));
        int y2 = y1 - (int)(size * Math.sin(angle - Math.PI / 6));
        int x3 = x1 - (int)(size * Math.cos(angle) * 1.73);
        int y3 = y1 - (int)(size * Math.sin(angle) * 1.73);
        int x4 = x1 - (int)(size * Math.cos(angle + Math.PI / 6));
        int y4 = y1 - (int)(size * Math.sin(angle + Math.PI / 6));
        Polygon diamond = new Polygon(
            new int[]{x1, x2, x3, x4},
            new int[]{y1, y2, y3, y4}, 4
        );
        g.setColor(Color.WHITE);
        g.fill(diamond);
        if(filled){
            g.setColor(Color.BLACK);
            g.fill(diamond);
        }else{
            g.setColor(Color.BLACK);
            g.draw(diamond);
        }
    }
    public void drawTriangle(Graphics2D g, Point from, Point to, boolean filled){
        double angle = Math.atan2(to.y - from.y, to.x - from.x);
        int len = 25;
        Polygon triangle = new Polygon();
        triangle.addPoint(to.x, to.y);
        for(int i = 0; i < 2; i++){
            triangle.addPoint(to.x - (int)(len * Math.cos(angle - 0.3 + i * 0.6)), to.y - (int)(len * Math.sin(angle - 0.3 + i * 0.6)));
        }
        g.setStroke(new BasicStroke(1f));
        g.setColor(Color.WHITE);
        g.fill(triangle);
        if(filled){
            g.setColor(Color.BLACK);
            g.draw(triangle);
        }else{
            g.setColor(Color.BLACK);
            g.draw(triangle);
        }
    }
    public Point getIntersection(Rectangle rect, Point from, Point to){
        Line2D line = new Line2D.Double(from, to);
        List<Line2D> edges = List.of(
            new Line2D.Double(rect.x, rect.y, rect.x + rect.width, rect.y),
            new Line2D.Double(rect.x, rect.y, rect.x, rect.y + rect.height),
            new Line2D.Double(rect.x + rect.width, rect.y, rect.x + rect.width, rect.y + rect.height),
            new Line2D.Double(rect.x, rect.y + rect.height, rect.x + rect.width, rect.y + rect.height)
        );
        for(Line2D edge : edges){
            if(edge.intersectsLine(line)){
                return getIntersectionPoint(line, edge);
            }
        }
        return to;
    }
    private Point getIntersectionPoint(Line2D l1, Line2D l2){
        double x1 = l1.getX1(), y1 = l1.getY1();
        double x2 = l1.getX2(), y2 = l1.getY2();
        double x3 = l2.getX1(), y3 = l2.getY1();
        double x4 = l2.getX2(), y4 = l2.getY2();
        double denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if(denom == 0) return null;
        double px = ((x1*y2 - y1*x2)*(x3 - x4) - (x1 - x2)*(x3*y4 - y3*x4)) / denom;
        double py = ((x1*y2 - y1*x2)*(y3 - y4) - (y1 - y2)*(x3*y4 - y3*x4)) / denom;
        return new Point((int) px, (int) py);
    }
    public Point toDiagramPoint(Point point){
        int x = (int)((point.getX() - offsetX) / scale);
        int y = (int)((point.getY() - offsetY) / scale);
        return new Point(x, y);
    }
    public int getOffsetForRelation(RelationType type){
        return switch(type){
            case COMPOSITION -> 0;
            case AGGREGATION -> 0;
            case ASSOCIATION -> 0;
            case DEPENDENCY -> 0;
            case IMPLEMENTS -> 0;
            case EXTENDS -> 0;
            default -> 0;
        };
    }
    public BufferedImage exportAsImage(){
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        paintComponent(g2);
        g2.dispose();
        return image;
    }
}