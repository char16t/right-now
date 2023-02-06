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
    UNWIND relationShips(path) AS r
    WITH collect(DISTINCT endNode(r))   AS endNodes,
         collect(DISTINCT startNode(r)) AS startNodes
    UNWIND endNodes AS leaf
    WITH leaf WHERE NOT leaf IN startNodes
    RETURN sum(leaf.estimate) as total
}
WITH collect(an) as sns, collect(total) as totals
WITH apoc.coll.zip(sns, totals) as res
FOREACH(x in res |
    set head(x).estimate = x[1]
)
RETURN res;
