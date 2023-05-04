# right-now

The task list engine implemented using Scala and Neo4J. Experimental alternative version of the [Assistant](https://github.com/char16t/assistant) kernel. All logic, if possible, was implemented at the Neo4j database level. It uses the [APOC](https://neo4j.com/developer/neo4j-apoc/) extension from Neo4J.

The main goal of this implementation is to show the user a flat list of those tasks that are available for execution right now. Often tasks from the to-do list depend on each other, so they are presented in the form of a tree. Each task has strictly one parent or exists without a parent. Each task can be collapsed or expanded to provide granularity and the ability to make a more atomic task. Subtasks can be a list or a set. In the first case, they must be executed in order. In the second case, they can be executed in any order. The root task in tree is a "task list".

### API

Call `Todo.method`. Avaliable methods:
 
 * `def todoList(rootId: String): Either[Throwable, Seq[Task]]` &ndash; get flatten list of tasks for right now execution
 * `def getTaskByElementId(elementId: String): Either[Throwable, Task]` &ndash; get task by id
 * `def createTask(task: Task): Either[Throwable, Task]` &ndash; create task
 * `def createSubTask(parentId: String, task: Task): Either[Throwable, Task]` &ndash; create subtask for other task
 * `def updateEstimate(taskId: String, estimate: Double): Either[Throwable, Unit]` &ndash; update estimate for given task
 * `def updateDue(taskId: String, due: ZonedDateTime): Either[Throwable, Unit]` &ndash; update estimate for given task
 * `def deleteTask(taskId: String): Either[Throwable, Any]` &ndash; delete task by id

### Problem and solution

Let's look at the examples:

 * Prepare a Mini Pizza Sandwich (`done: false`, `expand: false`, `type: LIST`)
   * Buy food (`done: false`, `expand: false`, `type: SET`)
     * 1 fresh white loaf (`done: false`, `expand: false`, `type: SET`)
     * 300 – 400 g of sausages (`done: false`, `expand: false`, `type: SET`)
     * 2 large tomatoes (`done: false`, `expand: false`, `type: SET`)
     * 150 g of hard cheese (`done: false`, `expand: false`, `type: SET`)
     * 1 tbsp chopped fresh garlic (`done: false`, `expand: false`, `type: SET`)
     * 0.5 cups chopped dill and parsley (`done: false`, `expand: false`, `type: SET`)
     * Mayonnaise (`done: false`, `expand: false`, `type: SET`)
   * Prepare the ingredients (`done: false`, `expand: false`, `type: SET`)
     * Sausages and tomatoes cut into small cubes (`done: false`, `expand: false`, `type: SET`)
     * Grind the cheese (`done: false`, `expand: false`, `type: SET`)
   * Prepare a thick spread (`done: false`, `expand: false`, `type: SET`)
   * Cover the slices of white bread with the resulting mixture (`done: false`, `expand: false`, `type: SET`)
   * Bake in the oven until lightly browned (`done: false`, `expand: false`, `type: SET`)

A flat task list will look like this:

 * Prepare a Mini Pizza Sandwich

If everything is clear to you, just do it and mark the task completed (`done: true`). If everything is clear to you, just do it and mark the task completed. All subtasks will also be marked as completed. Now your to-do list is empty. 

If you don't understand, expand (`expand: true`) the task. A flat task list will look like this::

 * Buy food

One task again? Please note that it is impossible to make a sandwich if you do not have components at home, so you do not see unnecessary subtasks. Now you need to go to the store. Let's set `expand: true`:

 * 1 fresh white loaf
 * 300 – 400 g of sausages
 * 2 large tomatoes
 * 150 g of hard cheese
 * 1 tbsp chopped fresh garlic
 * 0.5 cups chopped dill and parsley
 * Mayonnaise

Come to the store and buy from the list. Mark the purchased products (`done: true`). What now? Task "Buy food" also marked as done. Our list:

 * Prepare the ingredients

Expand:

 * Sausages and tomatoes cut into small cubes
 * Grind the cheese

Do this in any order. It's only and one actions avaliable for you right now.

Sequentially complete the remaining tasks that appear in the list. Thus, you have prepared a sandwich unnoticed by yourself. You can also do with complex tasks that you don't fully understand. The system will guide you step by step to the result.

You can also do several independent tasks in parallel. Detailing both will help you see only the actions that are available to you for each of them. It works and is very convenient.

### Automatic update of `estimate` and `due` fields

Each task has a time estimate in seconds and a deadline. When you add a task or change an estimate, all child and parent tasks also change time estimates and deadlines.

When the estimate of a task changes, the estimates of its subtasks are updated according to the distribution that was before. For example, if the estimates were distributed as follows:

```
* A (100)
  * AA (30)
  * AB (20)
  * AC (50)
``` 

if you change the estimate of A from 100 to 10, you will get:

```
* A (10)
  * AA (3)
  * AB (2)
  * AC (5)
``` 

If child tasks contain subtasks, they will be recalculated according to the same rule. 

```
        newEstimate + oldEstimate
coef = ----------------------------
               oldEstimate
```

Each estimate must be multiplied by `coef'. Similarly with due dates. Coefficient for revaluation of due dates:

```
         estimate + (dueAfter - dueBefore)
 coef =  ---------------------------------
                     estimate
```

Time estimates and due dates for parent tasks are also recalculates.

### Specification

Properties of each task:

 * `<id>` - internal Neo4j identifier
 * `parentId` - identifier of parent task
 * `title` - name of task
 * `expand` - boolean flag. If the task is expanded (`true`), then the user needs to drill down. If the task is not expanded (`false`), then the drill down is redundant for the user
 * `type` - `LIST` for ordered tasks, `SET` for unordered tasks
 * `done` - task completion flag. `true` when the task is closed, `false` when the task is open
 * `estimate` - estimation of the time to complete the task in seconds
 * `due` - deadline date and time
 * `startTo` - the date and time before which you need to start doing the task in order to meet the deadline

Automatically supported restrictions:

 * The `estimate` value of each task is equal to the sum of `estimate` of all its subtasks
 * The `due` value of each task is equal to the maximum `due` among all its subtasks
 * The `startTo` value of each task is equal to the minimum `startTo` among all its subtasks

### Usage example

```scala
  val nt = new Task(
    elementId = "",
    title = "Root task",
    expand = true,
    `type` = "SET",
    done = false,
    estimate = 0.0,
    due = ZonedDateTime.now(ZoneId.systemDefault())
  )

  val st = new Task(
    elementId = "",
    title = "TSK40",
    expand = true,
    `type` = "SET",
    done = false,
    estimate = 10.0,
    due = ZonedDateTime.now(ZoneId.systemDefault())
  )

  def cp(st: Task, title: String, due: ZonedDateTime) = {
    st.copy(
      title = title,
      due = due,
      startTo = due.minusSeconds(Math.round(st.estimate)))
  }

  val r = for {
    task <- Todo.createTask(nt)
    st1 <- Todo.createSubTask(task.elementId, cp(st, "S1", st.due.plusDays(1)))
    st2 <- Todo.createSubTask(task.elementId, cp(st, "S2", st.due.plusDays(2)))
    _ <- Todo.createSubTask(st1.elementId, cp(st1, "SS1", st1.due.plusDays(1)))
    sst2 <- Todo.createSubTask(st1.elementId, cp(st1, "SS2", st1.due.plusDays(2)))
    _ <- Todo.createSubTask(st1.elementId, cp(st1, "SS3", st1.due.plusDays(3)))
    _ <- Todo.createSubTask(sst2.elementId, cp(sst2, "SS21", st1.due.plusDays(1)))
    _ <- Todo.createSubTask(sst2.elementId, cp(sst2, "SS22", st1.due.plusDays(2)))
    _ <- Todo.updateEstimate(task.elementId, 100)
    _ <- Todo.updateEstimate(sst2.elementId, 10)
    _ <- Todo.updateDue(st1.elementId, ZonedDateTime.parse("2024-02-22T00:00:00.000+03:00"))
    tasks <- Todo.todoList(task.elementId)
//    _ <- Todo.deleteTask(sst2.elementId)
//    _ <- Todo.deleteTask(st1.elementId)
//    _ <- Todo.deleteTask(task.elementId)
  } yield tasks
  r match
    case Right(value) => print(value)
    case Left(e) => throw new Exception(e)
```

Result in Neo4J:

![Graph in Neo4J](https://raw.githubusercontent.com/char16t/i/master/right-now-data-example.png)
