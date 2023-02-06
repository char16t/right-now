MATCH path = (s:Task)-[r:DEPENDS_ON*]->(sub:Task)
WHERE elementId(sub) = $taskId
WITH path, length(path) as depth
ORDER BY depth DESC
UNWIND relationships(path) as rs
WITH collect(DISTINCT startNode(rs)) AS sn
with head(sn) as hsn
return elementId(hsn) as `id`, hsn as `task`;
