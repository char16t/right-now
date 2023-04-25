# RightNow Engine

The task list engine implemented using Scala and Neo4J. Experimental alternative version of the [Assistant](https://github.com/char16t/assistant) kernel. All logic, if possible, was implemented at the Neo4j database level. It uses the [APOC](https://neo4j.com/developer/neo4j-apoc/) extension from Neo4J.

The main goal of this implementation is to show the user a flat list of those tasks that are available for execution right now. Often tasks from the to-do list depend on each other, so they are presented in the form of a tree. Each task has strictly one parent or exists without a parent. Each task can be collapsed or expanded to provide granularity and the ability to make a more atomic task. Subtasks can be a list or a set. In the first case, they must be executed in order. In the second case, they can be executed in any order. The root task in tree is a "task list".

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

Bonus: Each task has a time estimate in seconds and a deadline. When you add a task or change an estimate, all child and parent tasks also change time estimates and deadlines.

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


