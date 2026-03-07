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

### Problem 2 Run

```bash
docker exec -it namenode hadoop jar /opt/mapreduce/target/ds503-project2-1.0.0.jar \
  edu.ds503.project2.OutlierJob \
  /project2/input/P.txt /project2/output/q2 20 5
```

### Problem 2 Status

- Job skeleton exists; core outlier logic still TODO.

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

### Problem 3 Run

```bash
docker exec -it namenode hadoop jar /opt/mapreduce/target/ds503-project2-1.0.0.jar \
  edu.ds503.project2.KMeansDriver \
  /project2/input/P.txt /project2/input/seeds.txt /project2/output/q3 10
```

### Problem 3 Status

- Driver/iteration skeleton exists; mapper/reducer clustering logic still TODO.

## Useful Commands

```bash
docker exec -it namenode hdfs dfs -ls -R /project2/
docker exec -it namenode hdfs dfs -rm -r /project2/output/q1
docker exec -it namenode hdfs dfs -rm -r /project2/output/q2
docker exec -it namenode hdfs dfs -rm -r /project2/output/q3
```

## Submission Notes

Submit source code + `Readme.pdf` status report. Do not include generated data or build artifacts.

## References

- K-Means: [Wikipedia Standard Algorithm](http://en.wikipedia.org/wiki/K-means_clustering#Standard_algorithm)
- Course: DS503 Big Data Management
- Spec file: `project 2.pdf`
