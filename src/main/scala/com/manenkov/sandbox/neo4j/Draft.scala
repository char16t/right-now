package com.manenkov.sandbox.neo4j
import com.manenkov.sandbox.neo4j.v1.Task
import org.neo4j.driver.{AuthTokens, Driver, GraphDatabase, Query, Session}

case class Task(title: String, sub: Seq[Task] = Seq())

def hello() = {
  val uri = "neo4j+s://c71aab36.databases.neo4j.io"
  val user = "neo4j"
  val password = "atvUqW3hIaPC66fQj9dGTH7jS-dqSbLjhqG_YyOCfGk"
  val driver: Driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password))

  val session: Session = driver.session()
  def write = session.executeWrite(tx => {
    val query = new Query(createQuery2)
    tx.run(query)
  })

  val tx = session.beginTransaction()
  val result = tx.run(query6)
  val found = result.list().forEach(r => {
    println(r.get("task").get("title"))
  })


  tx.close()
  driver.close()
}

val createQuery1 =
  """
    |create
    |  (a:Task {title:"A", expand: true, ordered:false, done: false}),
    |  (aa:Task {title:"AA", expand: true, ordered: false, done: false}),
    |  (ab:Task {title:"AB", expand: true, ordered: false, done: false}),
    |  (ac:Task {title:"AC", expand: true, ordered: false, done: false}),
    |  (aaa:Task {title:"AAA", expand: true, ordered: false, done: false}),
    |  (aab:Task {title:"AAB", expand: true, ordered: false, done: false}),
    |  (aac:Task {title:"AAC", expand: true, ordered: false, done: false}),
    |  (a)-[:DEPENDS_ON { order: 1 }]->(aa),
    |  (a)-[:DEPENDS_ON { order: 2 }]->(ab),
    |  (a)-[:DEPENDS_ON { order: 3 }]->(ac),
    |  (aa)-[:DEPENDS_ON{ order: 1 }]->(aaa),
    |  (aa)-[:DEPENDS_ON{ order: 2 }]->(aab),
    |  (aa)-[:DEPENDS_ON{ order: 3 }]->(aac)
    |return *;
    |""".stripMargin

val createQuery2 =
  """
    |create
    |  (a:Task {title:"A", expand: true, type: 'SET', done: false, estimate: 5}),
    |  (aa:Task {title:"AA", expand: true, type: 'LIST', done: false, estimate: 5}),
    |  (ab:Task {title:"AB", expand: true, type: 'SET', done: false, estimate: 5}),
    |  (ac:Task {title:"AC", expand: true, type: 'SET', done: false, estimate: 5}),
    |  (aaa:Task {title:"AAA", expand: true, type: 'SET', done: false, estimate: 5}),
    |  (aab:Task {title:"AAB", expand: true, type: 'SET', done: false, estimate: 5}),
    |  (aac:Task {title:"AAC", expand: true, type: 'SET', done: false, estimate: 5}),
    |  (a)-[:DEPENDS_ON { order: 1 }]->(aa),
    |  (a)-[:DEPENDS_ON { order: 2 }]->(ab),
    |  (a)-[:DEPENDS_ON { order: 3 }]->(ac),
    |  (aa)-[:DEPENDS_ON{ order: 1 }]->(aaa),
    |  (aa)-[:DEPENDS_ON{ order: 2 }]->(aab),
    |  (aa)-[:DEPENDS_ON{ order: 3 }]->(aac)
    |return *;
    |""".stripMargin


val insertQuery =
  """
    |MATCH path = (e:Task {title: "AB"})-[r:DEPENDS_ON]->(b)
    |WITH e, count(path)+1 as ord
    |CREATE (a:Task {title:"ABD", expand: true, type: 'SET', done: false, estimate: 10})<-[:DEPENDS_ON { order: ord}]-(e)
    |RETURN a;
    |""".stripMargin

// Recalculate all tree. Need to be optimized
val afterInsertA =
  """
    |MATCH (s:Task)-[r:DEPENDS_ON*]->(sub:Task)
    |WITH s, sum(sub.estimate) as total
    |set s.estimate=total
    |RETURN *;
    |""".stripMargin

// Recalculate all tree. Need to be optimized
val afterInsertB =
  """
    |MATCH (s:Task)-[r:DEPENDS_ON]->(sub:Task)
    |WITH s, sum(sub.estimate) as total
    |set s.estimate=total
    |RETURN *;
    |""".stripMargin

val query0 =
  """
    |Match path = (n:Task {title:"A"})-[:DEPENDS_ON *0..]->(m:Task)
    |Where
    |  m.done = false
    |  and exists(({expand: true, done: false})-[:DEPENDS_ON]->(m))
    |UNWIND relationShips(path) AS r
    |WITH collect(DISTINCT endNode(r))   AS endNodes,
    |     collect(DISTINCT startNode(r)) AS startNodes
    |UNWIND endNodes AS leaf
    |WITH leaf WHERE NOT leaf IN startNodes
    |RETURN leaf as `task`;
    |""".stripMargin;

