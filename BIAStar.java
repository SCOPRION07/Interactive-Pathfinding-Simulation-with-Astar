import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

// --- Node class with forward/backward costs and identities ---
class Node {
    private final int row, col;
    private Color color;
    private final List<Node> neighbors;
    private int gForward, gBackward;
    private int fForward, fBackward;
    private Node prevForward, prevBackward;

    public Node(int row, int col) {
        this.row = row;
        this.col = col;
        this.color = Color.WHITE;
        this.neighbors = new ArrayList<>();
        reset();
    }

    public void reset() {
        color = Color.WHITE;
        gForward = gBackward = Integer.MAX_VALUE;
        fForward = fBackward = Integer.MAX_VALUE;
        prevForward = prevBackward = null;
    }

    public void makeStart() { color = Color.BLUE; }
    public void makeEnd()   { color = Color.YELLOW; }
    public void makeObstacle() { color = Color.BLACK; }
    public void makeOpenForward()  { color = Color.GREEN; }
    public void makeOpenBackward() { color = new Color(0,150,200); }
    public void makeClosed()   { color = Color.RED; }
    public void makePath()     { color = Color.MAGENTA; }

    public boolean isObstacle() { return color.equals(Color.BLACK); }

    public void updateNeighbors(Node[][] grid) {
        neighbors.clear();
        int max = grid.length;
        if (row < max-1 && !grid[row+1][col].isObstacle()) neighbors.add(grid[row+1][col]);
        if (row > 0      && !grid[row-1][col].isObstacle()) neighbors.add(grid[row-1][col]);
        if (col < max-1  && !grid[row][col+1].isObstacle()) neighbors.add(grid[row][col+1]);
        if (col > 0      && !grid[row][col-1].isObstacle()) neighbors.add(grid[row][col-1]);
    }

    public Point getPos() { return new Point(row, col); }
    public List<Node> getNeighbors() { return neighbors; }
    public Color getColor() { return color; }
    public int getRow() { return row; }
    public int getCol() { return col; }

    // forward/backward getters and setters
    public int getGForward() { return gForward; }
    public void setGForward(int v) { gForward = v; }
    public int getGBackward() { return gBackward; }
    public void setGBackward(int v) { gBackward = v; }
    public int getFForward() { return fForward; }
    public void setFForward(int v) { fForward = v; }
    public int getFBackward() { return fBackward; }
    public void setFBackward(int v) { fBackward = v; }
    public Node getPrevForward() { return prevForward; }
    public void setPrevForward(Node n) { prevForward = n; }
    public Node getPrevBackward() { return prevBackward; }
    public void setPrevBackward(Node n) { prevBackward = n; }

//    @Override public boolean equals(Object o) {
//        if (this==o)
//            return true;
//        if (!(o instanceof Node))
//            return false;
//        Node n=(Node)o;
//        return row==n.row && col==n.col;
//    }
//    @Override public int hashCode() { return Objects.hash(row,col); }
}

class GridPanel extends JPanel {
    private static final int WIDTH = 800, HEIGHT = 800, ROWS = 50;
    private final int GRID_SIZE = WIDTH/ROWS;
    Node[][] grid;
    Node start, end;
    private boolean running = false;

    public GridPanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        initializeGrid();
        addMouseListener(new MouseAdapter(){ public void mousePressed(MouseEvent e){
            if (running) return;
            Node node = getNode(e.getPoint());
            if (node==null) return;
            if (SwingUtilities.isLeftMouseButton(e)){
                if (start==null && node!=end){
                    start=node; node.makeStart(); }
                else if (end==null && node!=start){
                    end=node; node.makeEnd();
                }
                else if (node!=start && node!=end){ node.makeObstacle(); }
            } else if (SwingUtilities.isRightMouseButton(e)){
                node.reset(); if (node==start) start=null; else if(node==end) end=null;
            }
            repaint(); }});

