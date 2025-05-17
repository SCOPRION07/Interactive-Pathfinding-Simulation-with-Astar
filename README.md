# Interactive-Pathfinding-Simulation-with-A*
An interactive Java Swing-based simulation that visually demonstrates the Bidirectional A* pathfinding algorithm. Users can set start/end points, draw obstacles, and watch the algorithm find the shortest path in real time across a grid. This project is designed for students, developers, and anyone interested in understanding advanced pathfinding and heuristic search methods.

📌**Features**

**Interactive Grid:** 50×50 grid where users can set start/end points and place obstacles using the mouse.

**Real-Time Visualization**: Watch the Bidirectional A* algorithm expand from both start and end points simultaneously.

**Color-Coded States:**

White: Unexplored

Blue: Start

Yellow: End

Green: Open nodes

Red: Closed nodes

Purple: Final path

Black: Obstacles

**Reset & Replay:** Instantly clear the grid and rerun the algorithm with different scenarios.

**Educational Value:** Ideal for learning and teaching concepts of heuristic search, bidirectional search, and AI navigation.

⚙️**How It Works**

**Grid Setup:**
The application displays a 50×50 grid. Each cell can be a start, end, obstacle, or free node.

**User Interaction:**

**Left-click:** Set start/end points and draw obstacles.

**Right-click:** Reset individual cells.

**Drag:** Quickly add multiple obstacles.

**Bidirectional A-star Algorithm:**

Two priority queues expand from the start and end nodes.

Each node tracks G-cost (distance), H-cost (heuristic, Manhattan distance), and F-cost (G + H).

The algorithm expands the lowest-cost nodes in both directions until the search frontiers meet.

The shortest path is reconstructed and highlighted.

**Visualization:**

The grid updates in real time, showing the algorithm’s decision-making process and final path.
