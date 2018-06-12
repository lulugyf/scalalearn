package gyf.test.scala.utils

import java.io._
import java.util.zip._

/** Gzcat
  */
object gzcat extends App {
  private val buf = new Array[Byte](1024)

  try {
    for (path <- args) {
      try {
        var in = new GZIPInputStream(new FileInputStream(path))
        var n = in.read(buf)
        while (n >= 0) {
          System.out.write(buf, 0, n)
          n = in.read(buf)
        }
      }
      catch {
        case _:FileNotFoundException =>
          System.err.printf("File Not Found: %s", path)
        case _:SecurityException =>
          System.err.printf("Permission Denied: %s", path)
      }
    }
  }
  finally {
    System.out.flush
  }
}


/** gzip
  */
object gzip {
  private val buf = new Array[Byte](1024)

  def gzip(path: String) {
    val src = new File(path)
    val dst = new File(path ++ ".gz")

    try {
      val in  = new BufferedInputStream(new FileInputStream(src))
      try {
        val out = new GZIPOutputStream(new FileOutputStream(dst))
        try {
          var n = in.read(buf)
          while (n >= 0) {
            out.write(buf, 0, n)
            n = in.read(buf)
          }
        }
        finally {
          out.flush
        }
      } catch {
        case _:FileNotFoundException =>
          System.err.printf("Permission Denied: %s", path ++ ".gz")
        case _:SecurityException =>
          System.err.printf("Permission Denied: %s", path ++ ".gz")
      }
    } catch {
      case _: FileNotFoundException =>
        System.err.printf("File Not Found: %s", path)
      case _: SecurityException =>
        System.err.printf("Permission Denied: %s", path)
    }
  }

  def main(filenames: Array[String]) {
    if (filenames.length == 0) {
      System.err.println("Usage: scala gzip file...")
      System.exit(1)
    }
    else {
      filenames.foreach(gzip(_))
    }
  }
}


object gunzip {
  private val buf = new Array[Byte](1024)

  /**
    * gunzip
    *
    * @param path: the path name without the extentson ".gz"
    *   You have to check if there is no file named as PATH
    * before you invode this method, or the file is going
    * to be overwritten.
    */
  def gunzip(path: String) {
    val src = new File(path ++ ".gz")
    val dst = new File(path)

    try {
      val in  = new GZIPInputStream(new FileInputStream(src))
      try {
        val out = new BufferedOutputStream(new FileOutputStream(dst))
        try {
          var n = in.read(buf)
          while (n >= 0) {
            out.write(buf, 0, n)
            n = in.read(buf)
          }
        }
        finally {
          out.flush
        }
      } catch {
        case _:FileNotFoundException =>
          System.err.printf("Permission Denied: %s", path)
        case _:SecurityException =>
          System.err.printf("Permission Denied: %s", path)
      }
    } catch {
      case _: FileNotFoundException =>
        System.err.printf("File Not Found: %s", path ++ ".gz")
      case _: SecurityException =>
        System.err.printf("Permission Denied: %s", path ++ ".gz")
    }
  }

  def main(filenames: Array[String]) {
    if (filenames.length == 0) {
      System.err.println("Usage: scala gunzip gz-file...")
      System.exit(1)
    }
    else {
      val eitherNames =
        for (name <- filenames)
          yield if (name.endsWith(".gz")) {
            val noExtension = name.drop(3)
            val src = new File(name)
            val dst = new File(noExtension)
            if (! src.exists)
              Left(new FileNotFoundException(name))
            else if (dst.exists)
              Left(new IOException("file already exists: " + noExtension))
            else
              Right(noExtension)
          } else {
            Left(new IOException("file is not gzip: " + name))
          }
      if (eitherNames.forall {_.isRight}) {
        for (Right(noExt) <- eitherNames) {
          gunzip(noExt)
        }
      }
      else {
        for (Left(e) <- eitherNames)
          throw e
      }
    }
  }
}

object GZCompress {
  def gzipToBytes(plaintext: Array[Byte]): Array[Byte] = {
    if (plaintext == null || plaintext.length == 0) return new Array[Byte](0)
    val out = new ByteArrayOutputStream
    val gzip = new GZIPOutputStream(out)
    gzip.write(plaintext)
    gzip.close()
    out.toByteArray
  }

  def gunzipFromBytes(ciphertext: Array[Byte]): Array[Byte] = {
    if (ciphertext == null || ciphertext.length == 0) return new Array[Byte](0)
    val out = new ByteArrayOutputStream
    val in = new ByteArrayInputStream(ciphertext)
    val gunzip = new GZIPInputStream(in)
    val buffer = new Array[Byte](256)
    var n = 1
    while (n > 0) {
      n = gunzip.read(buffer)
      if(n > 0)
        out.write(buffer, 0, n)
    }
    out.toByteArray
  }
}