        addMouseMotionListener(new MouseAdapter(){ public void mouseDragged(MouseEvent e){
            if (running||SwingUtilities.isRightMouseButton(e)) return;
            Node node=getNode(e.getPoint());
            if(node!=null&&node!=start&&node!=end){
                node.makeObstacle();
                repaint();
            }
        }});
        setupKeyBindings();
    }
    private void setupKeyBindings(){
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke("SPACE"), "start");
        am.put("start", new AbstractAction(){ public void actionPerformed(ActionEvent e){
            if (!running && start!=null && end!=null) {
                running=true;
                new Thread(() -> new AStar(grid,start,end,GridPanel.this).run()).start();
            }
        }});
        im.put(KeyStroke.getKeyStroke("C"), "clear");
        am.put("clear", new AbstractAction(){
            public void actionPerformed(ActionEvent e){
            if (running) return; start=null;
            end=null;
            initializeGrid();
            repaint();
            }
        }
        );
    }
    private void initializeGrid(){ grid=new Node[ROWS][ROWS];
        for(int i=0;i<ROWS;i++) for(int j=0;j<ROWS;j++) grid[i][j]=new Node(i,j);
    }
    private Node getNode(Point p){
        int row=p.x/GRID_SIZE, col=p.y/GRID_SIZE;
        if(row>=0&&row<ROWS&&col>=0&&col<ROWS)
            return grid[row][col];
        return null;
    }
    protected void paintComponent(Graphics g){ super.paintComponent(g);
        for(int i=0;i<ROWS;i++) for(int j=0;j<ROWS;j++){
            Node n=grid[i][j];
            g.setColor(n.getColor());
            g.fillRect(i*GRID_SIZE,j*GRID_SIZE,GRID_SIZE,GRID_SIZE);
            g.setColor(Color.BLACK);
            g.drawRect(i*GRID_SIZE,j*GRID_SIZE,GRID_SIZE,GRID_SIZE);
        }
    }
    public void setRunning(boolean r){ running=r; }
}

// Bidirectional A* implemented inside AStar class
class AStar implements Runnable {
    private final Node[][] grid;
    private final Node start, end;
    private final GridPanel panel;
    private int counter=0;

    public AStar(Node[][] grid, Node start, Node end, GridPanel panel){
        this.grid=grid; this.start=start; this.end=end; this.panel=panel;
    }
    public void run(){
        PriorityQueue<Entry> openF = new PriorityQueue<>();
        PriorityQueue<Entry> openB = new PriorityQueue<>();
        Set<Node> closedF = new HashSet<>();
        Set<Node> closedB = new HashSet<>();

        start.setGForward(0);
        start.setFForward(heuristic(start,end));
        end.setGBackward(0);
        end.setFBackward(heuristic(end,start));

        openF.add(new Entry(start.getFForward(),counter++,start,true));
        openB.add(new Entry(end.getFBackward(),counter++,end,false));
        start.makeOpenForward(); end.makeOpenBackward();
        SwingUtilities.invokeLater(panel::repaint);

        Node meet=null;
        while(!openF.isEmpty() && !openB.isEmpty()){
            meet = expand(openF,closedF,closedB,true);
            if(meet!=null) break;
            meet = expand(openB,closedB,closedF,false);
            if(meet!=null) break;
        }
        if(meet!=null) reconstruct(meet);
        panel.setRunning(false);
    }
    private Node expand(PriorityQueue<Entry> open, Set<Node> closedT, Set<Node> closedO, boolean forward){
        while(!open.isEmpty()){
            Entry e=open.poll(); Node cur=e.node;
            if(closedT.contains(cur)) continue;
            closedT.add(cur);
            if(closedO.contains(cur)) return cur;
            cur.updateNeighbors(grid);
            for(Node nb:cur.getNeighbors()){
                if(closedT.contains(nb)) continue;
                int tg = (forward?cur.getGForward():cur.getGBackward())+1;
                if(tg < (forward?nb.getGForward():nb.getGBackward())){
                    if(forward) nb.setGForward(tg); else nb.setGBackward(tg);
                    int f = tg + heuristic(nb, forward?end:start);
                    if(forward) nb.setFForward(f); else nb.setFBackward(f);
                    if(forward) nb.setPrevForward(cur); else nb.setPrevBackward(cur);
                    open.add(new Entry(f,counter++,nb,forward));
                    if(forward) nb.makeOpenForward(); else nb.makeOpenBackward();
                }
            }
            cur.makeClosed(); SwingUtilities.invokeLater(panel::repaint); sleep();
            break;
        }
        return null;
    }
    private void reconstruct(Node meet){
        Node cur=meet;
        while(cur!=null){
            cur.makePath(); cur=cur.getPrevForward();
        }
        cur=meet.getPrevBackward();
        while(cur!=null){
            cur.makePath(); cur=cur.getPrevBackward();
        }
        SwingUtilities.invokeLater(panel::repaint);
    }
    private int heuristic(Node a, Node b){
        return Math.abs(a.getRow()-b.getRow())+Math.abs(a.getCol()-b.getCol());
    }
    private void sleep(){
        try{ Thread.sleep(25);}
        catch(InterruptedException ignored){}
    }

    private static class Entry implements Comparable<Entry>{
        final int f; final int id; final Node node; final boolean fwd;
        Entry(int f,int id,Node n,boolean fwd){
            this.f=f;this.id=id;this.node=n;this.fwd=fwd;
        }
        @Override public int compareTo(Entry e){
            if(f!=e.f) return f-e.f;
            return id-e.id;
        }
    }
}

public class BIAStar{
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Pathfinding Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new GridPanel());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}