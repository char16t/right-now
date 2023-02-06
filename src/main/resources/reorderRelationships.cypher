match p = (n:Task)-[r:DEPENDS_ON]->(m:Task)
where elementId(n) = $taskId
with p, r
ORDER BY r.order ASC
unwind relationships(p) as rs
with collect(rs) as crs
with crs, size(crs) as cnt
with apoc.coll.zip(crs, range(1, cnt)) as result
foreach(x in result |
    set head(x).order = x[1]
)
return result;
