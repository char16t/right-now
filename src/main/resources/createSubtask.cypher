MATCH
    (parent:Task),
    path = (parent)-[r:DEPENDS_ON*0..1]->(subtask:Task)
WHERE elementId(parent) = $parentId
WITH parent, count(case length(path) when 0 then 1 else path end) as ord
CREATE (a:Task {title: $title, expand: $expand, type: $type, done: $done, estimate: $estimate, due: $due, startTo: $startTo})<-[:DEPENDS_ON {order: ord}]-(parent)
RETURN elementId(a) as `id`, a as `task`;
