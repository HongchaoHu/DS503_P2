# DS503 Project 2 — Advanced Hadoop MapReduce Operations

This repository contains three Hadoop MapReduce tasks for DS503:

1. Problem 1 (35 pts): Spatial Join between points and rectangles
2. Problem 2 (35 pts): Distance-based outlier detection
3. Problem 3 (30 pts): K-Means clustering with iterative MapReduce

## Project Structure

```text
DS503_P2/
├── mapreduce/
│   ├── pom.xml
│   ├── src/main/java/edu/ds503/project2/
│   │   ├── DataGenerator.java
│   │   ├── SpatialJoinJob.java
│   │   ├── OutlierJob.java
│   │   ├── KMeansDriver.java
│   │   └── model/
│   │       ├── Point2D.java
│   │       └── Rectangle2D.java
│   └── target/
├── data/
│   └── q1test/
├── docker-compose.yml
├── hadoop.env
└── project 2.pdf
```

## Global Constraints

- Coordinate space is `(1,1)` to `(10000,10000)`.
- Coordinates are integers.
- Datasets should be at least 100MB for final runs.
- Penalties in project spec still apply:
  - Extra MapReduce jobs where only one is required.
  - Non-distributed assumptions (single mapper/reducer where disallowed).

## Quick Start

### 1) Start Hadoop cluster

```bash
docker compose up -d
docker ps
```

### 2) Build JAR

```bash
cd mapreduce
mvn clean package
```

Expected output JAR:

`mapreduce/target/ds503-project2-1.0.0.jar`

### 3) Generate datasets

```bash
cd mapreduce
java -cp target/ds503-project2-1.0.0.jar edu.ds503.project2.DataGenerator \
  --points-out ../data/P.txt --rects-out ../data/R.txt --target-mb 100
```

### 4) Upload to HDFS

```bash
docker exec -it namenode hdfs dfs -mkdir -p /project2/input
docker cp data/P.txt namenode:/tmp/P.txt
docker cp data/R.txt namenode:/tmp/R.txt
docker exec -it namenode hdfs dfs -put -f /tmp/P.txt /project2/input/P.txt
docker exec -it namenode hdfs dfs -put -f /tmp/R.txt /project2/input/R.txt
```

## Problem 1 — Spatial Join

### Problem 1 Goal

Output pairs `<rectangle, point>` where point is inside or on rectangle boundary.

### Problem 1 Inputs

- Points path `P`
- Rectangles path `R`
- Output path
- Optional window `W(x1,y1,x2,y2)`

### Problem 1 Requirements

- Use one MapReduce job with dual input (points + rectangles).
- If window is provided, filter out points/rectangles outside it.

### Problem 1 Run

```bash
docker exec -it namenode hadoop jar /opt/mapreduce/target/ds503-project2-1.0.0.jar \
  edu.ds503.project2.SpatialJoinJob \
  /project2/input/P.txt /project2/input/R.txt /project2/output/q1
```

Windowed version:

```bash
docker exec -it namenode hadoop jar /opt/mapreduce/target/ds503-project2-1.0.0.jar \
  edu.ds503.project2.SpatialJoinJob \
  /project2/input/P.txt /project2/input/R.txt /project2/output/q1_windowed \
  1,3,3,20
```

### Problem 1 Status

- Fully working grid-based partition + window filtering.

### Problem 1 Validation Report (Submission Ready)

This section summarizes the executed tests against the Project 2 PDF requirements for Q1.

#### Environment Notes

- Hadoop services started via `docker compose up -d`.
- Built jar: `mapreduce/target/ds503-project2-1.0.0.jar`.
- In this machine/container setup, jar was executed from `/tmp/ds503-project2-1.0.0.jar` inside `namenode`.

#### Q1 Requirement Checklist

