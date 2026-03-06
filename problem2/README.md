# Problem 2 — Distance-Based Outlier Detection (35 Points)

## Overview

Outliers are objects that do not conform to the general behavior of other data points. This problem implements **distance-based outlier detection**.

### Definitions

**Given Parameters:**
- `r`: radius (distance threshold)
- `k`: minimum neighbor threshold

**Outlier:**
- A point `p` is an **outlier** if fewer than `k` neighbors exist within a circle centered at `p` with radius `r`

**Inlier:**
- A point `p` is an **inlier** if the number of neighbors within radius `r` is ≥ `k`

---

## Step 1 — Dataset

**Use Dataset P from Problem 1**

**Properties:**
- Space boundaries: **(1,1) to (10,000, 10,000)**
- Points in HDFS are **randomly distributed**
- Points may appear in **any block**

**Example:**
Points `(3,15)`, `(10,4)`, `(4,3)` might appear in the same HDFS block (no spatial ordering guaranteed)

---

## Step 2 — Reporting Outliers (35 Points)

### Requirements

**Input Parameters:**
- `inputPath`: Path to dataset P
- `outputPath`: Path for output results
- `r`: **radius** (mandatory)
- `k`: **threshold** (mandatory)

**Parameter Validation:**
- If either `r` or `k` is missing: **report an error**

### Implementation Constraints

**✅ Required:**
- Must use **one MapReduce job**
- Multiple mappers and reducers

**❌ Penalties:**
- Using more than one job: **−10 points per extra job**
- Assuming single map and reduce: **−15 points**

### Algorithm Hint

**Spatial Partitioning Strategy:**
- Divide the space into **small segments/cells**
- Each segment should be processed independently
- The algorithm determines whether point `p` is an outlier using only points in its segment (and neighboring segments)

**Suggested Approach:**
1. **Map Phase:**
   - Assign each point to a grid cell
   - Emit point to its cell and neighboring cells (within radius `r`)
   
2. **Reduce Phase:**
   - For each cell, count neighbors within radius `r` for each point
   - If neighbor count < `k`: emit as outlier
   - If neighbor count ≥ `k`: emit as inlier (or skip)

---

## Implementation

**File:** `mapreduce/src/OutlierJob.java`

**Usage:**
```bash
docker exec -it namenode hadoop jar /opt/mapreduce/target/ds503-project2-1.0.0.jar \
  edu.ds503.project2.OutlierJob \
  /project2/input/P.txt \
  /project2/output/q2 \
  20 5
```

**Parameters:**
- Input: `/project2/input/P.txt`
- Output: `/project2/output/q2`
- Radius `r = 20`
- Threshold `k = 5`

**Output Format:**
Each line contains one outlier point:
```
x,y
```

---

## Testing

**Test Strategy:**
1. Generate dataset P using DataGenerator
2. Run with different `r` and `k` values
3. Validate results by sampling and manual verification

**Status:** 🚧 **To Be Implemented**
