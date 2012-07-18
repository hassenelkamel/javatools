package javatools.util;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.filehandlers.FileSet;

/** 
This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
It is licensed under the Creative Commons Attribution License 
(see http://creativecommons.org/licenses/by/3.0) by 
the YAGO-NAGA team (see http://mpii.de/yago-naga)

This utility checks which files are referenced by a Latex File
*/
public class LatexChecker {

  /** Include statements*/
  public static final Set<String> includeStatements=new FinalSet<String>("\\include","\\input","\\includegraphics","\\documentclass");
  
  /** Returns all files referenced in this latex file*/
  public static Set<String> references(File latexFile) throws IOException {
    Set<String> result=new HashSet<String>();
    for(String line : new FileLines(latexFile)) {
      for(String stat : includeStatements) {
        for(int i=line.indexOf(stat);i!=-1;i=line.indexOf(stat,i+1)) {
          int j=line.indexOf('{',i);
          if(j==-1) continue;
          int k=line.indexOf('}',i);
          if(k==-1) continue;
          result.add(line.substring(j+1,k));
        }
      }
    }
    return(result);
  }
  
  /** Returns all referenced files, recursively*/
  public static Set<File> referencedBy(File latexFile) throws IOException {
    Announce.doing("Analyzing",latexFile);
    Set<File> result=new HashSet<File>();
    for(String filename : references(latexFile)) {
      File f=new File(latexFile.getParentFile(),filename);
      if(!f.exists()) f=FileSet.newExtension(f, "cls");
      if(!f.exists()) f=FileSet.newExtension(f, "bib");
      if(!f.exists()) f=FileSet.newExtension(f, "tex");
      if(!f.exists()) {
        Announce.warning("File not found:",f);
        continue;
      }
      result.add(f);
      if(FileSet.extension(f).equals(".tex")) result.addAll(referencedBy(f));
    }
    Announce.done();
    return(result);
  }
  
  /** returns all superfluous files*/
  public static Set<File> superfluous(Set<File> otherFiles) {
    Set<File> folders=new HashSet<File>();
    for(File f : otherFiles) folders.add(f.getParentFile());
    Set<File> result=new HashSet<File>();
    for(File folder : folders) {
      for(File f : folder.listFiles()) {
        if(!otherFiles.contains(f)) result.add(f);
      }
    }
    return(result);
  }
  
  /** returns all referenced and all superfluous files of a given latex file*/
  public static void main(String[] args) throws Exception {
    args=new String[]{"c:/fabian/conferences/icde2013/susie.tex"};
    File latexFile=new File(args[0]);
    Set<File> referenced=referencedBy(latexFile);
    Announce.doing("Referenced files");
    for(File f : referenced) {
      Announce.message(f);
    }
    Announce.doneDoing("Non-referenced");
    for(File f : superfluous(referenced)) {
      Announce.message(f);
    }
    Announce.done();
  }
}