| Requirement (from PDF) | Test Evidence | Result |
| --- | --- | --- |
| Single MapReduce job with distributed execution | `SpatialJoinJob` runs one job and sets `job.setNumReduceTasks(4)`; outputs include `part-r-00000`..`part-r-00003` | PASS |
| Optional window `W(x1,y1,x2,y2)` filtering works | Run with `1,3,3,20` on small dataset produced 3 expected join pairs (IDs differ due to generated IDs) | PASS |
| Boundary inclusion (point on edge/corner is inside rectangle) | Boundary dataset test returned `(1,1)`, `(1,3)`, `(3,1)`, `(3,3)`, `(2,2)` for rectangle `1,1,2,2` | PASS |
| Invalid argument handling | Missing args / malformed window / invalid bounds each return usage+error and exit code `1` | PASS |
| Robustness to malformed lines | Mixed invalid records are skipped; valid records still joined correctly | PASS |
| 100MB-scale run viability | Generated 100MB `P` and 100MB `R`, uploaded to HDFS, completed unwindowed and windowed runs | PASS |

#### Key Commands Used for Validation

```bash
# Build
cd mapreduce
mvn -q clean package -DskipTests

# Small dataset run (no window)
docker exec namenode hadoop jar /tmp/ds503-project2-1.0.0.jar \
  edu.ds503.project2.SpatialJoinJob \
  /project2/input/q1test/P_small.txt /project2/input/q1test/R_small.txt /project2/output/q1test

# Small dataset run (windowed)
docker exec namenode hadoop jar /tmp/ds503-project2-1.0.0.jar \
  edu.ds503.project2.SpatialJoinJob \
  /project2/input/q1test/P_small.txt /project2/input/q1test/R_small.txt /project2/output/q1test_window 1,3,3,20

# 100MB-scale runs
docker exec namenode hadoop jar /tmp/ds503-project2-1.0.0.jar \
  edu.ds503.project2.SpatialJoinJob \
  /project2/input/P_100mb.txt /project2/input/R_100mb.txt /project2/output/q1_100mb

docker exec namenode hadoop jar /tmp/ds503-project2-1.0.0.jar \
  edu.ds503.project2.SpatialJoinJob \
  /project2/input/P_100mb.txt /project2/input/R_100mb.txt /project2/output/q1_100mb_window 1,3,3,20
```

#### Observed Runtime/Output Stats (100MB Inputs)

| Run | Runtime | Reduce Output Records | Output Size (raw) |
| --- | ---: | ---: | ---: |
| Unwindowed | 275 s | 45,114,564 | 979,938,487 bytes |
| Windowed `1,3,3,20` | 12 s | 8 | 135 bytes |

#### Notes for Readme.pdf

- Rectangle IDs in outputs (`r...`) are generated from mapper input offsets, so they may differ from the PDF example IDs but preserve correct join semantics.
- For Q1 status table in `Readme.pdf`, mark **Fully Working** with comments based on the checklist above.

## Problem 2 — Distance-Based Outlier Detection

### Problem 2 Goal

A point is an outlier if fewer than `k` neighbors exist within radius `r`.

### Problem 2 Inputs

- Points path
- Output path
- Radius `r`
- Threshold `k`

### Problem 2 Requirements

- Use one MapReduce job.
- Must support distributed execution.
- Apply segmented spatial processing (hint from PDF) so each point is decided using its segment and adjacent segments.

### Problem 2 Run

```bash
docker exec -it namenode hadoop jar /opt/mapreduce/target/ds503-project2-1.0.0.jar \
  edu.ds503.project2.OutlierJob \
  /project2/input/P.txt /project2/output/q2 20 5
```

### Problem 2 Status

- Implemented and tested.
- Mandatory parameter handling (`r`, `k`) is enforced with error exit on invalid input.
- Single-job segmented algorithm is used (mapper emits to neighboring cells; reducer decides outliers for home-cell points only).
- Validation summary:
  - Small deterministic dataset (`r=2`, `k=2`) reports expected outlier `50,50`.
  - 100MB run (`r=20`, `k=5`) completed successfully in one job (`Q2_100MB_SECONDS=277`).

## Problem 3 — K-Means Clustering

### Problem 3 Goal

Cluster points into `K` groups using iterative center updates.

