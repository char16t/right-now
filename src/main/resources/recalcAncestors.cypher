MATCH path = (s:Task)-[r:DEPENDS_ON*]->(sub:Task)
WHERE elementId(sub) = $subId
WITH path, length(path) as depth
ORDER BY depth ASC
UNWIND relationships(path) as rs
WITH DISTINCT startNode(rs) as sn
CALL {
  WITH sn
  MATCH path = (sn)-[:DEPENDS_ON*]->(sub:Task)
  UNWIND relationships(path) AS r
  WITH collect(DISTINCT endNode(r))   AS endNodes,
       collect(DISTINCT startNode(r)) AS startNodes
  UNWIND endNodes AS leaf
  WITH leaf WHERE NOT leaf IN startNodes
  RETURN sum(leaf.estimate) as total, apoc.coll.max(collect(leaf.due)) as maxDue
}
WITH collect(sn) as sns, collect(total) as totals, collect(maxDue) as maxDues
WITH apoc.coll.zip(sns, totals) as res1, apoc.coll.zip(sns, maxDues) as res2
FOREACH(x in res1 |
  set head(x).estimate = x[1]
)
FOREACH(x in res2 |
  set head(x).due = x[1]
)
RETURN res1, res2;