val query1 =
  """
    |// find all sets of "firstChildren"
    |MATCH (jon:Task { title:'A'}),
    |       (p:Task)-[:DEPENDS_ON]->(child)
    |WHERE EXISTS((jon)-[:DEPENDS_ON*0..]->(p))
    |WITH
    |  jon,
    |  p,
    |  case p.ordered
    |    when true then COLLECT(child)[..1]
    |    else collect(child)
    |  end as firstChildren
    |
    |
    |// create a unique list of the Tasks that could be part of the tree
    |WITH jon,apoc.coll.toSet(
    | apoc.coll.flatten(
    |   [jon]+COLLECT(firstChildren)
    | )
    |) AS TasksInTree
    |
    |MATCH path = (jon)-[:DEPENDS_ON*]->(leaf:Task)
    |WHERE NOT (leaf)-[:DEPENDS_ON]->(:Task)
    |AND ALL(node in nodes(path) WHERE node IN TasksInTree)
    |
    |WITH collect(path) as paths
    |CALL apoc.convert.toTree(paths) yield value
    |RETURN paths;
    |
    |""".stripMargin

val query2 =
  """
    |// find all sets of "firstChildren"
    |MATCH (root:Task { title:'A'}),
    |       (p:Task)-[:DEPENDS_ON]->(child)
    |WHERE EXISTS((root)-[:DEPENDS_ON*0..]->(p))
    |WITH
    |  root,
    |  p,
    |  case p.ordered
    |    when true then COLLECT(child)[..1]
    |    else collect(child)
    |  end as firstChildren
    |
    |
    |// create a unique list of the Tasks that could be part of the tree
    |WITH root,apoc.coll.toSet(
    | apoc.coll.flatten(
    |   [root]+COLLECT(firstChildren)
    | )
    |) AS TasksInTree
    |
    |MATCH path = (root)-[:DEPENDS_ON*]->(leaf:Task)
    |WHERE NOT (leaf)-[:DEPENDS_ON]->(:Task)
    |AND ALL(node in nodes(path) WHERE node IN TasksInTree)
    |
    |UNWIND relationShips(path) AS r
    |WITH collect(DISTINCT endNode(r))   AS endNodes,
    |     collect(DISTINCT startNode(r)) AS startNodes
    |UNWIND endNodes AS leaf
    |WITH leaf WHERE NOT leaf IN startNodes
    |RETURN leaf as `task`;
    |""".stripMargin

val query3 =
  """
    |// find all sets of "firstChildren"
    |MATCH (root:Task { title:'A'}),
    |       (p:Task {expand: true})-[:DEPENDS_ON]->(child)
    |WHERE
    |  EXISTS((root)-[:DEPENDS_ON*0..]->(p))
    |  and child.done=false
    |WITH
    |  root,
    |  p,
    |  case p.ordered
    |    when true then COLLECT(child)[..1]
    |    else collect(child)
    |  end as firstChildren
    |//with root, firstChildren, [x IN firstChildren WHERE x.done = false] AS resFirstChildren
    |
    |// create a unique list of the Tasks that could be part of the tree
    |WITH root,apoc.coll.toSet(
    | apoc.coll.flatten(
    |   [root]+COLLECT([x in firstChildren where x.done=false])
    | )
    |) AS TasksInTree
    |
    |MATCH path = (root)-[:DEPENDS_ON*]->(leaf:Task)
    |WHERE ALL(node in nodes(path) WHERE node IN TasksInTree)
    |
    |// select only leafs
    |UNWIND relationShips(path) AS r
    |WITH collect(DISTINCT endNode(r))   AS endNodes,
    |     collect(DISTINCT startNode(r)) AS startNodes
    |UNWIND endNodes AS leaf
    |WITH leaf WHERE NOT leaf IN startNodes
    |RETURN leaf as `task`;
    |""".stripMargin

