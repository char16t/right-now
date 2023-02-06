// find all sets of "firstChildren"
MATCH (root:Task),
      (p:Task {expand: true})-[r:DEPENDS_ON]->(child)
  WHERE
  elementId(root) = $rootId
  and EXISTS((root)-[:DEPENDS_ON*0..]->(p))
  and child.done=false
WITH root,p,r,child
  ORDER BY r.order ASC
WITH
  root,
  p,
  case p.type
    when 'LIST'    then COLLECT(child)[..1]
    when 'SET'     then COLLECT(child)
//    when 'ONE_OF'  then []
//    when 'MANY_OF' then []
    else []
    end as firstChildren

// create a unique list of the Tasks that could be part of the tree
WITH root,apoc.coll.toSet(
apoc.coll.flatten(
[root]+COLLECT([x in firstChildren where x.done=false])
)
) AS TasksInTree

MATCH path = (root)-[:DEPENDS_ON*]->(leaf:Task)
  WHERE ALL(node in nodes(path) WHERE node IN TasksInTree)

WITH path, length(path) as depth

UNWIND relationShips(path) AS r

WITH r
  ORDER BY depth ASC, r.order ASC

WITH collect(DISTINCT endNode(r))   AS endNodes,
     collect(DISTINCT startNode(r)) AS startNodes
UNWIND endNodes AS leaf
WITH leaf WHERE NOT leaf IN startNodes
RETURN elementId(leaf) as `id`, leaf as `task`;
