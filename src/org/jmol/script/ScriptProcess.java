package org.jmol.script;


/**
 * the idea here is that the process { ... } command would collect and
 * preprocess the scripts, then pass them here for storage until the end of
 * the parallel block is reached.
 */

class ScriptProcess {
  String processName;
  ScriptContext context;

  ScriptProcess(String name, ScriptContext context) {
    //System.out.println("Creating process " + name);
    processName = name;
    this.context = context;
  }
}