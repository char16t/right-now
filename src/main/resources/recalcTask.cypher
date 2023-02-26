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
  WITH
    sum(leaf.estimate) as total
  RETURN total
}
WITH
  sn,
  collect(sn) as sns,
  collect(total) as totals
WITH
  sn, apoc.coll.zip(sns, totals) as zippedTotals
FOREACH(x in zippedTotals |
  set head(x).estimate = x[1]
)
WITH sn
CALL {
  WITH sn
  MATCH path = (sn)-[:DEPENDS_ON]->(sub:Task)
  UNWIND relationships(path) AS r
  WITH collect(DISTINCT endNode(r))   AS endNodes,
       collect(DISTINCT startNode(r)) AS startNodes
  UNWIND endNodes AS leaf
  WITH leaf WHERE NOT leaf IN startNodes
  WITH
    apoc.coll.max(collect(leaf.due)) as maxDue,
    apoc.coll.min(collect(leaf.startTo)) as minStartTo
  RETURN maxDue, minStartTo
}
WITH
  collect(sn) as sns2,
  collect(maxDue) as maxDues,
  collect(minStartTo) as minStartTos
WITH
  apoc.coll.zip(sns2, maxDues) as zippedMaxDues,
  apoc.coll.zip(sns2, minStartTos) as zippedMinStartTos
FOREACH(x in zippedMaxDues |
  set head(x).due = x[1]
)
FOREACH(x in zippedMinStartTos |
  set head(x).startTo = x[1]
  //set head(x).startTo = head(x).due - duration({ seconds: head(x).estimate })
)
RETURN *;
