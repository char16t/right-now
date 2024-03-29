match (task:Task)
where elementId(task)=$taskId
with task, (duration({seconds: task.estimate}) + duration.inSeconds(task.due, $newDue)) / task.estimate as coef
match (task)-[:DEPENDS_ON*0..]->(child:Task)
set child.due = child.due + (child.estimate * coef - duration({seconds: child.estimate}))
set child.startTo = child.due - duration({ seconds: child.estimate })
return child.title, child.due, child.startTo;
