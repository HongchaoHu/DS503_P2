# Problem 3 — K-Means Clustering (30 Points)

## Overview

K-Means clustering groups similar objects into **K clusters**.

### Algorithm

1. Start with **K randomly chosen seed points** as cluster centers
2. **Iteratively** assign points to nearest center and update centers
3. Stop when:
   - Two consecutive iterations produce **identical centers**, OR
   - **Maximum of 6 iterations** reached

**Reference:** [K-Means Standard Algorithm](http://en.wikipedia.org/wiki/K-means_clustering#Standard_algorithm)

---

## Step 1 — Create Dataset

### Dataset P (Points)

**Use Dataset P from Problem 1**

**Properties:**
- Space boundaries: **(1,1) to (10,000, 10,000)**
- Points are **randomly ordered**

### Seeds File

**Requirements:**
- Contains **K initial seed points** (cluster centers)
- `K` is a **parameter to your program**
- Seeds should be **generated randomly**
- Upload seed file to HDFS

**Format:**
Each line contains one seed point:
```
x,y
```

**Generation Example:**
```bash
java -cp target/ds503-project2-1.0.0.jar edu.ds503.project2.DataGenerator \
  --points-out data/problem3/seeds.txt \
  --target-count 10
```

---

## Step 2 — Clustering the Data (30 Points)

### Requirements

**Input Parameters:**
- `inputPath`: Path to dataset P
- `seedsPath`: Path to K seed points
- `outputPath`: Path for output results
- `K`: Number of clusters

### Termination Conditions

The algorithm stops when **either** condition is met:
1. **Cluster centers do not change** between two consecutive iterations
2. **Maximum of 6 iterations** reached

### Implementation Constraints

**✅ Optimization Requirements:**
- Use a **combiner** (for efficiency)
- Use a **single reducer** (for center coordination)
- The reducer output must **indicate whether centers changed**

**💡 Hint:**
- K-Means is **iterative**
- Your program should **control** whether another MapReduce iteration should run
- Consider using a **driver program** that:
  - Submits MapReduce jobs iteratively
  - Reads centers after each iteration
  - Decides whether to continue or terminate

---

## Implementation

**File:** `mapreduce/src/KMeansDriver.java`

**Usage:**
```bash
docker exec -it namenode hadoop jar /opt/mapreduce/target/ds503-project2-1.0.0.jar \
  edu.ds503.project2.KMeansDriver \
  /project2/input/P.txt \
  /project2/input/seeds.txt \
  /project2/output/q3 \
  10
```

**Parameters:**
- Input points: `/project2/input/P.txt`
- Initial seeds: `/project2/input/seeds.txt`
- Output: `/project2/output/q3`
- K = `10` clusters

### Algorithm Approach

**Iteration N:**

1. **Map Phase:**
   - Read current centers from distributed cache or HDFS
   - For each point: find nearest center
   - Emit: `(centerID, point)`

2. **Combine Phase:**
   - For each centerID: compute partial sum and count
   - Emit: `(centerID, (sum_x, sum_y, count))`

3. **Reduce Phase:**
   - For each centerID: compute new center as mean of assigned points
   - Compare new center with old center
   - Write new centers to output
   - Write convergence flag (centers changed: yes/no)

4. **Driver Logic:**
   - Read convergence flag
   - If converged or iteration ≥ 6: **stop**
   - Else: use new centers for next iteration

---

## Output Format

**Final Centers File:**
```
clusterID,center_x,center_y,point_count
```

**Clustered Points File (optional):**
```
point_x,point_y,assigned_clusterID
```

---

## Testing

**Test Strategy:**
1. Generate dataset P (100MB+)
2. Generate K seeds (e.g., K=5, K=10)
3. Run clustering and verify convergence
4. Visualize results to validate clustering quality

**Status:** 🚧 **To Be Implemented**
