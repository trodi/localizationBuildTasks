package main.scala

import java.io.File
import java.io.FileInputStream
import java.util.Properties
import scala.collection.JavaConverters
import scala.collection.mutable.Map
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.XML
import scala.io.Source

/**
 * -run property extraction
 * -send to translators
 * -run property insertion
 *
 * @author tmckinnon
 */
object LocalizationDiff {

  val FILE_EXTENSION_PROPERTIES = ".properties"
  val FILE_EXTENSION_RESX = ".resx"
  val FILE_EXTENSION_TEMPLATE = ".ftl"
  val ACCEPTED_FILE_EXTENSIONS = Set(FILE_EXTENSION_PROPERTIES, FILE_EXTENSION_RESX)

  def main(args: Array[String]) {

    val (startDir_New, startDir_Old, exclusionsList) = parseParameters(args)
    var _addedKeys, _updatedKeys, _removedKeys, _wordCount = 0

    recurseFileSystem(startDir_New, true) //check for updates to existing files and additional files
    recurseFileSystem(startDir_Old, false) //check for files removed in new set

    println(String.format("Total keys added[%s] changed [%s] deleted[%s]. ", _addedKeys.toString, _updatedKeys.toString, _removedKeys.toString))
    println(String.format("Total word count [%s].", _wordCount.toString))

    /** Compares pFile from the new dir to the equivalent in the old dir. */
    def helperCompareFiles(pFile: File) {
        val _old = new File(pFile.getAbsolutePath().replace(startDir_New.getAbsolutePath(), startDir_Old.getAbsolutePath()))
        if (_old.exists()) {
          updateCounters(compareFile(pFile, _old))
        } else {//handle new files
          val _propFile = JavaConverters.asScalaMapConverter(loadPropFile(pFile)).asScala
          updateCounters((_propFile.size,0,0,_propFile.size))
          println(new StringBuilder("// New Property File: ").append(pFile.getAbsolutePath()))
            printPropertySet("// New properties: ", _propFile)
        }
    }

    /** Checks to see if pFile from the old dir was removed in the new dir. */
    def helperCheckForRemovedFiles(pFile: File) {
//    val func = { (pFile: File) => {
      val _new = new File(pFile.getAbsolutePath().replace(startDir_Old.getAbsolutePath(), startDir_New.getAbsolutePath()))
      if (!_new.exists()) {
        val _propFile = JavaConverters.asScalaMapConverter(loadPropFile(pFile)).asScala
        updateCounters((0,0,_propFile.size,0))
        println(new StringBuilder("// Removed Property File: ").append(pFile.getAbsolutePath()))
        printPropertySet("// Removed properties: ", _propFile)
      }
    }

    /**
     *  Recurse the File system at given point and print out comparisons of files. Call with both new dir and old dir.
     *  Calling old dir will allow finding files that have been deleted since old version.
     *  @param pIsNewDir - Whether the given dir is from the new version or not
     */
    def recurseFileSystem(pDir: File, pIsNewDir: Boolean) {
      def helper(pFile: File) {
        if (pFile.isFile()) {
          if (pIsNewDir) helperCompareFiles(pFile)
          else helperCheckForRemovedFiles(pFile)
        } else if (pFile.isDirectory()) {
          recurseFileSystem(pFile, pIsNewDir)
        }
      }

      pDir.listFiles.toSet[File].filter(
          x => (x.isDirectory() || isAcceptedFileType(x)) && isNotExcluded(x)).map(x => helper(x))
    }

    /** @return Whether the file is allowed or in the exclusion list. */
    def isNotExcluded(pFile: File): Boolean = {
      val _name = pFile.getName()
      !exclusionsList.contains(_name)
    }

    /**
     * Addes the passed in values to the existing counts.
     * @param pCounters (added key pairs, updated key pairs, removed key pairs, total wc to send to translators)
     */
    def updateCounters(pCounters: (Int,Int,Int,Int)) {
      _addedKeys += pCounters._1
      _updatedKeys += pCounters._2
      _removedKeys += pCounters._3
      _wordCount += pCounters._4
    }
  }

