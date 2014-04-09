
This is the readme file of MyBot software project for developing a DipGame negotiator bot.

	_Developing_
This is an Eclipse project. We recommend you to use Eclipse (http://www.eclipse.org) as your Integrated Development Environment (IDE) for developing DipGame bots. You can also implement a bot using a simple editor or another IDE.
We include three classes in this project: MyBot, MyNegotiator and MyNegotiationHandler. Those classes define an skeleton that you can complete in order to create your own bot. See the comments in the code for more info.


	_Running_
MyBot.java contains the main method. You can run the bot executing this class, that is:
	cd <MyBot_project_folder>/src/
	javac *.java
	java MyBot
	
You can also do that with the help of Ant (http://ant.apache.org/). We included a build.xml file in the project that you can also use to generate an executable jar file with your bot.
	cd <MyBot_project_folder>
	ant
	
This jar file could be installed in a GameManager (http://www.dipgame.org/browse/gameManager) to run games with it. To do so, copy the file in the programs/ folder of GameManager and add the following line in the files/availablePlayers.txt file:
	myBot;<JAVA_ENV> -jar programs/myBot-0.1.jar <game_ip> <game_port> <nego_ip> <nego_port> <name>
	
	
	_Reporting your work_
You can report your work to DipGame and even offer your bot to be connected to the online games. In that case we will acknowledge your contribution. You could have your bot playing online against people from all the world.


Angela Fabregues, IIIA-CSIC, fabregues@iiia.csic.es 
