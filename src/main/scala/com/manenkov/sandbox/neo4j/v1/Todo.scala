package com.manenkov.sandbox.neo4j.v1

import com.manenkov.sandbox.neo4j.Task
import org.neo4j.driver.{AuthTokens, Driver, GraphDatabase, Record, Result, Session, Transaction, Value}

import java.util

case class Task(
                 elementId: String,
                 title: String,
                 expand: Boolean,
                 `type`: String,
                 done: Boolean,
                 estimate: Double
               ):
  def this(elementId: String, record: Value) = this(
    elementId,
    record.get("title").asString(),
    record.get("expand").asBoolean(),
    record.get("type").asString(),
    record.get("done").asBoolean(),
    record.get("estimate").asDouble()
  )

object Todo:
  private val uri = "neo4j+s://c71aab36.databases.neo4j.io"
  private val user = "neo4j"
  private val password = "atvUqW3hIaPC66fQj9dGTH7jS-dqSbLjhqG_YyOCfGk"
  private val driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password))
  private val GET_TASK_BY_ELEMENT_ID_QUERY = readQuery("getTaskByElementId")
  private val CREATE_TASK_QUERY = readQuery("createTask")
  private val CREATE_SUBTASK_QUERY = readQuery("createSubtask")
  private val RECALC_ANCESTORS_QUERY = readQuery("recalcAncestors")
  private val DELETE_TASK_QUERY = readQuery("deleteTask")
  private val RECALC_ANCESTORS_AFTER_DELETE_QUERY = readQuery("recalcAncestorsAfterDelete")
  private val GET_ROOT_TASK_QUERY = readQuery("getRootTask")
  private val RECALC_TASK_QUERY = readQuery("recalcTask")
  private val TODO_LIST_QUERY = readQuery("todoList")
  private val DELETE_ROOT_TASK_QUERY = readQuery("deleteRootTask")
  private val REORDER_RELATIONSHIPS = readQuery("reorderRelationships")
  private val UPDATE_ESTIMATE_QUERY = readQuery("updateEstimate")

  private def readQuery(name: String): String =
    import scala.io.Source
    Source.fromResource(name + ".cypher").getLines().mkString(System.lineSeparator())

  private def queryPrams[A, B](m: (A, B)*): java.util.Map[A, B] =
    import scala.jdk.CollectionConverters.*
    m.toMap.asJava

  private def throwableToLeft[T](block: => T): Either[Throwable, T] =
    try
      Right(block)
    catch
      case ex: Throwable => Left(ex)

  private def withSession[R](f: Session => R): Either[Throwable, R] =
    val session: Session = driver.session()
    val result: Either[Throwable, R] = throwableToLeft {
      f(session)
    }
    session.close()
    result

  private def withTransaction[R](f: Transaction => R): Either[Throwable, R] =
    withSession { session =>
      val tx = session.beginTransaction()
      try
        val result: R = f(tx)
        tx.commit()
        result
      catch
        case e: Throwable =>
          tx.rollback()
          throw new Exception(e)
      finally
        tx.close()
    }

  def getTaskByElementId(elementId: String): Either[Throwable, Task] =
    withTransaction { tx =>
      val result: Result = tx.run(GET_TASK_BY_ELEMENT_ID_QUERY, queryPrams("elementId" -> elementId))
      new Task(elementId, result.single().get("task"))
    }

  def createTask(task: Task): Either[Throwable, Task] =
    withTransaction { tx =>
      val result: Result = tx.run(CREATE_TASK_QUERY, queryPrams(
        "title" -> task.title,
        "expand" -> task.expand,
        "type" -> task.`type`,
        "done" -> task.done,
        "estimate" -> task.estimate
      ))
      val record = result.single()
      new Task(record.get("id").asString(), record.get("task"))
    }

  def createSubTask(parentId: String, task: Task): Either[Throwable, Task] =
    withTransaction { tx =>
      val result: Result = tx.run(CREATE_SUBTASK_QUERY, queryPrams(
        "parentId" -> parentId,
        "title" -> task.title,
        "expand" -> task.expand,
        "type" -> task.`type`,
        "done" -> task.done,
        "estimate" -> task.estimate
      ))
      val record = result.single()
      val createdTask = new Task(record.get("id").asString(), record.get("task"))
      tx.run(RECALC_ANCESTORS_QUERY, queryPrams(
        "subId" -> createdTask.elementId
      ))
      createdTask
    }

  def updateEstimate(taskId: String, estimate: Double) =
    withTransaction { tx =>
      tx.run(UPDATE_ESTIMATE_QUERY, queryPrams(
        "taskId" -> taskId,
        "newEstimate" -> estimate
      ))
      tx.run(RECALC_ANCESTORS_QUERY, queryPrams(
        "subId" -> taskId
      ))
    }

  def deleteTask(taskId: String): Either[Throwable, Any] =
    import scala.jdk.CollectionConverters.*
    withTransaction { tx =>
      val rootTaskResult: Result = tx.run(GET_ROOT_TASK_QUERY, queryPrams(
        "taskId" -> taskId
      ))
      val rootTaskRecord = rootTaskResult.list().asScala.headOption
      val parentResult: Result = tx.run(DELETE_TASK_QUERY, queryPrams(
        "taskId" -> taskId
      ))
      val parent = parentResult.list().asScala.headOption
      if (parent.isDefined) {
        tx.run(RECALC_ANCESTORS_AFTER_DELETE_QUERY, queryPrams(
          "taskId" -> parent.get.get("id").asString()
        ))

        // Re-enumerate relationships
        tx.run(REORDER_RELATIONSHIPS, queryPrams(
          "taskId" -> parent.get.get("id").asString()
        ))

        // Recalculate root node
        tx.run(RECALC_TASK_QUERY, queryPrams(
          "taskId" -> rootTaskRecord.get.get("id").asString()
        ))
      } else {
        tx.run(DELETE_ROOT_TASK_QUERY, queryPrams(
          "taskId" -> taskId
        ))
      }
    }

  def todoList(rootId: String): Either[Throwable, Seq[Task]] =
    import scala.jdk.CollectionConverters.*
    withTransaction { tx =>
      val result: Result = tx.run(TODO_LIST_QUERY, queryPrams(
        "rootId" -> rootId
      ))
      result.list().asScala.map(record =>
        new Task(record.get("id").asString(), record.get("task"))
      ).toSeq
    }

