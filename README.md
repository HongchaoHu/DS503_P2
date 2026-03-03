# CS585 Project 2 Starter (Docker + Hadoop MapReduce)

This workspace is set up to help you implement all three required problems:

- Problem 1: Spatial Join
- Problem 2: Distance-Based Outlier Detection
- Problem 3: K-Means Clustering

## Assignment Constraints (from the PDF)

- Use Java MapReduce jobs.
- Problem 1 and Problem 2 use dataset `P`; Problem 1 also needs rectangle dataset `R`.
- Space boundaries are `(1,1)` to `(10000,10000)`.
- Scale datasets to at least `100MB` each for `P` and `R`.
- Problem 2 must be a single MapReduce job (many mappers/reducers allowed, but one job).
- Problem 3 stops when centers do not change or max iterations reaches `6`.

## Structure

- `docker-compose.yml`: Hadoop cluster (single-node services split into containers)
- `mapreduce/`: Java Maven project containing starter classes

## Quick Start

1. Start Hadoop:

```bash
docker compose up -d
```

2. Build Java project:

```bash
cd mapreduce
mvn clean package
```

3. Generate datasets locally:

```bash
java -cp target/cs585-project2-1.0.0.jar edu.cs585.project2.DataGenerator \
  --points-out data/P.txt --rects-out data/R.txt --target-mb 100
```

4. Copy datasets to HDFS (example commands):

```bash
docker exec -it namenode hdfs dfs -mkdir -p /project2/input
docker cp data/P.txt namenode:/tmp/P.txt
docker cp data/R.txt namenode:/tmp/R.txt
docker exec -it namenode hdfs dfs -put -f /tmp/P.txt /project2/input/P.txt
docker exec -it namenode hdfs dfs -put -f /tmp/R.txt /project2/input/R.txt
```

5. Run jobs (examples):

```bash
hadoop jar target/cs585-project2-1.0.0.jar edu.cs585.project2.SpatialJoinJob \
  /project2/input/P.txt /project2/input/R.txt /project2/output/q1

hadoop jar target/cs585-project2-1.0.0.jar edu.cs585.project2.OutlierJob \
  /project2/input/P.txt /project2/output/q2 20 5

hadoop jar target/cs585-project2-1.0.0.jar edu.cs585.project2.KMeansDriver \
  /project2/input/P.txt /project2/input/seeds.txt /project2/output/q3 10
```

## Next Implementation Steps

- Implement reducers/mappers for `SpatialJoinJob` with optional window filtering.
- Implement one-job partitioned outlier detection in `OutlierJob`.
- Implement iterative K-Means driver and center update logic in `KMeansDriver`.
