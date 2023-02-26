 // For each parent
MATCH path = (s:Task)-[r:DEPENDS_ON*]->(sub:Task)
WHERE elementId(sub) = $taskId
WITH path, length(path) as depth
ORDER BY depth ASC
UNWIND relationships(path) as rs
WITH collect(DISTINCT endNode(rs))   AS en,
     collect(DISTINCT startNode(rs)) AS sn
WITH DISTINCT en+sn as an1
unwind an1 as an
CALL {
    WITH an
    MATCH path = (an)-[:DEPENDS_ON*]->(sub:Task)
    UNWIND relationships(path) AS r
    WITH collect(DISTINCT endNode(r))   AS endNodes,
         collect(DISTINCT startNode(r)) AS startNodes
    UNWIND endNodes AS leaf
    WITH leaf WHERE NOT leaf IN startNodes
    WITH
      sum(leaf.estimate) as total,
      apoc.coll.max(collect(leaf.due)) as maxDue,
      apoc.coll.min(collect(leaf.startTo)) as minStartTo
    RETURN total, maxDue, minStartTo
}
WITH
  collect(an) as sns,
  collect(total) as totals,
  collect(maxDue) as maxDues,
  collect(minStartTo) as minStartTos
WITH
  apoc.coll.zip(sns, totals) as zippedTotals,
  apoc.coll.zip(sns, maxDues) as zippedMaxDues,
  apoc.coll.zip(sns, minStartTos) as zippedMinStartTos
FOREACH(x in zippedTotals |
    set head(x).estimate = x[1]
)
FOREACH(x in zippedMaxDues |
    set head(x).due = x[1]
)
FOREACH(x in zippedMinStartTos |
  //set head(x).startTo = x[1]
  set head(x).startTo = head(x).due - duration({ seconds: head(x).estimate })
)
RETURN zippedTotals, zippedMaxDues, zippedMinStartTos;
