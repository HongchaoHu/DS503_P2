# DS503 Project 2

**Student:** Hongchao Hu  
**Date:** March 8, 2026

---

## Assumptions

1. Coordinate space is [1, 10000] for both x and y coordinates.
2. Rectangles are represented by bottom-left corner (x, y), height (h), and width (w).
3. Points on rectangle boundaries are considered inside the rectangle.
4. Q1 uses grid cell size of 200x200 for partitioning.
5. Q2 uses cell size equal to radius (r) for neighbor detection.
6. Q3 runs maximum 6 iterations and uses epsilon 1e-6 for convergence.
7. Malformed input records are skipped.

---

## Implementation Status

| Question | Status | Comment |
|----------|--------|---------|
| Q1 | Fully Working | Spatial join using grid-based partitioning. Tested successfully with and without window filtering. |
| Q2 | Fully Working | Outlier detection using segmented 3x3 cell approach. Tested with r=20, k=5 on 107K points. |
| Q3 | Fully Working | K-Means clustering with combiner optimization. Tested with K=5, completed 6 iterations. |

---

## Code Comments

**Q1 - Spatial Join:**
- Uses MultipleInputs for dual input (points and rectangles)
- Grid-based partitioning distributes work across reducers
- Supports optional window filtering

**Q2 - Outlier Detection:**
- Uses 3x3 cell neighborhood to find neighbors within radius
- Each point sent to home cell and 8 adjacent cells
- Uses squared distance calculation for efficiency

**Q3 - K-Means:**
- Iterative MapReduce with combiner for optimization
- Single reducer ensures consistent center updates
- Stops after 6 iterations or when centers converge
