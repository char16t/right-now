MATCH (t: Task), p = (t)-[:DEPENDS_ON*0..]->(sub:Task)
WHERE elementId(t) = $taskId
DETACH DELETE p;
