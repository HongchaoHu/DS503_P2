# Problem 1 — Spatial Join (35 Points)

## Overview

Spatial join is a common operation in applications managing multi-dimensional data. This problem joins two datasets:

- **Dataset P**: Set of 2D points
- **Dataset R**: Set of 2D rectangles

The spatial join reports any pair `(rectangle r, point p)` where point `p` is contained inside rectangle `r` (or on its boundary).

## Example Output

```
<r1, (3,15)>
<r2, (1,2)>
<r2, (2,4)>
<r3, (2,4)>
<r3, (4,3)>
<r5, (6,2)>
<r5, (7,7)>
<r6, (10,4)>
```

---

## Step 1 — Create the Datasets (10 Points)

### Requirements

**Coordinate Space:**
- Extends from **1 to 10,000** on both x and y axes
- All coordinates are **integers**

**Dataset P (Points):**
- Each line contains one point: `x,y`
- Must be **at least 100 MB** in size
- Points generated **randomly** with no specific order

**Dataset R (Rectangles):**
- Each line contains one rectangle: `bottomLeft_x,bottomLeft_y,height,width`
- Must be **at least 100 MB** in size
- Rectangle generation procedure:
  1. Randomly select a point as the bottom-left corner
  2. Generate height `h` uniformly between **[1, 20]**
  3. Generate width `w` uniformly between **[1, 7]**
  4. Representation: `<bottomLeft_x, bottomLeft_y, h, w>`

### Implementation

**File:** `mapreduce/src/DataGenerator.java`

**Usage:**
```bash
java -cp target/ds503-project2-1.0.0.jar edu.ds503.project2.DataGenerator \
  --points-out data/problem1/P.txt \
  --rects-out data/problem1/R.txt \
  --target-mb 100
```

---

## Step 2 — MapReduce Job for Spatial Join (25 Points)

### Requirements

**Input Parameters:**
- `inputPathP`: Path to dataset P (points)
- `inputPathR`: Path to dataset R (rectangles)
- `outputPath`: Path for output results
- `W(x1, y1, x2, y2)` *(optional)*: Spatial window (rectangle)

**Window Filtering:**
- If window `W` is provided:
  - Skip rectangles entirely outside `W`
  - Skip points outside `W`
- If `W` is not provided: perform join over entire datasets

**Example with Window:**
```
Window: W(1, 3, 3, 20)
Results:
<r1, (3,15)>
<r2, (2,4)>
<r3, (2,4)>
```

### Implementation Constraints

**✅ Required:**
- Use **one MapReduce job only**
- Job must include:
  - **Map phase**: Two map functions (one per dataset)
  - **Reduce phase**: 4–8 reducers recommended
- Multiple workers in both phases:
  - Multiple map tasks
  - Multiple reduce tasks

**❌ Penalties:**
- Using more than one MapReduce job: **−10 points per extra job**
- Assuming single map and single reduce (no distributed processing): **−15 points**

### Implementation

**File:** `mapreduce/src/SpatialJoinJob.java`

**Usage (No Window):**
```bash
docker exec -it namenode hadoop jar /opt/mapreduce/target/ds503-project2-1.0.0.jar \
  edu.ds503.project2.SpatialJoinJob \
  /project2/input/P.txt \
  /project2/input/R.txt \
  /project2/output/q1
```

**Usage (With Window):**
```bash
docker exec -it namenode hadoop jar /opt/mapreduce/target/ds503-project2-1.0.0.jar \
  edu.ds503.project2.SpatialJoinJob \
  /project2/input/P.txt \
  /project2/input/R.txt \
  /project2/output/q1_windowed \
  1 3 3 20
```

### Algorithm Approach

The implementation uses **grid-based spatial partitioning**:

1. **Map Phase:**
   - Divide space into grid cells
   - Each point emits to its cell
   - Each rectangle emits to all cells it intersects
   
2. **Reduce Phase:**
   - For each cell, test point-rectangle containment
   - Output pairs where point is inside rectangle

---

## Testing

**Test Files:** `data/q1test/`
- `P_small.txt`: Small point dataset (10 points)
- `R_small.txt`: Small rectangle dataset (10 rectangles)
- Expected outputs for validation

**Status:** ✅ **Fully Working**