val query4 =
  """
    |// find all sets of "firstChildren"
    |MATCH (root:Task { title:'A'}),
    |       (p:Task {expand: true})-[r:DEPENDS_ON]->(child)
    |WHERE
    |  EXISTS((root)-[:DEPENDS_ON*0..]->(p))
    |  and child.done=false
    |WITH root,p,r,child
    |ORDER BY r.order ASC
    |WITH
    |  root,
    |  p,
    |  case p.ordered
    |    when true then COLLECT(child)[..1]
    |    else collect(child)
    |  end as firstChildren
    |
    |// create a unique list of the Tasks that could be part of the tree
    |WITH root,apoc.coll.toSet(
    | apoc.coll.flatten(
    |   [root]+COLLECT([x in firstChildren where x.done=false])
    | )
    |) AS TasksInTree
    |
    |MATCH path = (root)-[:DEPENDS_ON*]->(leaf:Task)
    |WHERE ALL(node in nodes(path) WHERE node IN TasksInTree)
    |
    |UNWIND relationShips(path) AS r
    |WITH collect(DISTINCT endNode(r))   AS endNodes,
    |     collect(DISTINCT startNode(r)) AS startNodes
    |UNWIND endNodes AS leaf
    |WITH leaf WHERE NOT leaf IN startNodes
    |RETURN leaf as `task`;
    |""".stripMargin

val query5 =
  """
    |// find all sets of "firstChildren"
    |MATCH (root:Task { title:'A'}),
    |       (p:Task {expand: true})-[r:DEPENDS_ON]->(child)
    |WHERE
    |  EXISTS((root)-[:DEPENDS_ON*0..]->(p))
    |  and child.done=false
    |WITH root,p,r,child
    |ORDER BY r.order ASC
    |WITH
    |  root,
    |  p,
    |  case p.ordered
    |    when true then COLLECT(child)[..1]
    |    else collect(child)
    |  end as firstChildren
    |
    |// create a unique list of the Tasks that could be part of the tree
    |WITH root,apoc.coll.toSet(
    | apoc.coll.flatten(
    |   [root]+COLLECT([x in firstChildren where x.done=false])
    | )
    |) AS TasksInTree
    |
    |MATCH path = (root)-[rrr:DEPENDS_ON*]->(leaf:Task)
    |WHERE ALL(node in nodes(path) WHERE node IN TasksInTree)
    |
    |WITH path, length(path) as depth
    |
    |UNWIND relationShips(path) AS r
    |
    |WITH r
    |ORDER BY depth ASC, r.order ASC
    |
    |WITH collect(DISTINCT endNode(r))   AS endNodes,
    |     collect(DISTINCT startNode(r)) AS startNodes
    |UNWIND endNodes AS leaf
    |WITH leaf WHERE NOT leaf IN startNodes
    |RETURN leaf as `task`;
    |""".stripMargin

val query6 =
  """
    |// find all sets of "firstChildren"
    |MATCH (root:Task { title:'A'}),
    |       (p:Task {expand: true})-[r:DEPENDS_ON]->(child)
    |WHERE
    |  EXISTS((root)-[:DEPENDS_ON*0..]->(p))
    |  and child.done=false
    |WITH root,p,r,child
    |ORDER BY r.order ASC
    |WITH
    |  root,
    |  p,
    |  case p.type
    |    when 'LIST'    then COLLECT(child)[..1]
    |    when 'SET'     then COLLECT(child)
    |    when 'ONE_OF'  then []
    |    when 'MANY_OF' then []
    |    else []
    |  end as firstChildren
    |
    |// create a unique list of the Tasks that could be part of the tree
    |WITH root,apoc.coll.toSet(
    | apoc.coll.flatten(
    |   [root]+COLLECT([x in firstChildren where x.done=false])
    | )
    |) AS TasksInTree
    |
    |MATCH path = (root)-[:DEPENDS_ON*]->(leaf:Task)
    |WHERE ALL(node in nodes(path) WHERE node IN TasksInTree)
    |
    |WITH path, length(path) as depth
    |
    |UNWIND relationShips(path) AS r
    |
    |WITH r
    |ORDER BY depth ASC, r.order ASC
    |
    |WITH collect(DISTINCT endNode(r))   AS endNodes,
    |     collect(DISTINCT startNode(r)) AS startNodes
    |UNWIND endNodes AS leaf
    |WITH leaf WHERE NOT leaf IN startNodes
    |RETURN leaf as `task`;
    |""".stripMargin

val sumTree =
  """
    |create
    |  (a:Node {title:"A", value: 25}),
    |  (aa:Node {title:"AA", value: 15}),
    |  (ab:Node {title:"AB", value: 5}),
    |  (ac:Node {title:"AC", value: 5}),
    |  (aaa:Node {title:"AAA", value: 5}),
    |  (aab:Node {title:"AAB", value: 5}),
    |  (aac:Node {title:"AAC", value: 5}),
    |  (a)-[:HAS]->(aa),
    |  (a)-[:HAS]->(ab),
    |  (a)-[:HAS]->(ac),
    |  (aa)-[:HAS]->(aaa),
    |  (aa)-[:HAS]->(aab),
    |  (aa)-[:HAS]->(aac)
    |return *;
    |""".stripMargin

