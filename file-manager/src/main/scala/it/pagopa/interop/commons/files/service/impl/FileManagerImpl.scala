package it.pagopa.interop.commons.files.service.impl

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.interop.commons.files.service.FileManager

import java.io.{ByteArrayOutputStream, File, FileInputStream}
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.ExecutionContextExecutor

final class FileManagerImpl(blockingExecutionContext: ExecutionContextExecutor) extends FileManager {

  implicit val ec: ExecutionContextExecutor = blockingExecutionContext

  val tmp: Path = Path.of("/tmp")

  override def store(
    containerPath: String,
    path: String
  )(resourceId: UUID, fileParts: (FileInfo, File)): Future[StorageFilePath] = Future {
    val destPath: Path = createPath(path, resourceId.toString, fileParts._1.getFileName)
    Files.move(fileParts._2.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING).toAbsolutePath().toString()
  }

  override def storeBytes(
    containerPath: String,
    path: String
  )(resourceId: UUID, fileName: String, fileContents: Array[Byte]): Future[StorageFilePath] = Future {
    val destPath: Path = createPath(path, resourceId.toString, fileName)
    Files.write(destPath, fileContents).toAbsolutePath().toString()
  }

  override def get(containerPath: String)(filePath: String): Future[ByteArrayOutputStream] = Future {
    val outputStream: ByteArrayOutputStream = new ByteArrayOutputStream()
    new FileInputStream(filePath).transferTo(outputStream)
    outputStream
  }

  override def delete(containerPath: String)(filePath: String): Future[Boolean] = Future(
    Paths.get(filePath).toFile().delete()
  )

  override def copy(
    containerPath: String,
    path: String
  )(filePathToCopy: String, resourceId: UUID, fileName: String): Future[StorageFilePath] = Future {
    val destination: Path = createPath(path, resourceId.toString, fileName)
    Files.copy(Paths.get(filePathToCopy), destination, StandardCopyOption.REPLACE_EXISTING).toAbsolutePath().toString()
  }

  private def createPath(path: String, resourceId: String, fileName: String): Path = {
    val pathF: String       = path.stripMargin('/')
    val resourceIdF: String = resourceId.stripMargin('/')
    val docsHome: Path      = tmp.resolve(s"$pathF/$resourceIdF")
    val pathCreated: Path   = Files.createDirectories(docsHome)
    pathCreated.resolve(fileName.stripMargin('/'))
  }

}
