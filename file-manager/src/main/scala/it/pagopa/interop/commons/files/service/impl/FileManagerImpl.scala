package it.pagopa.interop.commons.files.service.impl

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.interop.commons.files.service.FileManager

import java.io.{ByteArrayOutputStream, File, FileInputStream}
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import scala.concurrent.{ExecutionContextExecutor, Future}
import java.nio.file.FileVisitOption
import java.util.stream.Collectors
import scala.jdk.CollectionConverters._

final class FileManagerImpl(blockingExecutionContext: ExecutionContextExecutor) extends FileManager {

  override def close(): Unit = ()

  implicit val ec: ExecutionContextExecutor = blockingExecutionContext

  val tmp: Path = Path.of("/tmp")

  override def store(
    containerPath: String,
    path: String
  )(resourceId: String, fileParts: (FileInfo, File)): Future[StorageFilePath] = Future {
    val destPath: Path = createPath(path, resourceId, fileParts._1.getFileName)
    Files.move(fileParts._2.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING).toAbsolutePath().toString()
  }

  override def storeBytes(
    containerPath: String,
    path: String
  )(resourceId: String, fileName: String, fileContents: Array[Byte]): Future[StorageFilePath] = Future {
    val destPath: Path = createPath(path, resourceId, fileName)
    Files.write(destPath, fileContents).toAbsolutePath().toString()
  }

  override def storeBytes(containerPath: String, path: String, filename: String)(
    fileContents: Array[Byte]
  ): Future[StorageFilePath] = Future(
    Files.write(createPath(path, "", filename), fileContents).toAbsolutePath().toString()
  )

  override def listFiles(container: String)(prefix: String): Future[List[StorageFilePath]] = Future(
    Files
      .walk(tmp.resolve(prefix), FileVisitOption.FOLLOW_LINKS)
      .collect(Collectors.toList[Path])
      .asScala
      .toList
      .map(_.toAbsolutePath.toString)
  )

  override def getFile(container: String)(path: String): Future[Array[Byte]] = Future(
    Files.readAllBytes(tmp.resolve(path))
  )

  override def getAllFiles(container: String)(prefix: String): Future[Map[String, Array[Byte]]] =
    listFiles(container)(prefix)
      .flatMap(files => Future.traverse(files)(p => getFile(container)(p).map((p, _))).map(_.toMap))

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
  )(filePathToCopy: String, resourceId: String, fileName: String): Future[StorageFilePath] = Future {
    val destination: Path = createPath(path, resourceId, fileName)
    Files.copy(Paths.get(filePathToCopy), destination, StandardCopyOption.REPLACE_EXISTING).toAbsolutePath().toString()
  }

  private def createPath(path: String, resourceId: String, fileName: String): Path = {
    val pathF: String       = path.stripMargin('/')
    val resourceIdF: String = resourceId.stripMargin('/')
    val docsHome: Path      =
      if (resourceIdF.isBlank()) tmp.resolve(pathF)
      else tmp.resolve(s"$pathF/$resourceIdF")
    val pathCreated: Path   = Files.createDirectories(docsHome)
    pathCreated.resolve(fileName.stripMargin('/'))
  }

}