  /** @return Whether this file type is supported for properties file. */
  private def isAcceptedFileType(pFile: File): Boolean = {
    ACCEPTED_FILE_EXTENSIONS.map(x => pFile.getName().contains(x)).contains(true)
  }

  /**
   * @param args 0 - start dir of new resources
   *             1 - start dir of old resources (extracted and unzipped from older build for now)
   *             2 - ???
   */
  private def parseParameters(args: Array[String]): (File, File, Set[String]) = {
    val _args = (new File(args(0)), new File(args(1)), args(2).split(',').toSet[String])

    if (!_args._1.exists()) throw new IllegalArgumentException("New starting dir doesn't exist!")
    if (_args._1.isFile()) throw new IllegalArgumentException("New starting dir is a file!")
    if (!_args._2.exists()) throw new IllegalArgumentException("Old starting dir doesn't exist!")
    if (_args._2.isFile()) throw new IllegalArgumentException("Old starting dir is a file!")

    _args
  }

  private def loadPropFile(pFile: File): Properties = {
    if (pFile.getName().contains(FILE_EXTENSION_PROPERTIES)) loadJavaPropFile(pFile)
    else if (pFile.getName().contains(FILE_EXTENSION_RESX)) loadXMLPropFile(pFile)
    else throw new IllegalArgumentException("Can't read file type: " + pFile.getName())
  }

  /** Load in properties from Java properties file. */
  private def loadJavaPropFile(pFile: File): Properties = {
    val _propFile = new Properties
    val _inputStream = new FileInputStream(pFile)
    _propFile.load(_inputStream)
    _propFile
  }

  /** Load in properties file from .resx file. */
  private def loadXMLPropFile(pFile: File): Properties = {
    val _node = "data"
    val _key = "name"
    val _value = "value"
    val _propFile = new Properties
    val _xmlObject = XML.loadFile(pFile).child.filter(x => x.label == _node)
      .map(x => (_propFile.setProperty(x.attribute(_key).get.toString(), x.child.filter(x => x.label == _value).text)))
    //printPropertySet("test OAI file", _propFile.stringPropertyNames().toArray().toSet, _propFile)
    _propFile
  }

  /** @return (added, updated, removed, total word count that has to be sent to translators) - tuple of the count of changed properties. */
  private def compareFile(pNewFile: File, pOldFile: File): (Int, Int, Int, Int) = {
    println(new StringBuilder("// Property File: ").append(pNewFile.getName()))

    val _new = loadPropFile(pNewFile)
    val _old = loadPropFile(pOldFile)

    val _newKeysSet = JavaConverters.asScalaMapConverter(_new).asScala
    val _oldKeysSet = JavaConverters.asScalaMapConverter(_old).asScala

    val _addedKeys = _newKeysSet.filter(x => !_oldKeysSet.contains(x._1))
    printPropertySet("// New properties: ", _addedKeys)

    val _removedKeys = _oldKeysSet.filter(x => !_newKeysSet.contains(x._1))
    printPropertySet("// Removed properties: ", _removedKeys)

    val _updatedValues = _newKeysSet.filter(x => _oldKeysSet.contains(x._1) && _oldKeysSet(x._1) != _newKeysSet(x._1))
    printPropertySet("// Updated properties: ", _updatedValues)

    val _wordCount = _addedKeys.map(x => x._2.split(' ').size).sum + _updatedValues.map(x => x._2.split(' ').size).sum

    (_addedKeys.size, _updatedValues.size, _removedKeys.size, _wordCount)
  }

  /** Print out a prettily formatted block of properties associated with pHeader. */
  private def printPropertySet(pHeader: String, pSet: Map[String,String]) {
    def format(pKey: String, pValue: String) {
      println(new StringBuilder("\t").append(pKey).append('=').append(pValue).toString())
    }
    if (!pSet.isEmpty) println(pHeader)
    pSet.map(x => format(x._1, x._2))
  }
}