val addNodeDraft =
  """
    |MATCH (parent:Node {title: "AB"})
    |WITH parent
    |CREATE (a:Node {title:"ABA", value: 15})<-[:HAS]-(parent)
    |RETURN a;
    |
    |MATCH path = (s:Node)-[r:HAS*]->(sub:Node {title: 'ABA'})
    |WITH path, length(path) as depth
    |ORDER BY depth ASC
    |UNWIND relationships(path) as rs
    |WITH DISTINCT startNode(rs) as sn
    |CALL {
    |    WITH sn
    |    MATCH path = (sn)-[:HAS*]->(sub:Node)
    |    UNWIND relationShips(path) AS r
    |    WITH collect(DISTINCT endNode(r))   AS endNodes,
    |        collect(DISTINCT startNode(r)) AS startNodes
    |    UNWIND endNodes AS leaf
    |    WITH leaf WHERE NOT leaf IN startNodes
    |    RETURN sum(leaf.value) as total
    |}
    |WITH collect(sn) as sns, collect(total) as totals
    |WITH apoc.coll.zip(sns, totals) as res
    |FOREACH(x in res |
    |    set head(x).value = x[1]
    |)
    |RETURN res;
    |""".stripMargin

val delNodeDraft =
  """
    |match p1=(m:Node {title: "AAA"})<-[:HAS]-(n:Node)
    |CALL {
    |    with m
    |    match p = (m)-[:HAS*0..]->(k:Node)
    |    DETACH DELETE p
    |}
    |return n;
    |
    | // For each parent
    |MATCH path = (s:Node)-[r:HAS*]->(sub:Node {title: 'AA'})
    |WITH path, length(path) as depth
    |ORDER BY depth ASC
    |UNWIND relationships(path) as rs
    |WITH collect(DISTINCT endNode(rs))   AS en,
    |     collect(DISTINCT startNode(rs)) AS sn
    |WITH DISTINCT en+sn as an1
    |unwind an1 as an
    |CALL {
    |    WITH an
    |    MATCH path = (an)-[:HAS*]->(sub:Node)
    |    UNWIND relationShips(path) AS r
    |    WITH collect(DISTINCT endNode(r))   AS endNodes,
    |         collect(DISTINCT startNode(r)) AS startNodes
    |    UNWIND endNodes AS leaf
    |    WITH leaf WHERE NOT leaf IN startNodes
    |    RETURN sum(leaf.value) as total
    |}
    |WITH collect(an) as sns, collect(total) as totals
    |WITH apoc.coll.zip(sns, totals) as res
    |FOREACH(x in res |
    |    set head(x).value = x[1]
    |)
    |RETURN res;
    |""".stripMargin

val updateNode =
  """
    |match path=(n:Node {title: 'A'})-[r:HAS*0..]->(m:Node)
    |with n, path, length(path) as depth
    |order by depth DESC
    |with n, path
    |unwind relationships(path) as rs
    |
    |set n.internalPrevValue = toFloat(n.value)
    |set n.value = 1000
    |
    |
    |with n,  collect(distinct startNode(rs))+collect(distinct endNode(rs)) as en
    |unwind en as enu
    |with n, enu
    |CALL {
    |    with enu
    |    match path = (enu)-[r:HAS*0..]->(m:Node)
    |    UNWIND relationships(path) as rs
    |    WITH enu, collect(DISTINCT endNode(rs)) AS en
    |    with enu, en, reduce(acc = 0, v in en | acc + v.value) as s
    |    FOREACH (pi IN en |
    |        SET pi.internalPrevValue = toFloat(pi.value)
    |        SET pi.value = enu.value*(pi.value/enu.internalPrevValue)
    |    )
    |    return en as en001
    |}
    |unwind en001 as en0
    |
    |with n, en0
    |remove n.internalPrevValue
    |
    |with en0
    |call { with en0 remove en0.internalPrevValue return en0 as en01 }
    |return en01;
    |
    |
    |MATCH path = (s:Node)-[r:HAS*]->(sub:Node {title: 'A'})
    |WITH path, length(path) as depth
    |ORDER BY depth ASC
    |UNWIND relationships(path) as rs
    |WITH DISTINCT startNode(rs) as sn
    |CALL {
    |    WITH sn
    |    MATCH path = (sn)-[:HAS*]->(sub:Node)
    |    UNWIND relationShips(path) AS r
    |    WITH collect(DISTINCT endNode(r))   AS endNodes,
    |        collect(DISTINCT startNode(r)) AS startNodes
    |    UNWIND endNodes AS leaf
    |    WITH leaf WHERE NOT leaf IN startNodes
    |    RETURN sum(leaf.value) as total
    |}
    |WITH collect(sn) as sns, collect(total) as totals
    |WITH apoc.coll.zip(sns, totals) as res
    |FOREACH(x in res |
    |    set head(x).value = x[1]
    |)
    |RETURN res;
    |""".stripMargin