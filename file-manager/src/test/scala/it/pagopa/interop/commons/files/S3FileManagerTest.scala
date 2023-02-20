package it.pagopa.interop.commons.files

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.concurrent.ScalaFutures

import java.util.concurrent.Executors
import it.pagopa.interop.commons.files.service.FileManager
import scala.concurrent.ExecutionContext
import org.scalatest.time._
import org.scalatest.BeforeAndAfterAll
import java.util.concurrent.ExecutorService
import java.net.URI
import it.pagopa.interop.commons.files.service.impl.S3ManagerImpl
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import scala.concurrent.Future
import scala.concurrent.ExecutionContextExecutor
import org.scalatest.BeforeAndAfterEach
import scala.concurrent.Await
import scala.concurrent.duration._

class S3FileManagerTest
    extends AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  override implicit val patienceConfig: PatienceConfig = new PatienceConfig(Span(1, Seconds))

  var fileManager: FileManager              = _
  var ex: ExecutorService                   = _
  implicit var ec: ExecutionContextExecutor = _

  override def beforeAll(): Unit = {
    ex = Executors.newFixedThreadPool(4)
    ec = ExecutionContext.fromExecutorService(ex)
    // create locally a minio with a "testBucket"
    fileManager = new S3ManagerImpl(ec)(
      _.endpointOverride(URI.create("http://localhost:9000/"))
        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("minioadmin", "minioadmin")))
    )
  }

  override def afterEach(): Unit = {
    val deletion: Future[Unit] = for {
      files <- fileManager.listFiles("testBucket")("")
      _     <- Future.traverse(files)(fileManager.delete("testBucket"))
    } yield ()
    Await.ready(deletion, 1.second)
  }

  override def afterAll(): Unit = {
    fileManager.close()
    ex.shutdown()
  }

  "S3" ignore {
    "have no files if no interaction was done" in {
      assert(fileManager.listFiles("testBucket")("").futureValue.isEmpty)
    }

    "have a single file with just one is written" in {
      val filesAndContent: Future[(List[String], String)] = for {
        _       <- fileManager.storeBytes("testBucket", "testFolder", "testFile")("testFile".getBytes())
        files   <- fileManager.listFiles("testBucket")("/testFolder")
        content <- fileManager.getFile("testBucket")("/testFolder/testFile").map(new String(_))
      } yield (files, content)

      val (files, content) = filesAndContent.futureValue
      assert(files == "testFolder/testFile" :: Nil)
      assert(content == "testFile")
    }

    "have two files, one of whom in the root dir" in {
      val filesAndContent: Future[(List[String], String, String)] = for {
        _           <- fileManager.storeBytes("testBucket", "", "rootFile")("rootFile".getBytes())
        _           <- fileManager.storeBytes("testBucket", "testFolder", "testFile")("testFile".getBytes())
        files       <- fileManager.listFiles("testBucket")("")
        content     <- fileManager.getFile("testBucket")("/testFolder/testFile").map(new String(_))
        rootContent <- fileManager.getFile("testBucket")("/rootFile").map(new String(_))
      } yield (files, content, rootContent)

      val (files, content, rootContent) = filesAndContent.futureValue
      assert(files == "rootFile" :: "testFolder/testFile" :: Nil)
      assert(content == "testFile")
      assert(rootContent == "rootFile")
    }
    "get all the files" in {
      val filesF: Future[Map[String, Array[Byte]]] = for {
        _     <- fileManager.storeBytes("testBucket", "", "rootFile")("rootFile".getBytes())
        _     <- fileManager.storeBytes("testBucket", "testFolder", "testFile")("testFile".getBytes())
        _     <- fileManager.storeBytes("testBucket", "testFolder/nestedFolder", "nestedFile")("nestedFile".getBytes())
        files <- fileManager.getAllFiles("testBucket")("")
      } yield files

      val files      = filesF.futureValue
      val contentMap = files.map { case (k, v) => (k, new String(v)) }
      val expected   = Map(
        "rootFile"                           -> "rootFile",
        "testFolder/testFile"                -> "testFile",
        "testFolder/nestedFolder/nestedFile" -> "nestedFile"
      )
      assert(contentMap == expected)
    }
  }

}