@main def application() =

  //  Todo.deleteTask("4:df8083f8-9f78-47f7-a403-2a10fcd71c74:39") match
  //    case Right(_) => println("deleted")
  //    case Left(e) => throw new Exception("Could not find task", e)

  //  val root = Todo.getTaskByElementId("4:df8083f8-9f78-47f7-a403-2a10fcd71c74:4") match
  //    case Right(task) => task
  //    case Left(e) => throw new Exception("Could not find task", e)
  //
  //  val subTask = Todo.createSubTask(root.elementId, )
  //
  //  Todo.todoList("4:df8083f8-9f78-47f7-a403-2a10fcd71c74:4") match
  //    case Right(tasks) => tasks.foreach(println)
  //    case Left(e) => throw new Exception("Internal error", e)

  val nt = Task(
    elementId = "",
    title = "Root task",
    expand = true,
    `type` = "SET",
    done = false,
    estimate = 0.0
  )

  val st = Task(
    elementId = "",
    title = "TSK40",
    expand = true,
    `type` = "SET",
    done = false,
    estimate = 10.0
  )

  val r = for {
    task <- Todo.createTask(nt)
    st1 <- Todo.createSubTask(task.elementId, st.copy(title = "S1"))
    st2 <- Todo.createSubTask(task.elementId, st.copy(title = "S2"))
    _ <- Todo.createSubTask(st1.elementId, st.copy(title = "SS1"))
    sst2 <- Todo.createSubTask(st1.elementId, st.copy(title = "SS2"))
    _ <- Todo.createSubTask(st1.elementId, st.copy(title = "SS3"))
    _ <- Todo.createSubTask(sst2.elementId, st.copy(title = "SS31"))
    _ <- Todo.createSubTask(sst2.elementId, st.copy(title = "SS32"))
    _ <- Todo.updateEstimate(task.elementId, 100)
    _ <- Todo.updateEstimate(sst2.elementId, 10)
    tasks <- Todo.todoList(task.elementId)
    _ <- Todo.deleteTask(sst2.elementId)
    _ <- Todo.deleteTask(st1.elementId)
    _ <- Todo.deleteTask(task.elementId)
  } yield tasks
  print(r)

