match (task:Task)
where elementId(task)=$taskId
with task, $newEstimate as a, task.estimate as b
with task, toFloat(a) / toFloat(b) as scaleFactor
match (task)-[:DEPENDS_ON*0..]->(child:Task)
set child.estimate = child.estimate * scaleFactor
return child.title, child.estimate, child.estimate * scaleFactor;
