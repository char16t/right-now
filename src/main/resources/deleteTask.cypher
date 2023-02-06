match p1=(m:Task)<-[:DEPENDS_ON]-(n:Task)
where elementId(m) = $taskId
CALL {
    with m
    match p = (m)-[:DEPENDS_ON*0..]->(k:Task)
    DETACH DELETE p
}
return  elementId(n) as `id`, n as `task`;
