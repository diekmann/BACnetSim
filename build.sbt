name := "BACnetSimulator"

version := "0.1.1"

scalaVersion := "2.9.2"


//https://github.com/sbt/sbt-onejar
seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

libraryDependencies += "commons-lang" % "commons-lang" % "2.6"

// http://mvnrepository.com/artifact/org.scala-lang/jline/2.9.2
//http://vikashazrati.wordpress.com/2011/09/01/quicktip-ignoring-some-jars-from-getting-assembled-in-sbt-0-10x/
libraryDependencies += "org.scala-lang" % "jline" % "2.9.2" intransitive()  

//https://github.com/lift/framework/tree/master/core/json
libraryDependencies += "net.liftweb" %% "lift-json" % "2.5-M1"

scalacOptions += "-deprecation"

scalacOptions += "-unchecked"
