MATCH (sn:Task)
WHERE elementId(sn) = $taskId
WITH sn
CALL {
  WITH sn
  MATCH path = (sn)-[:DEPENDS_ON]->(sub:Task)
  UNWIND relationShips(path) AS r
  WITH collect(DISTINCT endNode(r))   AS endNodes,
       collect(DISTINCT startNode(r)) AS startNodes
  UNWIND endNodes AS leaf
  WITH leaf WHERE NOT leaf IN startNodes
  RETURN sum(leaf.estimate) as total
}
WITH collect(sn) as sns, collect(total) as totals
WITH apoc.coll.zip(sns, totals) as res
FOREACH(x in res |
  set head(x).estimate = x[1]
)
RETURN res;
