# DS503 Project 2 — Advanced Hadoop MapReduce Operations

This project implements three advanced MapReduce operations in Hadoop using Java:

1. **[Problem 1: Spatial Join](problem1/README.md)** (35 points) — Join points and rectangles in 2D space
2. **[Problem 2: Distance-Based Outlier Detection](problem2/README.md)** (35 points) — Find outliers using radius-based neighbors
3. **[Problem 3: K-Means Clustering](problem3/README.md)** (30 points) — Iterative clustering with convergence

📖 **See individual problem READMEs in each folder for detailed requirements and implementation guides.**

---

## Project Structure

```
DS503_P2/
├── problem1/                   # Problem 1: Spatial Join
│   ├── README.md                   - Detailed specifications
│   ├── DataGenerator.java          - Generate datasets P and R
│   └── SpatialJoinJob.java         - MapReduce spatial join implementation
├── problem2/                   # Problem 2: Outlier Detection
│   ├── README.md                   - Detailed specifications
│   └── OutlierJob.java             - MapReduce outlier detection
├── problem3/                   # Problem 3: K-Means Clustering
│   ├── README.md                   - Detailed specifications
│   └── KMeansDriver.java           - Iterative K-Means implementation
├── shared/                     # Shared utility classes
│   ├── Point2D.java                - 2D point model
│   └── Rectangle2D.java            - Rectangle model with spatial operations
├── mapreduce/
│   ├── pom.xml                     - Maven build configuration
│   └── target/                     - Compiled JAR output
├── docker-compose.yml          # Hadoop cluster configuration
├── hadoop.env                  # Hadoop environment variables
└── data/                       # Local test datasets
```

---

## Quick Start

### 1. Start Hadoop Cluster

```bash
docker compose up -d
```

**Verify all services are healthy:**
```bash
docker ps
```
All 5 containers should show `(healthy)` status.

### 2. Build Java Project

```bash
docker run --rm -v "${PWD}/mapreduce:/app" -w /app maven:3.9.9-eclipse-temurin-8 mvn clean package
```

**Output:** `mapreduce/target/ds503-project2-1.0.0.jar`

### 3. Generate Datasets

**Generate 100MB datasets P and R:**
```bash
docker run --rm -v "${PWD}/mapreduce:/app" -v "${PWD}/data:/data" -w /app maven:3.9.9-eclipse-temurin-8 \
  java -cp target/ds503-project2-1.0.0.jar edu.ds503.project2.DataGenerator \
  --points-out /data/P.txt --rects-out /data/R.txt --target-mb 100
```

### 4. Upload to HDFS

```bash
# Create HDFS directories
docker exec -it namenode hdfs dfs -mkdir -p /project2/input

# Copy datasets to container
docker cp data/P.txt namenode:/tmp/P.txt
docker cp data/R.txt namenode:/tmp/R.txt

# Upload to HDFS
docker exec -it namenode hdfs dfs -put -f /tmp/P.txt /project2/input/P.txt
docker exec -it namenode hdfs dfs -put -f /tmp/R.txt /project2/input/R.txt

# Verify upload
docker exec -it namenode hdfs dfs -ls /project2/input/
```

### 5. Run MapReduce Jobs

**Problem 1: Spatial Join (No Window)**
```bash
docker exec -it namenode hadoop jar /opt/mapreduce/target/ds503-project2-1.0.0.jar \
  edu.ds503.project2.SpatialJoinJob \
  /project2/input/P.txt /project2/input/R.txt /project2/output/q1
```

**Problem 1: Spatial Join (With Window)**
```bash
docker exec -it namenode hadoop jar /opt/mapreduce/target/ds503-project2-1.0.0.jar \
  edu.ds503.project2.SpatialJoinJob \
  /project2/input/P.txt /project2/input/R.txt /project2/output/q1_windowed \
  1 3 3 20
```

**Problem 2: Outlier Detection**
```bash
docker exec -it namenode hadoop jar /opt/mapreduce/target/ds503-project2-1.0.0.jar \
  edu.ds503.project2.OutlierJob \
  /project2/input/P.txt /project2/output/q2 \
  20 5
```

**Problem 3: K-Means Clustering**
```bash
# Generate K seed points first
# Then run clustering
docker exec -it namenode hadoop jar /opt/mapreduce/target/ds503-project2-1.0.0.jar \
  edu.ds503.project2.KMeansDriver \
  /project2/input/P.txt /project2/input/seeds.txt /project2/output/q3 \
  10
```

---

## Implementation Status

| Problem | Status | Implementation |
|---------|--------|----------------|
| Problem 1: Spatial Join | ✅ Fully Working | Grid-based partitioning with window filtering |
| Problem 2: Outlier Detection | 🚧 To Be Implemented | Single MapReduce job with spatial segmentation |
| Problem 3: K-Means Clustering | 🚧 To Be Implemented | Iterative driver with combiner optimization |

---

## Key Constraints

**All Problems:**
- Coordinate space: **(1,1) to (10,000, 10,000)**
- All coordinates are **integers**
- Datasets must be **at least 100 MB** each

**MapReduce Requirements:**
- Problem 1: **One MapReduce job** with dual input (P and R)
- Problem 2: **One MapReduce job** (multiple mappers/reducers allowed)
- Problem 3: **Iterative MapReduce** jobs (max 6 iterations)

**Penalties:**
- Extra MapReduce job: **−10 points per job**
- Single mapper/reducer (no distribution): **−15 points**

---

## Docker Services

The Hadoop cluster consists of 5 containerized services:

- **namenode**: HDFS NameNode (metadata management)
- **datanode**: HDFS DataNode (data storage)
- **resourcemanager**: YARN ResourceManager (job scheduling)
- **nodemanager**: YARN NodeManager (task execution)
- **historyserver**: MapReduce Job History Server (job logs)

**Access Web UIs:**
- NameNode: http://localhost:9870
- ResourceManager: http://localhost:8088
- JobHistory: http://localhost:19888

---

## Useful Commands

**View HDFS files:**
```bash
docker exec -it namenode hdfs dfs -ls -R /project2/
docker exec -it namenode hdfs dfs -cat /project2/output/q1/part-r-00000 | head -20
```

**Clean output directories:**
```bash
docker exec -it namenode hdfs dfs -rm -r /project2/output/q1
docker exec -it namenode hdfs dfs -rm -r /project2/output/q2
docker exec -it namenode hdfs dfs -rm -r /project2/output/q3
```

**Rebuild and redeploy JAR:**
```bash
docker run --rm -v "${PWD}/mapreduce:/app" -w /app maven:3.9.9-eclipse-temurin-8 mvn clean package
docker cp mapreduce/target/ds503-project2-1.0.0.jar namenode:/opt/mapreduce/target/
```

**View MapReduce job logs:**
```bash
docker exec -it namenode hadoop job -history all
```

---

## Submission Requirements

**Submit one ZIP file containing:**
- Java source code (data generation + MapReduce jobs)
- `Readme.pdf` with:
  - Any assumptions made
  - Status table:

| Question | Status | Comment |
|----------|--------|---------|
| Q1 | Fully Working / Partially Working / Not Working | |
| Q2 | Fully Working / Partially Working / Not Working | |
| Q3 | Fully Working / Partially Working / Not Working | |

  - Additional implementation comments

**Do NOT include:**
- Compiled `.class` files
- `mapreduce/target/` directory
- Generated datasets (P.txt, R.txt)

---

## References

- [K-Means Clustering Algorithm](http://en.wikipedia.org/wiki/K-means_clustering#Standard_algorithm)
- Course: DS503 - Big Data Management
- Project specification: `project 2.pdf`
