#!/usr/bin/env bash

echo "Warning! Neo4j runtimesave database directory will be removed. Continue (y/n)?"
read confirmed
[ "$confirmed" = y ] || exit

project_root=$(dirname "$(dirname "$0")")
cd "$project_root" || exit
./gradlew -q --console=plain testClasses
classpath=$(./gradlew -q --console=plain --no-configuration-cache printTestClasspath)

sudo systemctl stop neo4j

for approach in "plain" "packed" "hashed" "pack&hash"; do
  neo4j_dir=/var/lib/neo4j/data
  sudo rm -rf "$neo4j_dir/databases/runtimesave" "$neo4j_dir/transactions/runtimesave"

  sudo systemctl start neo4j
  journalctl -u neo4j -f -n0 --no-pager | sed -n '/Started\./{q;}'
  java -cp "$classpath" com.github.sulir.runtimesave.comparison.Comparison "$approach"
  sudo systemctl stop neo4j

  du -sb "$neo4j_dir/databases/runtimesave"
done

sudo systemctl start neo4j
