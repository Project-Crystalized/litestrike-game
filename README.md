# Here are instructions to get a litestrike server up and running manualy
mainly for linux, can be adapted for windows

## pre setup:
java 21 is installed
litestrike is compiled

## main instructions
make a new directory and enter it

download paper: https://papermc.io/downloads/paper
move paper jar to the directory

run paper jar
$ java -jar <paper_server_jar_name>
	- you can add the -nogui option to the end of the command to prevent the gui

accept the paper eula

start server again and stop it when done starting

remove all the directorys starting with "world"

open server.properties
	set allow-nether=false
	set online-mode=false
	DONT CHANGE DEFUALT WORLD NAME
	TODO how to set resource pack
	set spawn-monsters=false

open bukkit.yml
	set allow-end: false

open spigot.yml
    set players: disable-saving to true, this will prevent saving player inventorys and potion effects

### setting up use behind proxy: {
		Proxy Setup:
	Download Velocity from [link here]
	Start it up in the same way as the Paper server so it generates the files, then shut it down
	In the Paper server, set online-mode to false.
	
	In paper_server_dir/config/paper-global.yml, Look for:
	proxies:
	 bungee-cord:
	   (...)
	 velocity:
	  enabled: false
	  online-mode: true
	  secret: ''
	
	Set enabled to true and copy the contents from velocity_server_dir/forwarding.secret and paste it into secret:, like so
	secret: ************ 
	
	In the Paper server, set server-ip to 127.0.0.1 (if running on the same machine) and server-port to something thats not port forwarded
	
	In Velocity's velocity.toml under [servers], Add the Paper server with the correct IP and Port 
	Start up both servers, use /server to go to the litestrike server and thats it
}

move the directory containing the litestrike map into the server directory
	-make sure the map is called "world"
	-make sure it contains a valid map_config.json file

go into plugin folder
	-download latest version of protocolib and move it here
	-move compiled litestrike jar into here

go back to server directory

start the server, it should now be ready!

you might have to set yourself to op