### Problem 3 Inputs

- Points path
- Seeds path
- Output base path
- `K`

### Problem 3 Requirements

- Iterative jobs, max 6 iterations.
- Stop when centers do not change.
- Use combiner optimization and reducer-driven center updates.
- Use single reducer for center coordination and emit convergence indicator.

### Problem 3 Run

```bash
docker exec -it namenode hadoop jar /opt/mapreduce/target/ds503-project2-1.0.0.jar \
  edu.ds503.project2.KMeansDriver \
  /project2/input/P.txt /project2/input/seeds.txt /project2/output/q3 10
```

### Problem 3 Status

- Implemented iterative K-Means driver and MapReduce pipeline.
- Uses combiner and a single reducer per iteration.
- Reducer output includes convergence metadata (`__meta__\tchanged=true/false`).
- Small deterministic validation completed: converged in 2 iterations with stable centers.
- 100MB validation completed (`K=10`): job finished in 77s and executed 6 iterations (`iter-0`..`iter-5`), stopping at max-iteration cap when centers were still changing.

## Useful Commands

```bash
docker exec -it namenode hdfs dfs -ls -R /project2/
docker exec -it namenode hdfs dfs -rm -r /project2/output/q1
docker exec -it namenode hdfs dfs -rm -r /project2/output/q2
docker exec -it namenode hdfs dfs -rm -r /project2/output/q3
```

## Final Regression Report

This section captures the most recent full end-to-end validation run across build, data generation, and all three problems.

### Scope

- Source build/compile validation
- Data generation and schema/bounds checks
- Problem 1 small + 100MB runs
- Problem 2 argument validation + small + 100MB runs
- Problem 3 small + 100MB iterative runs

### Build and Diagnostics

| Check | Result |
| --- | --- |
| `mvn -q clean package -DskipTests` | PASS |
| VS Code diagnostics (`get_errors`) | PASS |

### Data Validation

| Dataset | Validation | Result |
| --- | --- | --- |
| `P_final_100mb.txt` | Sampled format and bounds (`x,y`, each in `[1,10000]`) | PASS |
| `R_final_100mb.txt` | Sampled format/ranges (`x,y,h,w`; `h∈[1,20]`, `w∈[1,7]`, rectangle in bounds) | PASS |
| `seeds_final_k10.txt` | Seed file created with 10 points | PASS |

### Problem Test Matrix

| Problem | Test | Observed Result | Verdict |
| --- | --- | --- | --- |
| Q1 Spatial Join | Small no-window | 8 output pairs | PASS |
| Q1 Spatial Join | Small window `1,3,3,20` | 3 output pairs | PASS |
| Q1 Spatial Join | 100MB no-window | Completed in 278s, output produced | PASS |
| Q1 Spatial Join | 100MB window | Completed in 12s, tiny filtered output | PASS |
| Q2 Outlier | Missing args | Usage error + exit code 1 | PASS |
| Q2 Outlier | Small deterministic (`r=2,k=2`) | Outlier output includes `50,50` | PASS |
| Q2 Outlier | 100MB (`r=20,k=5`) | Completed in 258s, output count 0 | PASS |
| Q3 K-Means | Small deterministic (`K=2`) | `iter-0: changed=true`, `iter-1: changed=false` | PASS |
| Q3 K-Means | 100MB (`K=10`) | Completed in 78s; `iter-0..iter-5`; stop at max 6 iterations | PASS |

### Overall Status

- Q1: Fully Working
- Q2: Fully Working
- Q3: Fully Working (max-iteration termination path validated)

### Post-Run Cleanup State

- Local generated data/build artifacts were removed.
- HDFS test outputs were removed.
- Baseline test input folder `data/q1test` retained.

## Submission Notes

Submit source code + `Readme.pdf` status report. Do not include generated data or build artifacts.

## References

- K-Means: [Wikipedia Standard Algorithm](http://en.wikipedia.org/wiki/K-means_clustering#Standard_algorithm)
- Course: DS503 Big Data Management
- Spec file: `project 2.pdf`
