package tum.in.net.bacnet

import tum.in.net.bacnet.deviceFactory.{MySlaveDevice, MyReader, MyReaderWriterActiveMasterSlaveDevice}



object MyMain {
  
  def slave(): Unit = {
    (new MySlaveDevice("127.0.0.255", "corny's personal slave")).start()
  }
  
  def randomSlave(): Unit = {
   (new MySlaveDevice("127.0.0.255", randomValues=true)).start()
  }
  
  def reader(): Unit = {
    (new MyReader()).start()
  }
  
  def randomRWSlave(): Unit = {
    println("Will generate a slave device with IO which also connects to other slaves")
    (new MyReaderWriterActiveMasterSlaveDevice()).start()
  }
  
  
  def help(): Unit = {
    println("available commands: "+cmds.keys)
    println("place a `otherdevices.xml' in your current working directory with the IP addresse "+
        "of all BACnet devices")
  }
  
  val cmds: Map[String, () => Unit] = Map("slave" -> slave, 
      "randomSlave" -> randomSlave, 
      "reader" -> reader, 
      "randomRWSlave" -> randomRWSlave, 
      "help" -> help)

  def main(args: Array[String]) {
    System.setProperty("java.net.preferIPv4Stack", "true");
    
    
    if(args.length > 0){
      println("Oh cool, an argument")
      val actingThing = cmds.get(args(0))
      actingThing match {
        case None => println("available commands: "+cmds.keys)
        case Some(f) => f()
      }
    }else{
      println("starting default. pass command line argument help for help")
    	(new MyReader).start()
    }
    

  }
  
  
  
  
}