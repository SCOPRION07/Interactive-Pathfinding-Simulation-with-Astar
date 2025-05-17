import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

class Node {
    private int row, col;
    private Color color;
    private List<Node> neighbors;
    private int gCost;
    private int hCost;
    private int fCost;
    private Node previous;

    public Node(int row, int col) {
        this.row = row;
        this.col = col;
        this.color = Color.WHITE;
        this.neighbors = new ArrayList<>();
        reset();
    }

    public void reset() {
        color = Color.WHITE;
        gCost = Integer.MAX_VALUE;
        hCost = Integer.MAX_VALUE;
        fCost = Integer.MAX_VALUE;
        previous = null;
    }

    public void makeStart() { color = Color.BLUE; }
    public void makeEnd() { color = Color.YELLOW; }
    public void makeObstacle() { color = Color.BLACK; }
    public void makeOpen() { color = Color.GREEN; }
    public void makeClosed() { color = Color.RED; }
    public void makePath() { color = Color.MAGENTA; }


    public boolean isObstacle() { return color == Color.BLACK; }


    public void updateNeighbors(Node[][] grid) {
        neighbors.clear();
        if (row < grid.length - 1 && !grid[row + 1][col].isObstacle()) neighbors.add(grid[row + 1][col]);
        if (row > 0 && !grid[row - 1][col].isObstacle()) neighbors.add(grid[row - 1][col]);
        if (col < grid[0].length - 1 && !grid[row][col + 1].isObstacle()) neighbors.add(grid[row][col + 1]);
        if (col > 0 && !grid[row][col - 1].isObstacle()) neighbors.add(grid[row][col - 1]);
    }

    public Point getPos() { return new Point(row, col); }
    public List<Node> getNeighbors() { return neighbors; }
    public Color getColor() { return color; }

    public int getFCost() { return fCost; }
    public void setFCost(int f) { fCost = f; }

}

class GridPanel extends JPanel {
    private static final int WIDTH = 800, HEIGHT = 800, ROWS = 50;
    private final int GRID_SIZE = WIDTH / ROWS;
    private Node[][] grid;
    private Node start, end;
    private boolean running = false;

    public GridPanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        initializeGrid();
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (running) return;
                Node node = getNode(e.getPoint());
                if (node == null) return;

                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (start == null && node != end) {
                        start = node;
                        node.makeStart();
                    } else if (end == null && node != start) {
                        end = node;
                        node.makeEnd();
                    } else if (node != start && node != end) {
                        node.makeObstacle();
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    node.reset();
                    if (node == start) start = null;
                    else if (node == end) end = null;
                }
                repaint();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (running || SwingUtilities.isRightMouseButton(e)) return;
                Node node = getNode(e.getPoint());
                if (node != null && node != start && node != end) {
                    node.makeObstacle();
                    repaint();
                }
            }
        });

        setupKeyBindings();
    }

    private void setupKeyBindings() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke("SPACE"), "start");
        am.put("start", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!running && start != null && end != null) {
                    running = true;
                    new Thread(() -> new Astar(grid, start, end, GridPanel.this).run()).start();
                }
            }
        });

        im.put(KeyStroke.getKeyStroke("C"), "clear");
        am.put("clear", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (running) return;
                start = null;
                end = null;
                initializeGrid();
                repaint();
            }
        });
    }

    private void initializeGrid() {
        grid = new Node[ROWS][ROWS];
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < ROWS; j++) {
                grid[i][j] = new Node(i, j);
            }
        }
    }

    private Node getNode(Point p) {
        int row = p.x / GRID_SIZE;
        int col = p.y / GRID_SIZE;
        if (row >= 0 && row < ROWS && col >= 0 && col < ROWS) {
            return grid[row][col];
        }
        return null;
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < ROWS; j++) {
                Node node = grid[i][j];
                g.setColor(node.getColor());
                g.fillRect(i * GRID_SIZE, j * GRID_SIZE, GRID_SIZE, GRID_SIZE);
                g.setColor(Color.BLACK);
                g.drawRect(i * GRID_SIZE, j * GRID_SIZE, GRID_SIZE, GRID_SIZE);
            }
        }
    }

    public void setRunning(boolean running) { this.running = running; }
}

class Astar implements Runnable {
    private Node[][] grid;
    private Node start, end;
    private GridPanel panel;
    private int count = 0;

    public Astar(Node[][] grid, Node start, Node end, GridPanel panel) {
        this.grid = grid;
        this.start = start;
        this.end = end;
        this.panel = panel;
    }

    public void run() {
        Map<Node, Node> cameFrom = new HashMap<>();
        Map<Node, Integer> gScore = new HashMap<>();
        PriorityQueue<NodeWithPriority> openSet = new PriorityQueue<>(
                Comparator.comparingInt((NodeWithPriority nwp) -> nwp.fScore)
                        .thenComparingInt(nwp -> nwp.count)
        );
        Set<Node> openSetHash = new HashSet<>();

        for (Node[] row : grid) {
            for (Node node : row) {
                gScore.put(node, Integer.MAX_VALUE);
            }
        }
        gScore.put(start, 0);
        start.setFCost(heuristic(start.getPos(), end.getPos()));
        openSet.add(new NodeWithPriority(start.getFCost(), count++, start));
        openSetHash.add(start);
        start.makeOpen();

        SwingUtilities.invokeLater(panel::repaint);

        while (!openSet.isEmpty()) {
            NodeWithPriority currentNWP = openSet.poll();
            Node current = currentNWP.node;
            openSetHash.remove(current);

            if (current == end) {
                reconstructPath(cameFrom, current);
                end.makeEnd();
                panel.setRunning(false);
                return;
            }

            current.updateNeighbors(grid);
            for (Node neighbor : current.getNeighbors()) {
                int tempG = gScore.get(current) + 1;

                if (tempG < gScore.get(neighbor)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tempG);
                    int f = tempG + heuristic(neighbor.getPos(), end.getPos());
                    neighbor.setFCost(f);

                    if (!openSetHash.contains(neighbor)) {
                        openSet.add(new NodeWithPriority(f, count++, neighbor));
                        openSetHash.add(neighbor);
                        neighbor.makeOpen();
                    }
                }
            }

            current.makeClosed();
            SwingUtilities.invokeLater(panel::repaint);

            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        panel.setRunning(false);
    }

    private int heuristic(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    private void reconstructPath(Map<Node, Node> cameFrom, Node current) {
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            if (current != start) {
                current.makePath();
                SwingUtilities.invokeLater(panel::repaint);
                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class NodeWithPriority {
        int fScore, count;
        Node node;

        NodeWithPriority(int f, int c, Node n) {
            fScore = f;
            count = c;
            node = n;
        }
    }
}

public class AStar {
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